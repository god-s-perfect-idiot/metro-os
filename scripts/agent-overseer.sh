#!/usr/bin/env bash
# Overseer: orchestrate Cursor coding agent → verify-app → run-app/AVD → verifier agent.
#
# Usage:
#   ./scripts/agent-overseer.sh <app> "<task>"
#   ./scripts/agent-overseer.sh <app> --verify-only
#   ./scripts/agent-overseer.sh <app> --fix "<follow-up for coding agent>"
#
# Requires: agent CLI (curl https://cursor.com/install -fsS | bash) + agent login
# Optional: CURSOR_API_KEY for headless auth
#
# Environment:
#   METRO_OVERSEER_MAX_ITER=5   max coding/fix loops (default 5)
#   METRO_AGENT_MODEL=...       passed to agent --model
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/metro-common.sh
source "$ROOT/scripts/lib/metro-common.sh"

APP="${1:-}"
shift || true

MODE="full"
TASK=""
FIX_TASK=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --verify-only) MODE="verify-only" ;;
    --fix)
      MODE="fix"
      FIX_TASK="${2:-}"
      shift
      ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0
      ;;
    *)
      if [[ -z "$TASK" ]]; then
        TASK="$1"
      else
        TASK="$TASK $1"
      fi
      ;;
  esac
  shift
done

if [[ -z "$APP" ]]; then
  echo "Usage: $0 <app> \"<task>\" | --verify-only | --fix \"<task>\"" >&2
  exit 2
fi

if [[ "$MODE" == "full" && -z "$TASK" ]]; then
  echo "ERROR: provide a task string or use --verify-only" >&2
  exit 2
fi

if [[ "$MODE" == "fix" && -z "$FIX_TASK" ]]; then
  echo "ERROR: --fix requires a follow-up task string" >&2
  exit 2
fi

MAX_ITER="${METRO_OVERSEER_MAX_ITER:-5}"
AGENT_BIN="$(metro_agent_bin)"
export PATH="$(dirname "$AGENT_BIN"):$PATH"

if ! metro_require_agent_auth; then
  exit 1
fi

APP_DIR="$(metro_app_dir "$APP")"
OVERSEER_LOG="$APP_DIR/deploy/overseer.log"
mkdir -p "$APP_DIR/deploy"
: >"$OVERSEER_LOG"

log() {
  echo "$*" | tee -a "$OVERSEER_LOG"
}

render_prompt() {
  local template="$1"
  local content
  content="$(cat "$template")"
  content="${content//\{\{APP\}\}/$APP}"
  content="${content//\{\{TASK\}\}/$TASK}"
  printf '%s' "$content"
}

agent_args() {
  AGENT_ARGS=(
    -p
    --force
    --trust
    --workspace "$ROOT"
    --output-format text
  )
  if [[ -n "${METRO_AGENT_MODEL:-}" ]]; then
    AGENT_ARGS+=(--model "$METRO_AGENT_MODEL")
  fi
  if [[ -n "${CURSOR_API_KEY:-}" ]]; then
    AGENT_ARGS+=(--api-key "$CURSOR_API_KEY")
  fi
}

run_coding_agent() {
  local prompt_file="$ROOT/scripts/prompts/coding-agent.md"
  local prompt
  if [[ "$MODE" == "fix" ]]; then
    prompt="Follow-up for apps/$APP: $FIX_TASK

Read apps/$APP/deploy/verify-report.json and apps/$APP/deploy/avd-report.json.
Fix the first failure, then re-run:
  ./scripts/verify-app.sh $APP
  ./scripts/run-app.sh $APP --verify"
  else
    prompt="$(render_prompt "$prompt_file")"
  fi

  log "==> coding agent (apps/$APP)"
  agent_args
  "${AGENT_BIN}" "${AGENT_ARGS[@]}" "$prompt" 2>&1 | tee -a "$OVERSEER_LOG"
}

run_verifier_agent() {
  local prompt
  prompt="$(render_prompt "$ROOT/scripts/prompts/verifier-agent.md")"
  local verifier_args=(
    -p
    --mode ask
    --trust
    --workspace "$ROOT"
    --output-format text
  )
  if [[ -n "${METRO_AGENT_MODEL:-}" ]]; then
    verifier_args+=(--model "$METRO_AGENT_MODEL")
  fi
  if [[ -n "${CURSOR_API_KEY:-}" ]]; then
    verifier_args+=(--api-key "$CURSOR_API_KEY")
  fi

  log "==> verifier agent (read-only)"
  local out
  out="$("${AGENT_BIN}" "${verifier_args[@]}" "$prompt" 2>&1 | tee -a "$OVERSEER_LOG")"

  local verdict_file="$APP_DIR/deploy/verifier-verdict.json"
  python3 - "$verdict_file" "$out" <<'PY'
import json, re, sys
path, text = sys.argv[1], sys.argv[2]
# Extract first JSON object from agent output
m = re.search(r'\{[\s\S]*\}', text)
if not m:
    open(path, 'w').write(json.dumps({"passed": False, "error": "verifier did not return JSON"}, indent=2))
    sys.exit(0)
try:
    data = json.loads(m.group(0))
except json.JSONDecodeError:
    data = {"passed": False, "error": "invalid JSON from verifier", "raw": m.group(0)[:500]}
with open(path, 'w') as f:
    json.dump(data, f, indent=2)
PY
  echo "Verifier verdict: $verdict_file"
  cat "$verdict_file"
}

run_harness() {
  log "==> verify-app.sh $APP"
  if ! "$ROOT/scripts/verify-app.sh" "$APP" 2>&1 | tee -a "$OVERSEER_LOG"; then
    return 1
  fi
  log "==> run-app.sh $APP --verify"
  if ! "$ROOT/scripts/run-app.sh" "$APP" --verify 2>&1 | tee -a "$OVERSEER_LOG"; then
    return 1
  fi
  return 0
}

log "==> agent-overseer: $APP (mode=$MODE, max_iter=$MAX_ITER)"
"$ROOT/scripts/verify-env.sh" 2>&1 | tee -a "$OVERSEER_LOG" || true

iter=1
while [[ $iter -le $MAX_ITER ]]; do
  log "--- iteration $iter/$MAX_ITER ---"

  if [[ "$MODE" == "verify-only" ]]; then
    :
  elif [[ "$MODE" == "fix" ]]; then
    run_coding_agent
    MODE="full"
    TASK=""
  else
    run_coding_agent
  fi

  if run_harness; then
    run_verifier_agent
    if python3 - "$APP_DIR/deploy/verifier-verdict.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
sys.exit(0 if data.get("passed") else 1)
PY
    then
      log "agent-overseer: PASSED (harness + verifier)"
      exit 0
    fi
    log "WARN  harness passed but verifier returned passed=false"
    TASK="Address verifier gaps in apps/$APP/deploy/verifier-verdict.json recommended_fixes"
    MODE="fix"
    FIX_TASK="$TASK"
  else
    log "WARN  harness failed — scheduling fix iteration"
    FIX_TASK="Fix the first failure in verify-report.json or avd-report.json for apps/$APP"
    MODE="fix"
  fi

  iter=$((iter + 1))
done

log "agent-overseer: FAILED after $MAX_ITER iterations — see $OVERSEER_LOG"
echo "Append postmortem to apps/$APP/README.md per AGENTS.md" >&2
exit 1
