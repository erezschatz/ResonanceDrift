# ReDrift — the predecessor

ReDrift is the Progressive Web App this project replaces. It is the reference
for *behaviour* ("what did the old app do when…") and for nothing else: its
code is JavaScript web components fighting a browser, and none of it is ported
line by line.

## Where it is

- Repo: `ssh://git@phorge.erezschatz.net:2222/source/ReDrift.git`
  (Erez's Phorge). Local clones: `~/dev/ReDrift` on the Mac,
  `C:\Users\ErezSchatz\Downloads\ReDrift` on Windows.
- Last version: **5.13** (Sept 2026). The last functional change was v5.11–5.13,
  all attempts to fix locked-screen playback. Nothing after.
- Its own `CLAUDE.md` is thorough and worth reading once; its `TODO.md` is the
  source of phase-1 additions (steps 1, 2, 4 adopted; 3 parked; 5–7 obsolete).
- Still installed on the phone until ResonanceDrift step 14.

## History (why we are here)

React Native (abandoned: Android Auto support at the time, and RN install
pain) → Kotlin (abandoned: "flying blind") → JS web-component PWA (ReDrift,
Oct 2025 – Sept 2026, 59 commits) → **ResonanceDrift**, native Kotlin again,
this time with the decision log in `DECISIONS.md` so the same ground is not
re-trodden.

## What ReDrift got right (carry over the behaviour)

| Behaviour | Where in ReDrift | Lands in |
|---|---|---|
| Ampache handshake / calls / URLs | `js/services/AmpacheAPI.js` | `AMPACHE.md`, task 02–03 |
| Albums page at a random offset; "Shuffle all" | `js/components/AlbumsView.js` | task 08, 12 |
| Album shuffle with 50-album history, next album prefetched before the boundary | `js/services/AlbumShuffle.js` | task 12 |
| Playlist detail: collapsed "now playing / next up", full list, remove song, version counter against stale refresh | `js/components/PlaylistDetail.js`, `Database.js` (`playlist_songs`) | task 09, 06 |
| Search = three `advanced_search` queries merged | `AmpacheAPI.search()` | task 10 (+ songs) |
| "Artist - Album" prefix stripping | `AudioPlayer.updateMediaSession()` | `ampache/AlbumNames.kt` (done) |
| Cache-first lists (Artists, Songs, Genres), API-only Albums | `Views.js`, `Database.js` | task 06 |
| Queue + position persisted and restored | `AudioPlayer.restoreState/saveState` | task 13 (restored **paused**, D8) |
| `record_play` on song start | `AudioPlayer.loadSong()` | task 13 |
| Diagnostics: ring buffer + one-tap copy of a report | `AudioPlayer` (`getDiagnostics`) | task 05, 11 |
| Media session metadata incl. 512px art | `AudioPlayer.updateMediaSession()` | task 04 (Media3 does it from `MediaItem` metadata) |

## What ReDrift was made of that is NOT ported

All of this existed only because a browser page cannot own playback. If any of
it reappears in ResonanceDrift, the wrong layer is being used (D1):

- 4-songs-ahead full-download window, its gate, its backoff, blob object URLs,
  the `AUDIO_CACHE` in Cache Storage
- Stall watchdog, `stalled`/`waiting` recovery, `_resumeOrReload`, rate-limited
  reloads
- `visibilitychange` / `freeze` / `resume` hooks; "no timers while locked"
- The synchronous-transition rule and the one-`Audio`-element rule
- Service worker, manifest, versioning across three files
- `localStorage`-mirrored event log with rotation (replaced by a file-backed
  `PlaybackLog` in the service)
- `escapeHtml` discipline (Compose has no HTML path)
- Session keep-alive via `visibilitychange` (a foreground ping policy remains,
  but it is a convenience, not a survival tactic)

## ReDrift's TODO.md, disposition

| Step | Content | ResonanceDrift |
|---|---|---|
| 1 | Year on album line; tappable artist/album in Now Playing | task 11 |
| 2 | Genre line + Genre detail + Genres tab | tasks 08, 11 |
| 3 | Lyrics panel | **parked** (D4) — needs a server check for non-empty `lyrics` |
| 4 | Songs in search results | task 10 |
| 5 | Extract diagnostics module | obsolete (new `PlaybackLog`) |
| 6 | Tests for AlbumShuffle handoff | obsolete in form; same intent in task 12's tests |
| 7 | Album pool > 5000 check | solved by paged fetch (task 03, 12) |
