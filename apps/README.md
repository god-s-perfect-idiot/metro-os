# Apps

One folder per Android application. Each is self-contained with its own `AGENTS.md`, `references/`, tests, and verify gate.

Each app `README.md` is the detailed implementation brief for that app: page inventory, system functions/contracts, Metro guardrails, reference targets, and test-critical flows. Agents should read `AGENTS.md`, `README.md`, and `references/README.md` before making app changes.

## Per-app references

```
apps/<name>/references/
├── guides/blueprint.md   # Authoritative spec — read first
├── README.md
├── web-resources.md
├── images/             # Visual aids only
└── known-gaps.md       # optional
```

```bash
./scripts/bootstrap-references.sh <name>
```

## Tier 0 — Metro Shell (build first)

| App | Package | AGENTS |
|-----|---------|--------|
| [launcher](launcher/) | `com.metro.launcher` | [AGENTS.md](launcher/AGENTS.md) |
| [statusbar](statusbar/) | `com.metro.statusbar` | [AGENTS.md](statusbar/AGENTS.md) |
| [navbar](navbar/) | `com.metro.navbar` | [AGENTS.md](navbar/AGENTS.md) |

## Tier 1 — Core

| App | Package | AGENTS |
|-----|---------|--------|
| [browser](browser/) | `com.metro.browser` | [AGENTS.md](browser/AGENTS.md) |
| [notes](notes/) | `com.metro.notes` | [AGENTS.md](notes/AGENTS.md) |
| [music](music/) | `com.metro.music` | [AGENTS.md](music/AGENTS.md) |

## Tier 2 — Extended

| App | Package | AGENTS |
|-----|---------|--------|
| [photos](photos/) | `com.metro.photos` | [AGENTS.md](photos/AGENTS.md) |
| [calendar](calendar/) | `com.metro.calendar` | [AGENTS.md](calendar/AGENTS.md) |
| [mail](mail/) | `com.metro.mail` | [AGENTS.md](mail/AGENTS.md) |
| [messaging](messaging/) | `com.metro.messaging` | [AGENTS.md](messaging/AGENTS.md) |
| [people](people/) | `com.metro.people` | [AGENTS.md](people/AGENTS.md) |
| [dialer](dialer/) | `com.metro.dialer` | [AGENTS.md](dialer/AGENTS.md) |
| [store](store/) | `com.metro.store` | [AGENTS.md](store/AGENTS.md) |
| [settings](settings/) | `com.metro.settings` | [AGENTS.md](settings/AGENTS.md) |
| [calculator](calculator/) | `com.metro.calculator` | [AGENTS.md](calculator/AGENTS.md) |
| [clock](clock/) | `com.metro.clock` | [AGENTS.md](clock/AGENTS.md) |
| [files](files/) | `com.metro.files` | [AGENTS.md](files/AGENTS.md) |

## Scaffold a new app

Only after adding the app to `scope.md`:

```bash
./scripts/scaffold-app.sh <name> <tier> <package-suffix>
```

## Verify one app

```bash
./scripts/verify-app.sh <name>
```

## Template

Copy from [`_template/`](_template/) when creating harness docs manually.
