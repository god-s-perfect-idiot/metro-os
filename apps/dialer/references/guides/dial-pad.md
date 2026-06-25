# Dial pad & in-call

Supplementary guide for keypad and active call surfaces. Authoritative layout: [`blueprint.md`](blueprint.md).

## Keypad layout (WP 8.1)

WP 8.1 moved **call** and **save** from the bottom app bar into the tile grid — do not put call/save in the app bar on the dial pad page.

### Grid structure

```
[ 1   ] [ 2   ] [ 3   ]
        ABC
[ 4   ] [ 5   ] [ 6   ]
        DEF   GHI
[ 7   ] [ 8   ] [ 9   ]
        PQRS  TUV  WXYZ
[  *  ] [ 0 + ] [  #  ]
[ call (accent) ] [ save (border) ]
```

- Tiles are **square**, separated by 4dp gaps.
- Digit: 36sp Light centred; letter hints: 10sp grey below digit.
- `0` tile shows small `+` hint; long-press inserts `+`.
- **Call** tile: accent background, white `call` label.
- **Save** tile: 2dp white border, white `save` label — opens add-contact flow (stub in v1).

### T9 / autocomplete

As digits are entered, show up to **3 matching contacts** above the grid:

- Match against contact name (T9 mapping) and number prefix.
- Row: name (24sp) + number (grey 16sp).
- Tap suggestion: populate number field; second tap or explicit call button places call.

Reference: `images/dialpad_dark_blue.jpg`.

## In-call screen

WP 8.1 enlarged the outgoing/in-call UI:

- Photo or blank avatar upper-left/center.
- Name/number dominates vertical space.
- **End call** is a large red circular control at the bottom — not a border button.
- **Video** button appeared in 8.1 for Skype/VOIP handoff — stub disabled in v1.

During outgoing ring, status reads `calling…`. After connect (or simulated connect in harness), show elapsed timer `mm:ss`.

Reference: `images/in_call_dark_blue.png`.

## End-call behavior

- Terminate telephony session (`TelecomManager` / `ACTION_CALL` lifecycle stub acceptable in v1 emulator).
- Pop route stack to dial pad or history depending on entry point.
- System Back during ring ≡ End call.
