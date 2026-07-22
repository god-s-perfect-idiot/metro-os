---
name: publish-release
description: >-
  Cut and publish a metro-os GitHub release: find the latest release, collect
  post-release commits, draft release notes, bump the next tag, build all suite
  APKs, attach them, and publish. Use when the user asks to publish a release,
  cut a release, ship alpha, create a GitHub release, or attach APKs to a release.
---

# Publish metro-os release

End-to-end suite release for this repo. Follow the steps in order. Do not skip
confirmation before creating the GitHub release.

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
- [ ] 4. Next tag chosen and confirmed with user
- [ ] 5. All suite apps built
- [ ] 6. Release created with notes
- [ ] 7. APKs attached
- [ ] 8. Release published (not draft)
- [ ] 9. URL reported to user
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

Show the draft to the user and incorporate edits before publishing.

## Step 4 — Next tag

Default scheme for this repo: `alpha-N` → increment N (`alpha-2` → `alpha-3`).

If the user specifies a different tag (`v0.1.0`, `beta-1`, etc.), use that.

**Stop and confirm** with the user:

- `PREV_TAG` → `NEXT_TAG`
- Draft vs published (default: **published**, not draft)
- Prerelease flag (default: **false** for `alpha-N`, matching recent releases)
- Build variant: **debug** APKs (default; matches existing releases) unless the
  user asks for `--release`

Do not create the GitHub release until the user confirms the tag and notes.

## Step 5 — Build all apps

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

## Step 6–8 — Create release, attach APKs, publish

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

## Step 9 — Report

Tell the user:

- Release URL (`gh release view "$NEXT_TAG" --json url -q .url`)
- Tag and commit SHA
- App count / asset names attached
- One-line summary from the Highlights

## Hard rules

- Do not create a release without user confirmation of tag + notes.
- Do not force-push or delete existing release tags.
- Do not commit APKs; only attach from `deploy/apks/` (gitignored).
- Do not update golden screenshots as part of release.
- Prefer `./scripts/build-apks.sh` over ad-hoc Gradle loops.
- Commit message prefixes in notes should reflect real commits; do not invent apps.

## Failure handling

| Failure | Action |
|---------|--------|
| No `gh` auth | Stop; ask user to `gh auth login` |
| Dirty tree with unrelated WIP | Warn; ask whether to tag current HEAD anyway |
| Build fails mid-suite | Stop; do not publish; paste failing app + log tail |
| Tag already exists | Stop; ask to reuse, bump, or abort |
| Asset upload fails | Keep release; retry `gh release upload`; report missing assets |
