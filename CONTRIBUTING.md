# Contributing

## For humans reviewing agent work

### Review order

1. `deploy/verify-report.json` — must show `"passed": true`
2. Visual spot-check against `apps/<name>/references/images/` (dark + light theme)
3. `docs/DESIGN-CHECKLIST.md` mentally walk through changed screens
4. No new dependencies without scope update
5. Golden screenshot changes require explicit approval

### Golden screenshot updates

Agents **must not** update files in `screenshots/golden/`. Only humans approve golden changes when:

- Reference device capture was wrong, or
- WP8.1 spec in `scope.md` was corrected

Document the reason in the PR description.

### Platform exceptions

If Android cannot replicate WP8.1 behavior, the app README must gain a § Platform exceptions entry:

```markdown
## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|------------------|------------|
| ... | ... | ... |
```

### Merge criteria

- [ ] `verify-app.sh` passes locally
- [ ] Tier dependencies respected (shell before consumer apps)
- [ ] No Material in UI modules
- [ ] Commit messages follow `<app>: <summary>` format

## For agents

See [`AGENTS.md`](AGENTS.md) and [`docs/HARNESS.md`](docs/HARNESS.md).

Do not create commits or PRs unless the user explicitly requests.
