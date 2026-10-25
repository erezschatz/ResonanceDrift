# ResonanceDrift — Specification

Native Android rewrite of ReDrift (the Ampache PWA). Kotlin, Jetpack Compose,
Media3. Phone only, light theme only, one user.

This is the *what* and the *how*. The *why* behind each choice is in
[DECISIONS.md](DECISIONS.md); the order of work is in [PLAN.md](PLAN.md); the
server contract is in [AMPACHE.md](AMPACHE.md); the predecessor is described in
[REDRIFT.md](REDRIFT.md). When this file and the code disagree, fix one of them
in the same commit.

## Why this project exists

ReDrift did 100% of what was wanted and failed ~1% of the time, always for the
same root cause: a browser page cannot own audio playback on Android. Roughly
half of ReDrift's `AudioPlayer.js` was machinery to survive that — a 4-song
blob preload window, a preload gate, a stall watchdog, `visibilitychange` /
`freeze` / `resume` hooks, a synchronous-transition rule, a localStorage-
mirrored event log, a service worker.

**None of that is ported.** A `MediaSessionService` owning an ExoPlayer makes
every one of those problems not exist. If you find yourself writing code that
"keeps playback alive", stop: it is almost certainly the wrong layer.

## Scope

### Phase 1 (this repo, now)
Feature parity with ReDrift v5.13 **plus** ReDrift's TODO steps 1, 2 and 4:

