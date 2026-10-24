# Changelog

One entry per change, written in the same commit as the change. Newest first.
Versions follow `app/build.gradle.kts` (`versionName`); *Unreleased* collects
what has not yet been installed on the phone as a numbered version.

## Unreleased

### 2026-10-24 — Browse: Albums, Artists, Genres and their details (step 08) · 1.8 (9)
- Albums: a random page as a grid (art, name, artist · year), Refresh for
  another page; "Shuffle all" shown disabled until step 12.
- Album detail: art, name, artist (opens Artist detail), songs · year; Play;
  tap a track to play the album from it and open Now Playing.
- Artists and Genres: full cached lists → Artist detail (albums newest first)
  and Genre detail (Play all; albums by year, then name).
- A ViewModel per back-stack entry (`lifecycle-viewmodel-navigation3`) and a
  unique id per stack entry, so the same album can be on the stack twice.
  Decision D29.
- The step-07 "Play a random album" stop-gap is gone; Albums is the real grid.
- Swipe back fades like every other screen change instead of shrinking the
  screen.
- Verified on the phone the same day; step 08 closed.
- Tests: formatting, deep stacks, genre sort (105 JVM, 19 instrumented).

### 2026-10-24 — Step 07 verified on the phone (1.7)
- Tabs, back and predictive back, mini player, Now Playing and Copy
  diagnostics checked on the Pixel. Step 07 closed.

### 2026-10-24 — Navigation, bottom bar, mini player (step 07) · 1.7 (8)
- Five tabs (Albums, Artists, Genres, Playlists, Search) with a back stack
  per tab; back pops within a tab, then to Albums, then out; predictive back.
  Decision D28.
- Mini player above the bar on every tab (art, title, artist, play/pause);
  tap opens Now Playing, a full-screen route without the bars.
- `PlayerConnection` now exposes the current item, queue, index and position.
- Tab screens are placeholders. The dev screen is deleted: "Play a random
  album" is on Albums until step 08; Copy diagnostics and Log out are on
  Now Playing until step 11.
- Coil art keyed by album id from the MediaItem's extras.
- Tests: `Navigator` back rules (6 new, 100 JVM).

### 2026-10-24 — Cached lists follow the server on refetch (step 06)
- A removed playlist's cached songs are dropped with it; a playlist refresh
  skipped because of a removal fetches once more.
- Fix: a finished refresh could be reported with the list as it was before
  the refresh wrote; `Done` now comes with a fresh read.
- Tests: refetch against a fake server (artists, playlist songs, removal
  during a fetch), artist albums and playlist removal (18 instrumented).

### 2026-10-24 — Library data layer (step 06)
- Room `Db` with `albums`, `artists`, `genres`, `songs`, `playlists`,
  `playlist_songs`, `shuffle_history`, `queue_state`; schema exported to
  `app/schemas/`. Decision D27.
- `LibraryRepository`: cache-first `Flow<Listing<T>>` for artists, genres,
  playlists, an artist's albums, an album's songs, a playlist's songs;
  API-only random Albums page and Genre detail; remove song from playlist.
- `playlist_songs` version counter: a removal during a refresh survives it.
- `ShuffleHistory` (newest 50) and `QueueStateStore`, not yet wired in
  (steps 12, 13).
- `StoredLogin.albumCount` from the handshake, for the random page's offset.
- Coil's image loader on the shared OkHttp client; art requests keyed by
  album id, not by the token-bearing URL.
- Tests: refresh flow, offsets, converters, song round-trip, album count
  (JVM, 94 total); DAOs, version counter, 50-cap, queue state (13
  instrumented, run on the API 37 emulator). No version bump: nothing to
  install yet.

### 2026-10-23 — Offline-previous bug attached to step 11
- "Previous song offline did not play" and "one truncated diagnostics copy"
  moved from task 05's notes to step 11 as acceptance criteria.

