#!/usr/bin/env bash
# Shared helpers for metro-os scripts. Source from repo scripts; do not execute directly.
set -euo pipefail

metro_root() {
  if [[ -n "${METRO_ROOT:-}" ]]; then
    echo "$METRO_ROOT"
    return 0
  fi
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  METRO_ROOT="$here"
  echo "$METRO_ROOT"
}

metro_android_home() {
  if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
    echo "$ANDROID_HOME"
    return 0
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT" ]]; then
    echo "$ANDROID_SDK_ROOT"
    return 0
  fi
  for candidate in \
    "$HOME/Library/Android/sdk" \
    "$HOME/Android/Sdk"; do
    if [[ -d "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done
  echo "ERROR: Android SDK not found — set ANDROID_HOME" >&2
  return 1
}

metro_path_android_tools() {
  local sdk
  sdk="$(metro_android_home)"
  export ANDROID_HOME="$sdk"
  export PATH="$sdk/platform-tools:$sdk/emulator:$PATH"
}

metro_list_avds() {
  metro_path_android_tools
  local emulator_bin="$ANDROID_HOME/emulator/emulator"
  if [[ -x "$emulator_bin" ]]; then
    "$emulator_bin" -list-avds 2>/dev/null || true
  elif command -v emulator >/dev/null 2>&1; then
    emulator -list-avds 2>/dev/null || true
  fi
}

metro_resolve_avd() {
  metro_path_android_tools
  if [[ -n "${METRO_AVD:-}" ]]; then
    echo "$METRO_AVD"
    return 0
  fi
  local first
  first="$(metro_list_avds | head -1)"
  if [[ -z "$first" ]]; then
    echo "ERROR: no AVD found — create one in Android Studio (API 26+, portrait phone)" >&2
    return 1
  fi
  echo "$first"
}

metro_adb_devices() {
  metro_path_android_tools
  adb devices 2>/dev/null | awk 'NR>1 && $2=="device" { print $1 }'
}

metro_wait_for_boot() {
  metro_path_android_tools
  adb wait-for-device
  local boot=""
  local i
  for i in $(seq 1 90); do
    boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot" == "1" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "ERROR: emulator boot timeout (sys.boot_completed != 1)" >&2
  return 1
}

metro_ensure_avd() {
  metro_path_android_tools
  if [[ -n "$(metro_adb_devices)" ]]; then
    metro_wait_for_boot
    echo "OK  device connected: $(metro_adb_devices | head -1)"
    return 0
  fi

  local avd emulator_bin
  avd="$(metro_resolve_avd)"
  emulator_bin="$ANDROID_HOME/emulator/emulator"
  if [[ ! -x "$emulator_bin" ]]; then
    echo "ERROR: emulator binary not found at $emulator_bin" >&2
    return 1
  fi

  echo "==> starting AVD: $avd"
  nohup "$emulator_bin" -avd "$avd" -no-snapshot-load >/tmp/metro-emulator.log 2>&1 &
  metro_wait_for_boot
  echo "OK  AVD ready: $avd"
}

# Returns RUNNING_UNLOCKED, RUNNING_LOCKED, or unknown.
metro_user_state() {
  metro_path_android_tools
  adb shell dumpsys user 2>/dev/null | tr -d '\r' | grep -oE '0=RUNNING_[A-Z]+' | head -1 | cut -d= -f2
}

metro_try_dismiss_keyguard() {
  metro_path_android_tools
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell input swipe 540 1800 540 600 250 >/dev/null 2>&1 || true
}

# Fresh installs need an unlocked user so Android can create app data dirs.
# When user 0 is locked, am start fails with "Activity class ... does not exist".
metro_ensure_user_unlocked() {
  metro_path_android_tools
  local state
  state="$(metro_user_state)"
  if [[ "$state" == "RUNNING_UNLOCKED" ]]; then
    echo "OK  user unlocked"
    return 0
  fi

  echo "WARN  emulator user is locked ($state) — attempting keyguard dismiss"
  metro_try_dismiss_keyguard

  local i
  for i in $(seq 1 15); do
    state="$(metro_user_state)"
    if [[ "$state" == "RUNNING_UNLOCKED" ]]; then
      echo "OK  user unlocked"
      return 0
    fi
    sleep 2
  done

  echo "ERROR: emulator is locked — unlock it before installing/launching apps." >&2
  echo "  Symptom: 'Activity class ... does not exist' even though the APK installed." >&2
  echo "  Fix: on the emulator, swipe up and enter your PIN/pattern, then re-run." >&2
  echo "  Tip: remove the AVD screen lock (Settings → Security → Screen lock → None)" >&2
  echo "       so cold boots from run-app.sh work without manual unlock." >&2
  return 1
}

metro_app_dir() {
  local app="$1"
  local root
  root="$(metro_root)"
  echo "$root/apps/$app"
}

metro_app_package() {
  local app_dir="$1"
  local gradle="$app_dir/app/build.gradle.kts"
  if [[ -f "$gradle" ]]; then
    local pkg
    pkg="$(python3 - "$gradle" <<'PY'
import re, sys
text = open(sys.argv[1]).read()
m = re.search(r'applicationId\s*=\s*"([^"]+)"', text)
print(m.group(1) if m else "", end="")
PY
)"
    if [[ -n "$pkg" ]]; then
      echo "$pkg"
      return 0
    fi
  fi
  if [[ -f "$app_dir/AGENTS.md" ]]; then
    grep -oE 'com\.metro\.[a-z0-9._]+' "$app_dir/AGENTS.md" | head -1
    return 0
  fi
  echo "ERROR: cannot resolve package for $app_dir" >&2
  return 1
}

metro_app_launch_activity() {
  local app_dir="$1"
  local manifest="$app_dir/app/src/main/AndroidManifest.xml"
  local pkg activity
  pkg="$(metro_app_package "$app_dir")"
  if [[ -f "$manifest" ]]; then
    activity="$(python3 - "$manifest" <<'PY'
import re, sys
text = open(sys.argv[1]).read()
# First exported activity with MAIN/LAUNCHER
blocks = re.split(r'<activity\b', text)[1:]
for block in blocks:
    if 'android.intent.action.MAIN' in block and 'android:exported="true"' in block:
        m = re.search(r'android:name="([^"]+)"', block)
        if m:
            print(m.group(1))
            break
PY
)"
  fi
  if [[ -z "${activity:-}" ]]; then
    activity=".MainActivity"
  fi
  if [[ "$activity" == .* ]]; then
    echo "${pkg}/${activity}"
  elif [[ "$activity" == *.* ]]; then
    echo "${activity}"
  else
    echo "${pkg}/.${activity}"
  fi
}

metro_agent_bin() {
  if command -v agent >/dev/null 2>&1; then
    command -v agent
    return 0
  fi
  if [[ -x "$HOME/.local/bin/agent" ]]; then
    echo "$HOME/.local/bin/agent"
    return 0
  fi
  echo "ERROR: Cursor agent CLI not found — install: curl https://cursor.com/install -fsS | bash" >&2
  return 1
}

metro_require_agent_auth() {
  local agent_bin out
  agent_bin="$(metro_agent_bin)"
  export PATH="$(dirname "$agent_bin"):$PATH"
  out="$("$agent_bin" status 2>&1 || true)"
  if echo "$out" | grep -qi 'not logged in'; then
    echo "ERROR: Cursor agent not authenticated — run: agent login" >&2
    echo "       Or set CURSOR_API_KEY for headless runs" >&2
    return 1
  fi
  return 0
}

metro_timestamp_utc() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}
