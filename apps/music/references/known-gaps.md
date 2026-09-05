# Music — known gaps

High-fidelity WP8.1 captures missing or low-fidelity for blueprint pages. Prefer filling these before golden polish.

| Missing / weak file | Should show | Workaround |
|---------------------|-------------|------------|
| Full `collection` panorama pane (recent plays + section links) without now-playing chrome | Collection hub content | Use `hub_nowplaying_compare_dark_unknown.jpg` + blueprint § Page 2; artists showing for list chrome |
| Dense artists A–Z list (many rows) | Long letter list + jump list | `artists_showing_dark_teal.jpg` shows header + `#` jump tile; implement `MetroLetterList` |
| Playlists pivot | Playlist titles list | Same list chrome as artists; WP text sources in Thurrott Showing article |
| Genres pivot | Genre names | Same as playlists |
| Radio panorama pane | Station list / create station | Blueprint stub; YT Music search “radio” when connected |
| Explore / Store pane | Featured albums | Blueprint Page 9; YT Music search results |
| Queue full-screen list | Now playing queue reorder | Icons on now playing + `Up next` line in `hub_nowplaying_dark_green.jpg` |
| Light theme Music captures | Light bg Music UI | Dark refs + `MetroTheme` light tokens |

Do not start UI against an empty `images/` folder — primary now-playing and showing captures are present.

## Resolved — YouTube Music stream resolution

Measured against Innertube on 2026-08-05. Anonymous player clients, art track
(a `music.youtube.com` upload) versus a regular video:

| Client | Art track | Regular video |
|--------|-----------|---------------|
| `ANDROID_MUSIC` | `LOGIN_REQUIRED` | `LOGIN_REQUIRED` |
| `WEB_REMIX` | `UNPLAYABLE` (`OK` but cipher-only when signed in) | ciphered formats only |
| `TVHTML5` / `WEB` signed in | `OK` but SABR-only, no stream URLs | — |
| `IOS` | `OK`, progressive **403 past ~1.5 MiB** (no HLS on art tracks) | HLS / progressive past 1.5 MiB |
| `ANDROID_VR` **without** `visitorData` | `LOGIN_REQUIRED` | plays fully |
| `ANDROID_VR` **with** `visitorData` | `OK`, adaptive GVS **403 past ~1 MiB** without PO token | 403 past ~1 MiB without PO |

Two findings drive the implementation (updated 2026-09-06):

1. **`visitorData` unlocks art tracks on mobile player clients.** Send it as `X-Goog-Visitor-Id`
   and in `context.client.visitorData`. `YtMusicAuthStore` caches it for 12 hours.
2. **Catalog art tracks need a GVS PO token past ~1 MiB on every Innertube client we tried.**
   Without it, Range requests at byte ≥ ~1.5 MiB return 403 — ExoPlayer stops near **0:48–1:04**
   depending on bitrate. The fix is NewPipe-style **BotGuard PO minting** (`YtPoTokenSession`):
   mint a streaming pot bound to `visitorData`, append `pot=`/`potc=1` on googlevideo URLs, and
   optionally send a video-bound pot in `serviceIntegrityDimensions.poToken`. Innertube order is
   IOS → ANDROID_VR → WEB_REMIX with a mid-file Range probe; progressive fallback still starts
   audio if minting fails.
3. **Reads must be bounded ranges.** `YtStreamLogic` stamps `clen` from `contentLength` when the
   signed URL omits it; `ChunkedDataSource` reads 512 KiB Ranges. Premature upstream EOS with
   bytes still remaining is raised as an error instead of ending the song.