### 2026-10-23 — Steps 04 and 05 verified on the phone (1.6)
- A full album played through a locked screen and in a pocket over Bluetooth;
  airplane mode, elevator and Wi-Fi handovers all resumed in place. The
  milestone after 04 holds: the rewrite's reason is proven.
- Steps 04 and 05 closed. Kept the 10:15 run's report as
  `docs/logs/2026-10-23-1015-run.txt`. Open notes in task 05: previous song
  offline did not play; one diagnostics copy came out truncated.

### 2026-10-22 — Docs: the phone is a Pixel 10
- `SETUP.md` and task 01 said Pixel 9; corrected. D26 records the correction
  to D6 (decision unchanged).

### 2026-10-22 — Dev screen: random album · 1.6 (7)
- The dev screen loads a random album and offers "Another album", so phone
  tests stream from the network instead of the media cache filled by earlier
  tests (the first album was cached by the 1.4 test).

### 2026-10-21 — Network recovery and playback log (step 05, in progress) · 1.5 (6)
- Step 05 started before step 04 closed, to explain 1.4's locked-screen
  hiccups (D24).
- Failing loads retry ~10 min with backoff instead of failing the song;
  after that `NetworkRecovery` resumes in place for ~30 min and immediately
  when the network returns. 401/403 from the stream renews the token. Never
  skips to the next song. Decision D25.
- `PlaybackLog`: 300 events, persisted, previous run kept; tokens scrubbed.
  `PlaybackEvents` logs state with position and buffer, focus/suppression,
  transitions, loads, audio underruns, errors.
- Dev screen: "Copy diagnostics" (device, version, player state, this run,
  previous run).
- `Session.renewToken()` for the stream's 401/403.
- Tests: ring buffer, rotation, scrubbing, report; retry schedules and
  classification; renewToken (19 new, 86 total).

### 2026-10-21 — Playback service (step 04, in progress) · 1.4 (5)
- `PlaybackService` (`MediaSessionService`, `foregroundServiceType=
  mediaPlayback`) owns one ExoPlayer: audio focus, pause on unplug,
  `WAKE_MODE_NETWORK`, 2–3 min buffers, `rd://song/<id>` → stream URL with the
  current token → 512 MB cache keyed by song → OkHttp.
- `MediaItems`: song metadata (title, artist, album, year, track, duration,
  512 px art) for the notification and lock screen.
- `PlayerConnection`: the UI's MediaController, connected while the app is visible.
- Notification permission asked once on first launch.
- TEMPORARY dev screen replaces the logged-in placeholder: the first album,
  Play album, Pause/Resume, Log out. Removed in step 07. Decision D23.
- Tests: `SongUriResolverTest` (5 new, 67 total).
- Phone (1.4): streaming, notification art, lock-screen controls, unplug,
  Bluetooth and swipe-from-Recents verified. Open: hiccups mid-song while
  locked; full album not yet played through.

### 2026-10-21 — Step 03 done: Ampache client
- `AmpacheClient` covers every action in `docs/AMPACHE.md`: albums, album,
  album_songs, artists, artist, artist_albums, artist_songs, genres, genre,
  genre_albums, genre_songs, playlists, playlist, playlist_songs,
  playlist_remove_song, song, record_play, advanced_search (album by
  title/artist, song by title). `fetchAllPages` pages 5000 at a time.
- DTOs in `ampache/model/` (Album, Song, Artist, Genre, Playlist, Ref) with
  lenient ids and counts; playlist ids are strings. Decision D22.
- `AmpacheUrls.stream` and `AmpacheUrls.art`, pinned to ReDrift's strings.
- The Json configuration moved from `AppGraph` to `AmpacheClient.json` so the
  tests parse with the same one.
- `app/src/test/fixtures/capture.sh` captures the real server's answers;
  18 fixtures captured and committed. They showed single objects at the top
  level, string ids, and a server reporting API 8.1.0 (`docs/AMPACHE.md`).
- Tests: 36 new (fixture parsing, client shapes, URLs, paging); 62 total, all
  passing. No UI changes; no version bump.

