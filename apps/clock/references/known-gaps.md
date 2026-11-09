# Clock — known reference gaps

| Missing / low-fidelity | Should show | Workaround |
|------------------------|-------------|------------|
| `worldclock_list_dark_amber.png` | World clock city list with local times + offsets | Match alarms list typography/spacing; blueprint § Page 5 |
| `worldclock_add_city_dark_amber.png` | City picker / search list | `MetroListItem` + `MetroTextBox` search; blueprint § Page 5 |
| `timer_idle_dark_amber.png` | Timer countdown digits + Play/Reset app bar | Large light digits centered; blueprint § Page 6 |
| `timer_duration_picker_dark_amber.png` | Hours \| minutes \| seconds accent tiles | Reuse `alarms_time_picker_dark_amber.png` tile language; blueprint § Page 8 |
| `stopwatch_dark_amber.png` | Stopwatch elapsed + lap list | Large digits + lap rows; blueprint § Page 7 |
| Light-theme captures for alarms | Same pages in light theme | Match dark captures + theme tokens; validate both themes before merge |
| Sound picker capture | Nokia clock / tone list | Intentionally omitted in v1 — generic bundled tone only |