- Auth against an Ampache 6 server (URL, user, password); session kept alive
- Albums (random page, "Shuffle all"), Album detail
- Artists → Artist detail (albums grid with year)
- **Genres → Genre detail** (albums grid sorted by year, then name; no "Play all", D30)
- Playlists → Playlist detail (large lists, full list, remove song from
  playlist via each row's ⋮ menu; not on smart playlists). Light by choice
  (D31): no collapsed "now playing / next up" view.
- Search: albums by title, albums by artist, albums containing a matching song,
  **and songs by title** (songs section under the albums grid). Three parallel
  requests; each section degrades on its own; the query survives tab switches.
- Album shuffle: whole-album random play, 50-album no-repeat history, next
  album chained before the current one ends, "Next album" button
- Now Playing: art, title, **artist (tappable) · album (tappable) · year**,
  **genre line (tappable, hidden when absent)**, seek bar, prev / play-pause /
  next, queue panel, shuffle-album button, diagnostics copy button
- Mini player, bottom nav (Albums · Artists · Genres · Playlists · Search)
- Queue + position persisted; restored (paused) on next launch
- Album art cached; metadata views render from cache, then refresh
- `record_play` scrobble to the server on each song start

### Explicitly out
- Dark theme, dynamic colour, tablet layouts, Android Auto, Wear, Cast
- Offline / download-ahead. Caching is transparent and incidental (see
  Playback). Temporary connection loss must not stop playback; being fully
  offline is not a use case.
- Lyrics (ReDrift TODO step 3) — parked until the server is confirmed to
  return non-empty `lyrics`
- Ratings / favourites, multiple-genre picker, synced lyrics
- The standalone Songs view (dead in ReDrift; songs live in search)
- Play Store, signing for distribution, multiple accounts / servers

### Phase 2 (not now)
Portfolio polish, a README with screenshots, possibly a release on GitHub.
Nothing in Phase 1 should make that harder; nothing in Phase 1 should be
done *for* it.

## Stack — rote libraries only

A library is acceptable when it solves a rote, already-solved part and does
not impose an application architecture. Nothing here hides control flow.

| Concern | Choice | Not this |
|---|---|---|
| Language | Kotlin (current stable), compiled by AGP 9's built-in Kotlin | A separate `org.jetbrains.kotlin.android` plugin — AGP 9 rejects it |
| UI | Jetpack Compose, Material 3, Navigation 3 (`navigation3-*`; the back stack is a plain list we own) | Views/XML, Navigation 2 (maintenance mode) |
| ViewModel per screen | `lifecycle-viewmodel-navigation3` (same version as the rest of lifecycle): a ViewModel per back-stack entry, cleared when the entry is popped | Activity-scoped ViewModels keyed by hand, which never clear (D29) |
| Icons | `material-icons-extended` (every Material icon, free) | Core `Icons.Default` only — too small, which is what bit the Kotlin attempt last time |
| Playback | Media3: ExoPlayer, `MediaSessionService`, `MediaController` | ExoPlayer 2.x, `MediaBrowserService`, any wrapper |
| HTTP | OkHttp | Retrofit, Ktor |
| JSON | kotlinx.serialization | Gson, Moshi |
| DB | Room (SQLite) | Realm, SQLDelight |
| Prefs | DataStore (Preferences) | SharedPreferences |
| Images | Coil (Compose) | Glide, Picasso |
| DI | **None.** Manual wiring in `AppGraph` | Hilt, Dagger, Koin |
| Async | Kotlin coroutines + Flow | RxJava, LiveData |
| Tests | JUnit 4, kotlinx-coroutines-test, Room testing, Media3 test-utils | Mockito-heavy mocking; prefer fakes we own |

Versions are pinned in `gradle/libs.versions.toml` and nowhere else.
`minSdk = 34`, `compileSdk`/`targetSdk` = current stable.

## Repository layout

The Android SDK dictates `app/src/main/java/<package>/…` and a pile of Gradle
files. Inside our package the structure is flat and by concern. **Do not add
layers** (`domain/`, `usecase/`, `presentation/`) — four packages is the budget.

```
ResonanceDrift/
├── CLAUDE.md                     how to work in this repo (read first)
├── CHANGELOG.md                  one entry per change, written with the change
├── docs/
│   ├── SPEC.md                   this file
│   ├── DECISIONS.md              dated decision log with alternatives rejected
│   ├── PLAN.md                   ordered steps and their status
│   ├── tasks/NN-name.md          one file per step: promise, acceptance, notes
│   ├── AMPACHE.md                the server API exactly as we use it
│   ├── REDRIFT.md                the predecessor: where it is, what carries over
│   └── SETUP.md                  install guide (Mac; Windows as optional backup)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml     every version lives here
├── gradle.properties
├── keystore/debug.keystore       shared debug key (committed — see Signing)
├── keystore.properties           release key paths/passwords — GITIGNORED
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/                 launcher icon, strings, nothing else if possible
        │   └── java/net/erezschatz/resonancedrift/
        │       ├── ResonanceDriftApp.kt   Application; builds AppGraph
        │       ├── AppGraph.kt            all singletons, constructed by hand
        │       ├── MainActivity.kt        single Activity; hosts Compose nav
        │       ├── ampache/               server contract
        │       │   ├── AmpacheClient.kt       handshake, ping, every action
        │       │   ├── AmpacheUrls.kt         API / stream / art URL builders
        │       │   ├── AmpacheException.kt    every failure as a closed set, user-facing messages
        │       │   ├── Session.kt             token + expiry, refresh logic
        │       │   └── model/                 @Serializable DTOs, one file per type
        │       ├── data/                  local state
        │       │   ├── Db.kt                  Room database + migrations
        │       │   ├── entity/                Album, Artist, Genre, Song, Playlist, …
        │       │   ├── dao/
        │       │   ├── LibraryRepository.kt   cache-first reads, refresh writes
        │       │   ├── ShuffleHistory.kt      last 50 album ids
        │       │   ├── QueueStateStore.kt     persisted queue + position
        │       │   └── Settings.kt            DataStore: server URL, credentials
        │       ├── playback/              the part ReDrift could never have
        │       │   ├── PlaybackService.kt     MediaSessionService
        │       │   ├── PlayerFactory.kt       ExoPlayer + cache + data sources
        │       │   ├── SongUriResolver.kt     rd://song/<id> → stream URL w/ live token
        │       │   ├── AlbumShuffle.kt        Player.Listener; appends next album
        │       │   ├── QueueController.kt     the small interface AlbumShuffle talks to
        │       │   ├── QueuePersistence.kt    Player.Listener; saves the queue, restores it paused
        │       │   ├── Scrobbler.kt           Player.Listener; record_play once per song start
        │       │   ├── MediaItems.kt          Song ↔ MediaItem mapping
        │       │   ├── NetworkRecovery.kt     retry-on-error + connectivity callback
        │       │   ├── RecoveryPolicy.kt      retry decisions (pure) + the loader's retry policy
        │       │   ├── PlaybackEvents.kt      ExoPlayer analytics events → PlaybackLog lines
        │       │   ├── StreamTiming.kt        OkHttp timings per stream request → PlaybackLog
        │       │   ├── Commands.kt            custom SessionCommands (shuffle, next album)
        │       │   └── PlaybackLog.kt         1500-entry ring buffer + report
        │       └── ui/
        │           ├── theme/                 light colour scheme, typography
        │           ├── nav/                   route keys, NavDisplay, bottom bar wiring
        │           ├── player/
        │           │   └── PlayerConnection.kt  MediaController as StateFlow
        │           ├── components/            AlbumCard, SongRow, MiniPlayer, BottomNav, …
        │           └── screens/
        │               ├── auth/
        │               ├── albums/            AlbumsScreen, AlbumDetailScreen
        │               ├── artists/           ArtistsScreen, ArtistDetailScreen
        │               ├── genres/            GenresScreen, GenreDetailScreen
        │               ├── playlists/         PlaylistsScreen, PlaylistDetailScreen
        │               ├── search/
        │               └── nowplaying/        NowPlayingScreen, QueuePanel, DiagnosticsSheet
        ├── test/                    JVM unit tests (fast; most tests live here)
        │   └── java/net/erezschatz/resonancedrift/
        │       ├── ampache/           parsing fixtures, URL building, hashing
        │       ├── playback/          AlbumShuffle against FakeQueueController
        │       └── fixtures/          real JSON captured from the server
        └── androidTest/             instrumented: Room DAOs, service smoke test
```

Every screen is `XxxScreen.kt` (composables) + `XxxViewModel.kt` in the same
folder. A ViewModel gets its dependencies from `AppGraph` via a tiny factory;
no reflection, no annotations.

## Architecture

### Process shape
One Activity, one Service. The **service owns the player and the queue**. The
UI is a client: it holds a `MediaController` and renders whatever the session
says. If the Activity is killed, playback and album shuffle are unaffected.

```
┌──────────── UI process-side ────────────┐    ┌──────── PlaybackService ────────┐
│ Screens ── ViewModels ── PlayerConnection│◄──►│ MediaSession ── ExoPlayer        │
│                 │                        │    │     │            │               │
│          LibraryRepository ──────────────┼────┼─► AlbumShuffle   SongUriResolver │
│                 │                        │    │     │            │               │
│           AmpacheClient ◄────────────────┼────┼─────┴── NetworkRecovery          │
└──────────────────────────────────────────┘    └──────────────────────────────────┘
```
(Same process, two lifecycles. `AppGraph` singletons are shared.)

### Playback (`playback/`)

- **`PlaybackService`** extends `MediaSessionService`. Builds the player via
  `PlayerFactory`, wraps it in a `MediaSession`, registers `AlbumShuffle` and
  `NetworkRecovery` as listeners, restores the persisted queue (paused) on
  create. Media3 handles the foreground notification, lock-screen controls,
  headset/Bluetooth buttons, audio focus and "becoming noisy" (pause on
  unplug). Manifest: `foregroundServiceType="mediaPlayback"`.
- **`PlayerFactory`** configures ExoPlayer:
  - `setAudioAttributes(USAGE_MEDIA / CONTENT_TYPE_MUSIC, handleAudioFocus = true)`
  - `setHandleAudioBecomingNoisy(true)`
  - `setWakeMode(C.WAKE_MODE_NETWORK)` — holds CPU + Wi-Fi locks while
    streaming with the screen off. Mandatory.
  - `DefaultLoadControl` with generous buffers (target ~2–3 minutes ahead,
    bounded by memory) so a dropped mobile connection in a tunnel drains the
    buffer, not the music.
  - Data source chain: `ResolvingDataSource(SongUriResolver)` →
    `CacheDataSource(SimpleCache, LRU ~512 MB)` → `OkHttpDataSource`.
    Cached bytes replay without network; nothing is ever downloaded ahead
    beyond ExoPlayer's own buffering. One `SimpleCache` per process (Media3
    requirement) — it lives in `AppGraph`, under `filesDir/media-cache` (D23).