### 2026-10-19 — Log out keeps server and username · 1.3 (4)
- Log out now forgets only the password hash, token and expiry; the auth
  screen comes back with server URL and username filled in. Decision D21.
- Tests: 26 total (logout keeps the account; prefill after restart; empty on
  a fresh install).

### 2026-10-19 — Step 02 done; auth screen fixes · 1.2 (3)
- Verified on the phone against the real server (1.1): errors, login, restart
  offline, log out.
- URL and username fields: autocorrect and auto-capitalisation off (Gboard
  inserted a space after each "."); whitespace is also stripped from the URL.
- Errors read plainly ("Wrong username or password.", "Can't reach the
  server…", "…doesn't look like an Ampache server."); the technical detail
  stays in the exception message for logs.

### 2026-10-19 — Login and session (step 02, in progress) · 1.1 (2)
- Auth screen (server URL, username, password, Connect) with inline errors:
  unreachable, plain http blocked, non-JSON answer, wrong credentials.
- `AmpacheClient` handshake + ping; `Session` keeps the token, re-handshakes
  silently on expiry and retries once, pings on foreground when < 20 min
  remain (throttled to once a minute). Decision D20.
- `Settings` (DataStore) stores server, username, password hash, token, expiry.
- Logged-in placeholder screen with Log out, until navigation (task 07).
- Tests: hash vectors, URL building, expiry maths, re-auth-then-retry against
  MockWebServer (19 new, 24 total).

### 2026-10-19 — Step 01 done: toolchain green on the Mac
- First real build: Gradle sync, Assemble Project and `./gradlew test` pass;
  Run ▶ installs on the Pixel 9 and shows the "ResonanceDrift" placeholder.
- AGP 9.4.0 → 9.4.1 in `gradle/libs.versions.toml` (Studio's upgrade prompt
  during the first sync; the build above ran on 9.4.1).
- `docs/SETUP.md`: Build → *Assemble Project* (the old *Make Project*).

### 2026-10-19 — Windows PC demoted to optional backup
- Task 01 acceptance is Mac-only; the Windows check moved to its notes.
- `docs/SETUP.md`, `docs/SPEC.md` and task 14 say Windows is optional. Decision D19.

### 2026-10-19 — Build JDK pinned in the repo
- Added `gradle/gradle-daemon-jvm.properties` (Daemon JVM criteria, JDK 25),
  generated by Studio's *Gradle JVM criteria* setting.
- `docs/SETUP.md` step 3 rewritten: the old "Gradle JDK" setting does not exist
  in current Studio. Decision D18.

### 2026-10-17 — Documentation restructured under the Unities
- `CLAUDE.md` now holds only the working discipline and per-commit checklist.
- Spec moved to `docs/SPEC.md`; setup guide to `docs/SETUP.md`.
- Added `docs/DECISIONS.md` (D1–D17, from the October 12 design session),
  `docs/PLAN.md` with one task file per step in `docs/tasks/`,
  `docs/AMPACHE.md` (the server contract as ReDrift used it) and
  `docs/REDRIFT.md` (the predecessor: location, what carries over, what does not).
- This changelog.

### 2026-10-12 — Project skeleton
- Gradle project: AGP 9.4 (built-in Kotlin), Kotlin 2.4.10, Compose BOM
  2026.09.00, Media3 1.11.1, Room 2.8.5, Navigation 3 1.2.0; versions in
  `gradle/libs.versions.toml`; wrapper pinned to Gradle 9.6.0.
- Shared debug keystore committed; release signing optional via
  `keystore.properties`, falling back to the debug key.
- Manifest with the phase-1 permissions; vector adaptive launcher icon.
- `ResonanceDriftApp`, `AppGraph` (OkHttp + Json), placeholder `MainActivity`,
  light theme carrying ReDrift's colours.
- `ampache/AlbumNames` ("Artist - Album" prefix stripping) with unit tests —
  the first ported behaviour and the toolchain's smoke test.
- Not yet built on a machine with an Android SDK (see task 01).
