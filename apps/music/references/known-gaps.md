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
| `IOS` | `OK`, but only the first ~1 MiB is served | plays fully |
| `ANDROID_VR` **without** `visitorData` | `LOGIN_REQUIRED` | plays fully |
| `ANDROID_VR` **with** `visitorData` | `OK`, uncapped | plays fully |

Two findings drive the implementation:

1. **`visitorData` is what unlocks art tracks.** Sending the Innertube visitor identity as the
   `X-Goog-Visitor-Id` header (and in `context.client.visitorData`) turns `ANDROID_VR` from
   `LOGIN_REQUIRED` into `OK` with plain, uncapped URLs. No BotGuard PoToken is required.
   `YtMusicAuthStore` caches the value for 12 hours; a `LOGIN_REQUIRED` response refetches it once.
2. **Reads must be bounded ranges.** A single unbounded GET is rate limited to roughly 33 KB/s,
   and the `IOS` fallback answers 403 to a range-less or open-ended request. Chunked 512 KiB
   ranges (`playback/ChunkedDataSource.kt`) avoid both: a 10 MB track pulls in 4.3 s.
