# People

**Package:** `com.metro.people`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the WP8.1 people hub.

## App role

This app recreates the WP8.1 **People** hub: a contact-centered experience with broad overview sections, all-contacts browsing, and filtered navigation.

The app should feel like a Metro hub, not a generic Android contacts manager. The social integration surface mentioned in project scope should be treated as a carefully bounded area, not an excuse to invent new network features.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Contacts permission and local-data strategy approved

## Screen inventory

### 1. People hub

- Landing surface with high-level sections such as all contacts and what’s new
- Expected reference: `references/images/hub_dark_blue.png`

### 2. Contacts pivot / filter surface

- Filter or segmented browsing view for contacts
- Expected reference: `references/images/pivot_dark_blue.png`

### 3. Contact detail

- Detail page for a selected person
- Expected reference: `references/images/detail_dark_blue.png`

## System functions and contracts

- Use `ContactsContract` for local contacts in v1
- Define sorting, grouping, and display-name fallback rules explicitly
- Social integration is part of scope language but should be treated as stubbed or informational until a real backend exists
- Keep avatar loading/fallbacks simple and deterministic

## UI and interaction guardrails

- `MetroHub` for landing organization
- `MetroPivot` only where reference supports filter/category switching
- No Material contact chips, floating add buttons, or rounded avatar card grids
- Large names and left-aligned layout should dominate

## Data and state model

- `PersonSummary`, `PersonDetail`, `ContactMethod`, `PeopleFilter`
- Track selected filter, permission state, loaded contacts, and section loading state

## Primary implementation order

1. Define contacts repository and permission flow
2. Build hub sections
3. Build contact list/filter navigation
4. Build person detail
5. Add any approved social surface stubs

## Test-critical user flows

1. Grant/deny contacts permission and show correct state
2. Load and browse contacts
3. Filter contacts
4. Open person detail and return without losing list context

## Reference and golden expectations

- `references/images/hub_dark_blue.png`
- `references/images/pivot_dark_blue.png`
- `references/images/detail_dark_blue.png`

## Commands

```bash
cd apps/people

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh people
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Social integration surfaces backed by Microsoft/social services | Out of current v1 backend scope | Focus on local contacts first and stub or omit external social feeds with explicit documentation |

## Agent postmortem

_None._