- **`SongUriResolver`.** `MediaItem` URIs are token-free and stable:
  `rd://song/<id>`. The resolver turns that into the real stream URL with the
  *current* session token at request time. Token refresh therefore never
  touches the playlist, and `customCacheKey = song id` keeps the cache valid
  across sessions. It also clears ExoPlayer's `FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN`:
  the OkHttp source knows a length only from `Content-Length`, and without
  this a stream lacking that header is never cached (D34). This replaces ReDrift's `token-refreshed` URL rewriting.
- **`AlbumShuffle`** is a `Player.Listener` living in the service. When shuffle
  mode is on and the current item is the last-but-one (or last) in the
  playlist, it picks the next album (pool minus 50-album history), fetches its
  songs and `addMediaItems` — before the current album ends, so the transition
  is gapless and needs no UI. It talks to the player only through
  `QueueController`, an interface we own, so it is unit-tested with a fake.
  "Next album" replaces the queue with a fresh album. Shuffle state is
  exposed to the UI via session extras + custom commands (`Commands.kt`):
  `SHUFFLE_START(albumId?)`, `SHUFFLE_CONTINUE` (keep the queue, random albums
  after it — Now Playing's button), `SHUFFLE_STOP`, `NEXT_ALBUM`. A queue set
  by a controller turns shuffle off (`onAddMediaItems`). The pool is every
  album, paged and cached, loaded once per service run: the stored ids at once
  when there are at least as many as the handshake's album count, refreshed
  from the server behind them; otherwise fetched first. Offline, the cached
  ids. A failed next-album fetch retries after 30 s (D35, D37).
- **`NetworkRecovery`.** The one place we still "fight": ExoPlayer stops on an
  unrecoverable load error. Policy: a custom `LoadErrorHandlingPolicy` retries
  I/O errors with backoff for a long time instead of failing; if the player
  still enters `STATE_IDLE` with a network-class error, we keep the position,
  `prepare()` + `play()` again on backoff, and immediately when
  `ConnectivityManager` reports a network back. Session-expiry responses
  (401/403 from the stream URL) trigger a re-handshake, then the same retry.
  Never skip to the next item on error — that hides problems and is not what
  the user wants in a tunnel. Numbers (D25): the loader retries ~10 min
  (1→30 s backoff) with the player in BUFFERING; `NetworkRecovery` then tries
  for ~30 min (5→60 s). `prepare()` alone resumes — it keeps playWhenReady, so
  a pause pressed during the outage holds. An HTML/wrong-type answer gets one
  re-handshake before being given up on.
- **`PlaybackLog`.** 1500-entry ring buffer (about ten hours of listening) of player events (state, transition,
  error with cause, recovery attempts, shuffle decisions) persisted to a file
  in app storage, rotated on service start. Now Playing's bug button copies a
  report (device, app version, current state, previous run, this run) to the
  clipboard. Smaller than ReDrift's but the same workflow. Lines are logged by
  `PlaybackEvents` (an `AnalyticsListener`): state with position and buffered
  time, playWhenReady reasons, suppression, transitions, seeks within a song, load start/done/error,
  audio underruns, errors with their cause chain; and by `NetworkRecovery`.
  `ssid=`/`auth=` values are scrubbed from every line. `StreamTiming` (an
  OkHttp `EventListener` on the streaming client only) adds one line per
  stream request: time to the server's answer, to the first audio bytes, and
  whether a new connection was opened.
- **Persistence.** `QueuePersistence` saves `[song ids]`, index, position and
  the shuffle flag to `QueueStateStore` on every transition, queue change,
  seek and stop of playback (pause, focus loss, buffering), every 10 s while
  playing, and in `onDestroy`. Writes run on the app scope, in order, so the
  last one lands after the service is gone. On service create the saved queue
  is rebuilt — songs from Room, a `song` call for any id not cached, missing
  ones left out — and set as a prepared, **paused** playlist at the saved song
  and position; shuffle continues after it if the flag was on. No saves until
  the restore is done (the empty new player would overwrite it); if a screen
  set a queue meanwhile, the restore is dropped. Logged out: nothing restored.
  Never auto-resume: ReDrift's auto-resume was unreliable after a session
  expiry and startled when it worked; the user taps play (D8).
- **`record_play`** (`Scrobbler`) is sent once per song start — at its
  transition when the player is playing, else when play follows — fire and
  forget, failures logged not surfaced. Not counted: the restored song when
  resumed, and a new queue that starts with the song already playing (D36).

### Ampache contract (`ampache/`)
Replicate ReDrift's `AmpacheAPI.js` exactly — it is known to work against the
real server.

- Base: `<server>/server/json.server.php?action=…&auth=<token>`
- Handshake: `auth = sha256(username + sha256(password))`, `version=6.0.0`;
  response carries `auth` (token) and `session_expire`. No timestamp variant.
- Keep-alive: `ping` when the app comes to the foreground with < 20 min left,
  throttled to once a minute (`MainActivity.onStart` → `Session.pingIfStale`).
  A ping answered without `session_expire` means the token is dead. Every API/stream call also extends the session
  server-side, so active playback keeps itself alive.
- Stream: `<server>/play/index.php?ssid=<token>&type=song&oid=<id>&format=raw`
- Art: `get_art&id=<albumId>&type=album&size=<px>`
- Actions used: `albums`, `album`, `album_songs`, `artists`, `artist`,
  `artist_albums`, `artist_songs`, `genres`, `genre`, `genre_songs`,
  **`genre_albums`**, `playlists`, `playlist`, `playlist_songs`,
  `playlist_remove_song`, `song`, `record_play`, `advanced_search`
  (rule `title` / `artist` on type `album`; **`title` on type `song`**;
  operator `0` = contains; limit 50).
- Server max 5000 items per request. The album pool for shuffle is fetched
  paged until a short page comes back (fixes ReDrift TODO step 7 for free).
- Album names may arrive as `"Artist - Album"` when the server disambiguates;
  strip the prefix when it matches the artist, as ReDrift does.
- DTOs are `@Serializable` with `ignoreUnknownKeys = true`. Capture real
  responses into `test/fixtures/` and parse them in tests — the server's JSON
  is the boundary that matters.

### Data (`data/`)
- Room entities mirror what the UI needs, not the server's shape. Tables:
  `albums`, `artists`, `genres`, `songs`, `playlists`, `playlist_songs`,
  `shuffle_history`, `queue_state`.
- `LibraryRepository` is the only thing ViewModels read from. Pattern:
  emit cached rows, fetch, upsert, emit again — as a `Flow<Listing<T>>`
  (the items plus the refresh's state: running, done, or failed with the
  cached items kept). Room's Flows carry the refreshed rows out.
  Cache-first: artists, genres, playlists (full lists, every page), an
  artist's albums (newest first, as the server sends them), an album's songs, a playlist's songs.
  API-only: the Albums view (random page, as in ReDrift; the offset range is
  the handshake's `albums` count, kept in `StoredLogin`) and Genre detail.
  A refresh writes the server's whole answer for that list, so additions,
  changes and removals on the server all show up on the next open.
- `playlist_songs` is one row per playlist (ordered song ids + `version`);
  the songs themselves live in `songs`. It keeps the version counter trick
  from ReDrift: every write bumps the version, and a refresh writes only if
  the version is unchanged since it started, so a removal during a slow
  fetch is not undone.
- `shuffle_history` keeps the newest 50 album ids, trimmed on insert;
  `queue_state` is a single row. No destructive migration fallback: these
  two cannot be re-fetched.
- Album art is Coil's job (memory + disk cache), on the shared OkHttp
  client. Art URLs carry the token, so requests use `album-art-<id>-<size>`
  as the cache key (`ui/components/AlbumArt.kt`). No art blobs in Room.
- Credentials: server URL, username, and the password hash (not the password)
  in DataStore. Single account.

### UI (`ui/`)
- Light theme only: `lightColorScheme(...)` with our palette, `dynamicColor`
  off, system dark mode ignored. Simple and clean; no gradients, no blur.
- `PlayerConnection` wraps the `MediaController` and exposes `StateFlow`s
  (current item, position, playing, queue, shuffle state). Position is read
  twice a second while playing, for display only. Everything in the
  UI reads those; nothing in the UI touches ExoPlayer. It lives in `AppGraph`;
  `MainActivity` connects it in `onStart` and releases it in `onStop` (D23).
- Items sent from the controller lose their URI on the way to the session;
  the service's `onAddMediaItems` rebuilds `rd://song/<id>` from the media id.
- Persistent chrome (mini player, bottom bar) lives in the scaffold around `NavDisplay`.
  Now Playing is a full-screen destination, not an overlay hack.
- Navigation (`ui/nav/Navigator.kt`): one back stack per tab, kept across
  tab switches. Back pops within the tab; at a tab's root it goes to Albums;
  at Albums' root it leaves the app. The stack handed to `NavDisplay` is
  Albums' stack with the current tab's on top, so predictive back previews
  what back will actually show. Tapping the current tab returns to its root.
  Now Playing: art; title; artist and album · year as tappable text (to their
  detail screens); genre line (first two, tap → first genre; hidden when
  none); seek bar; shuffle / prev / play-pause / next / next album; a queue
  bottom sheet (jump, remove); ⋮ menu with Copy diagnostics and Log out
  (confirmed). A link from Now Playing replaces it on the stack.
  While a song is starting (or the buffer refills) the mini player's and Now
  Playing's play/pause shows a spinner; tapping it pauses.
  Every pushed screen gets its own id, so the same route can be on the stack
  twice (album → its artist → the album again) with separate state.
- Browse (step 08): tapping an album card opens Album detail; tapping a
  track there plays the album from it and opens Now Playing. Album detail's
  artist name opens Artist detail.
- The UI keys album art by album id, carried in each `MediaItem`'s metadata
  extras (`MediaItems.albumIdOf`).
- Lists use `LazyColumn` / `LazyVerticalGrid` — virtualisation is free, so
  the 2000-song playlist needs nothing special.
- Every string the server sends is rendered via `Text(...)` — Compose does
  not interpret HTML, so ReDrift's `escapeHtml` discipline has no equivalent
  and no need.
- Tappable text (artist, album, genre in Now Playing) gets no underline or
  link colour — a pressed-state alpha change is enough, matching list rows.

## Permissions (manifest)
`INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
`WAKE_LOCK`, `POST_NOTIFICATIONS` (runtime prompt once; playback works even
if denied — the notification is what disappears), `ACCESS_NETWORK_STATE`.
If the server is plain `http://`, `usesCleartextTraffic="true"` is required;
prefer fixing the server.

## Testing

Contract enforcement at the boundaries, not a unit/integration/e2e taxonomy.

- **`test/` (JVM, runs in seconds, run before every commit):**
  - `ampache/`: every DTO parses the captured fixture; URL builders produce
    the exact strings ReDrift produced; handshake hash matches a known vector.
  - `playback/AlbumShuffleTest`: against `FakeQueueController` — appends when
    near the end and not before; respects history; "next album" replaces the
    tail; stopping shuffle stops appending; a failed fetch retries.
  - `playback/ScrobblerTest`: once per start; not when only queued paused;
    not on pause/resume, a restored song, or a new queue keeping the song;
    again on repeat. `QueuePersistenceTest`: where a restored queue starts
    when songs are missing.
  - `playback/SongUriResolverTest`: resolves with the current token; a token
    change is reflected on the next resolve without touching items.
  - Album-name prefix stripping; genre line formatting (cap at two).
- **`androidTest/`:** Room DAOs (version-counter semantics on
  `playlist_songs`, 50-cap on `shuffle_history`); a service smoke test that
  binds a `MediaController`, sets a two-item playlist of local test assets,
  and asserts transition to item 2.
- Not pursued: Compose screenshot tests, UI automation. Manual testing on the
  Pixel is the acceptance test for Phase 1.

## Signing (two development machines)
Android refuses to update an app signed with a different key — you would have
to uninstall and lose the queue. So:
- `keystore/debug.keystore` is **committed** and both machines' debug builds
  use it (`signingConfigs.debug.storeFile`). Private repo, personal app; fine.
- The release key lives outside the repo on both machines at the same path,
  referenced from the gitignored `keystore.properties`. Copy it between
  machines once (see SETUP.md). Never commit it.

## Versioning
`versionCode` increments by 1 and `versionName` is `1.x` per installable
change, both in `app/build.gradle.kts` only. No other file carries a version.
The diagnostics report prints both.
