# {{DISPLAY_NAME}}

**Package:** `{{PACKAGE}}`  
**Tier:** {{TIER}}

## Status

Harness docs only — Android project not scaffolded yet.

## Commands

```bash
cd apps/{{APP_NAME}}

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh {{APP_NAME}}
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

_None documented._

## Agent postmortem

_None._
