---
name: publish-release
description: >-
  Cut and publish a metro-os GitHub release: find the latest release, collect
  post-release commits, draft release notes, bump suite app versions (minor by
  default, major for overhauls), bump the next tag, build all suite APKs, attach
  them, and publish. Use when the user asks to publish a release, cut a release,
  ship alpha, create a GitHub release, attach APKs to a release, or bump app
  versions for a release.
---

# Publish metro-os release

End-to-end suite release for this repo. Follow the steps in order. Do not skip
confirmation before creating the GitHub release or committing version bumps.

## Preconditions

- Working tree is clean enough to tag (warn if dirty; ask before proceeding).
- `gh` is authenticated for this repo.
- Run from repo root.

## Workflow checklist

Copy and track:

```
Release progress:
- [ ] 1. Latest GitHub release identified
- [ ] 2. Commits since that release collected
- [ ] 3. Release notes drafted from relevant changes
- [ ] 4. Next tag + app version bumps proposed and confirmed with user
- [ ] 5. Version bumps applied and committed
- [ ] 6. All suite apps built
- [ ] 7. Release created with notes
- [ ] 8. APKs attached
- [ ] 9. Release published (not draft)
- [ ] 10. URL reported to user
```

## Step 1 — Latest release

```bash
gh release list --limit 5
gh release view --json tagName,name,publishedAt,isPrerelease,isLatest,body
# If no --json default works, use:
gh api repos/:owner/:repo/releases/latest --jq '{tag:.tag_name,published:.published_at,prerelease:.prerelease}'
```

Record `PREV_TAG` (e.g. `alpha-2`). If there is no prior release, use the empty
range from the first commit and note that in the release notes.

## Step 2 — Commits since release

Prefer the release tag as the range start:

```bash
git fetch --tags origin
git log "${PREV_TAG}..HEAD" --oneline --no-merges
git log "${PREV_TAG}..HEAD" --format='%h %s' --no-merges
```

If the tag is missing locally, `git fetch --tags` first. Group commits by
prefix from the repo convention: `<app-or-toolkit>: <summary>`
(e.g. `launcher:`, `messaging:`, `metro-ui-android:`, `scripts:`).

Skip noise: chore-only bumps with no user impact, revert pairs that cancel out,
and pure formatting unless it changes behavior.

## Step 3 — Release notes

Draft notes in the established metro-os style (match prior release body shape):

```markdown
## metro-os <NEXT_TAG>

**Since:** `<PREV_TAG>`  
**Tag:** `<NEXT_TAG>` (`<SHORT_SHA>`)

<One-paragraph summary of the release theme.>

### Highlights

- **Title** — User-facing impact in one sentence.
- …

### <App or area>

- Bullet of relevant change
- …

### Shell & branding / Toolkits / Scripts

- …
```

Rules:

- Lead with user-visible Highlights (not every commit).
- Section by app/toolkit when there are enough commits.
- Prefer outcome language over file lists.
- Mention new apps entering the suite build when applicable.
- After bumps are confirmed, optionally note version deltas in app sections
  (e.g. `launcher 1.0.0 → 1.1.0`).

Show the draft to the user and incorporate edits before publishing.

## Step 4 — Next tag + app version bumps

### Release tag

Default scheme for this repo: `alpha-N` → increment N (`alpha-2` → `alpha-3`).

If the user specifies a different tag (`v0.1.0`, `beta-1`, etc.), use that.

### App version policy

Suite apps store Android versions in `apps/<name>/app/build.gradle.kts`:

```kotlin
versionCode = N
versionName = "MAJOR.MINOR.PATCH"
```

Propose a bump **per complete suite app that has relevant commits** since
`PREV_TAG` (match commit prefixes and meaningfully touched paths under
`apps/<name>/`).

| Kind | When to use | Semver effect | `versionCode` |
|------|-------------|---------------|---------------|
| **minor** (default) | Normal feature / UX / behavior changes for that app | `X.Y.Z` → `X.(Y+1).0` | `+1` |
| **major** | Full major overhaul or huge update for that app (new IA, rewrite, breaking UX reset) | `X.Y.Z` → `(X+1).0.0` | `+1` |
| **patch** | Only if the user asks (tiny hotfix-only delta) | `X.Y.Z` → `X.Y.(Z+1)` | `+1` |
| **skip** | No relevant commits; docs-only; or user opts out | unchanged | unchanged |

Rules:

- Default to **minor** for changed apps — do not invent majors.
- Propose **major** only when the commit set / release notes clearly show a
  full overhaul or huge update for that specific app (or the user asks).
- Toolkit-only / scripts-only / shared-resource commits do **not** auto-bump
  every app. Bump an app only when that app (or its shipped APK contents)
  changed. Shared icon/toolkit landings that ship inside many APKs may warrant
  bumps for the apps that actually rebuild with those assets — ask if unclear.
- Brand-new apps entering the suite: leave at `1.0.0` / `versionCode = 1`
  unless the user wants a different starting version.
- **keyboard** is special (FlorisBoard fork; versions in
  `apps/keyboard/gradle.properties`). Do **not** auto-bump keyboard unless the
  user explicitly asks; then edit those properties manually and note it.
- Always bump `versionCode` by **1** whenever `versionName` changes.
- Never bump versions without user confirmation.

Helper (from repo root):

```bash
BUMP=".cursor/skills/publish-release/scripts/bump-app-version.sh"
chmod +x "$BUMP"   # once if needed

# Inventory current suite versions
"$BUMP" --list-suite

# Read one app
"$BUMP" --read launcher

# Preview / apply (after confirmation)
"$BUMP" launcher minor --dry-run
"$BUMP" launcher minor
"$BUMP" messaging major
```

While drafting the confirmation, build a table:

```markdown
### Proposed app version bumps

| App | Current | Proposed | Kind | Why |
|-----|---------|----------|------|-----|
| launcher | 1.0.0 (1) | 1.1.0 (2) | minor | tile branding + music now-playing |
| messaging | 1.2.0 (3) | 2.0.0 (4) | major | conversation rewrite |
| photos | 1.1.0 (2) | — | skip | no commits |
```

### Confirmation (stop here)

**Stop and confirm** with the user before writing files or creating the release:

- `PREV_TAG` → `NEXT_TAG`
- App version bump table (edit kinds / skip apps as requested)
- Draft vs published (default: **published**, not draft)
- Prerelease flag (default: **false** for `alpha-N`, matching recent releases)
- Build variant: **debug** APKs (default; matches existing releases) unless the
  user asks for `--release`
- Whether to commit version bumps (default: **yes**, required before tag so
  APKs and the git tag match)

Do not create the GitHub release until the user confirms the tag, notes, and
version bumps.

## Step 5 — Apply version bumps and commit

After confirmation, apply only the agreed bumps:

```bash
BUMP=".cursor/skills/publish-release/scripts/bump-app-version.sh"
"$BUMP" launcher minor
"$BUMP" messaging major
# …one call per agreed app
```

Then commit the gradle changes (user already confirmed this release step):

```bash
git add apps/*/app/build.gradle.kts
git status
git commit -m "$(cat <<'EOF'
release: bump app versions for <NEXT_TAG>

EOF
)"
```

Only stage the version files you changed. Do not include unrelated WIP.
If the working tree had other dirty files, leave them untouched.

Update release notes with final `versionName` deltas if useful, then proceed.
The release tag must point at (or after) this version-bump commit so attached
APKs match the tagged sources.

## Step 6 — Build all apps

Build every complete app and collect suite APKs:

```bash
./scripts/build-apks.sh
# or, if user requested release builds:
./scripts/build-apks.sh --release
```

Outputs land in `deploy/apks/`:

- debug: `deploy/apks/<app>-debug.apk`
- release: `deploy/apks/<app>-release.apk`

On failure, fix or report; do not publish a partial suite unless the user
explicitly allows `--continue-on-error` and a partial asset set.

List assets that will be uploaded:

```bash
ls -1 deploy/apks/*-debug.apk
# or *-release.apk
```

## Step 7–9 — Create release, attach APKs, publish

Create the tag on current `HEAD` (annotated via `gh`), attach all suite APKs,
and publish in one shot:

```bash
NEXT_TAG=alpha-3   # example — use confirmed tag
NOTES_FILE="$(mktemp)"
# write finalized notes into NOTES_FILE

gh release create "$NEXT_TAG" \
  --title "$NEXT_TAG" \
  --notes-file "$NOTES_FILE" \
  deploy/apks/*-debug.apk
```

Flags:

- Add `--prerelease` only if the user asked for a prerelease.
- Add `--draft` only if the user asked to keep it as a draft (then say so; do
  not call it published).
- Do **not** pass `--latest=false` unless the user does not want this marked latest.

If the release already exists without assets, upload instead:

```bash
gh release upload "$NEXT_TAG" deploy/apks/*-debug.apk --clobber
```

Verify:

```bash
gh release view "$NEXT_TAG"
```

Confirm every expected APK appears under assets.

## Step 10 — Report

Tell the user:

- Release URL (`gh release view "$NEXT_TAG" --json url -q .url`)
- Tag and commit SHA
- App version bumps applied (table summary)
- App count / asset names attached
- One-line summary from the Highlights

## Hard rules

- Do not create a release without user confirmation of tag + notes + version bumps.
- Do not bump app versions without an explicit confirmed bump table.
- Default bump is **minor**; use **major** only for full overhauls / huge updates
  (or when the user requests it).
- Do not force-push or delete existing release tags.
- Do not commit APKs; only attach from `deploy/apks/` (gitignored).
- Do not update golden screenshots as part of release.
- Prefer `./scripts/build-apks.sh` over ad-hoc Gradle loops.
- Prefer
  `.cursor/skills/publish-release/scripts/bump-app-version.sh` over hand-editing
  version lines (except keyboard).
- Commit message prefixes in notes should reflect real commits; do not invent apps.
- Version-bump commit message: `release: bump app versions for <NEXT_TAG>`.

## Failure handling

| Failure | Action |
|---------|--------|
| No `gh` auth | Stop; ask user to `gh auth login` |
| Dirty tree with unrelated WIP | Warn; ask whether to tag current HEAD anyway; still isolate version-bump commit to version files only |
| Version parse/bump fails | Stop; fix `build.gradle.kts` format or bump manually; re-run `--read` |
| Build fails mid-suite | Stop; do not publish; paste failing app + log tail |
| Tag already exists | Stop; ask to reuse, bump, or abort |
| Asset upload fails | Keep release; retry `gh release upload`; report missing assets |
