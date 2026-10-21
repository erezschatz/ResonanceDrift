# Decisions

Dated, append-only. Each entry: what was decided, what was rejected, why. A
decision is reversed by a new entry that names the one it supersedes — never by
editing the old one. Read this before proposing an alternative that is already
listed as rejected.

Format: `D<n> · YYYY-MM-DD · title` → **Decision** / **Rejected** / **Why**.

---

## D1 · 2026-10-12 · Abandon the PWA; go native

**Decision.** ReDrift (the PWA) is retired as the direction. The music player
is rebuilt as a native Android app under the original name, ResonanceDrift.

**Rejected.** Another round of fixing the PWA's background-playback bug.

**Why.** ReDrift does 100% of what is wanted and works ~99% of the time. The
1% — freezes, the next track not starting, mid-track stops when the phone
locks, stutter without network — has one root cause: Android does not let a
browser page own audio playback. Every fix attempted (a 4-song blob preload
window, a preload gate, a stall watchdog, lifecycle hooks, synchronous
transitions) was negotiating with Chrome's tab lifecycle, and Android does not
negotiate. A `MediaSessionService` owning the player is the only layer where
the problem does not exist. No urgency: ReDrift keeps working meanwhile, and
once ResonanceDrift works it is a finished project needing almost no updates.

## D2 · 2026-10-12 · Kotlin + Media3, not React Native, Flutter or Capacitor

**Decision.** Kotlin, Jetpack Compose, Media3 (ExoPlayer + `MediaSessionService`).

**Rejected.**
- *React Native + react-native-track-player.* Known language; the recent RN
  work (Attic) went smoothly; portfolio value. But RNTP is a community wrapper
  over Media3 with a history of breaking on RN upgrades and a long tail of
  "playback stops on Android N" issues fixed a release late. When it fails you
  debug through two layers in a language you do not read — "flying blind"
  relocated, not removed.
- *Flutter + just_audio/audio_service.* Best cross-platform audio story, but a
  new language for an Android-only personal app.
- *Capacitor wrapping the PWA.* Same WebView, same throttling, same bug.

**Why.** Every option is ExoPlayer underneath; the only question is how many
layers sit between us and it when something goes wrong at 1%. Kotlin + Media3
is zero layers, the most documented and battle-tested Android media code there
is, and the stack trace is the real one. Erez: "I want the best answer" and is
willing to take the Kotlin plunge despite less fluency and more scaffolding
hassle than RN. Portfolio value is deferred to phase 2, not off.

## D3 · 2026-10-12 · Full native UI, no WebView hybrid

**Decision.** Compose for all UI from day one.

**Rejected.** A Kotlin shell (service + Media3) with ReDrift's existing
web-component UI in a WebView over a JS bridge — would have reused ~90% of
ReDrift and kept the Kotlin surface to a few hundred rote lines.

**Why.** Erez, from the Mandy experience: "it's going to be rewritten. There
will be bugs and those bugs will be caused by the shortcut we took, and before
phase 1 is done that 'ever want to' will become 'now'." Do it right once.

## D4 · 2026-10-12 · Phase-1 scope

**Decision.** ReDrift v5.13 feature parity **plus** ReDrift's TODO steps 1, 2
and 4 (year + tappable artist/album in Now Playing; genre line, Genre detail,
Genres tab; songs in search). Genres becomes a browse view like Artists and
Albums. Songs are reachable only through search; the standalone Songs view is
dropped (it was dead code in ReDrift).

**Rejected.** Pure parity (cheaper, but those screens are being rebuilt
anyway); lyrics (TODO step 3 — parked until the server is confirmed to return
non-empty `lyrics`); ratings/favourites; dark theme; tablet; Android Auto;
Play Store.

## D5 · 2026-10-12 · No offline / download-ahead; survive connection loss

**Decision.** No explicit download-ahead and no offline mode. ExoPlayer's
transparent cache (`CacheDataSource` over a bounded `SimpleCache`) is the only
caching. Playback must **not stop** on a temporary connection loss: generous
buffers plus `NetworkRecovery` (retry instead of fail; re-arm when the network
returns).

**Rejected.** Porting ReDrift's 4-songs-ahead download window.

**Why.** The window existed because a locked browser page cannot fetch; a
foreground service can. The real requirement is "driving on a data connection
stays smooth", which buffers and retry satisfy. Erez: anyone wanting offline
"has a stadium looking for a show, not a show looking for a stadium."

## D6 · 2026-10-12 · minSdk 34, phone only

**Decision.** `minSdk = 34`. Phone layouts only.

**Rejected.** Lower minSdk; tablet layouts; Android Auto (explicitly no longer
wanted or used since Sept 2026).

**Why.** The only target device is a Pixel 9; anything older would not run on
it anyway. Fewer API branches.

## D7 · 2026-10-12 · Light theme only, simple and clean

**Decision.** One light colour scheme carrying ReDrift's indigo (#6366F1) and
surfaces; dynamic colour off; the system dark-mode setting ignored.

**Rejected.** Dark theme; Material You dynamic colour.

## D8 · 2026-10-12 · Restore the queue paused; never auto-resume

**Decision.** On launch the persisted queue is restored, prepared at the saved
position, **paused**.

**Rejected.** ReDrift's auto-resume.

**Why.** Erez: it never worked after a session expiry forced a re-login, and
when it did work it was annoying — opening the app to browse started music.
Android also will not let a cold-started service begin audio on its own.

## D9 · 2026-10-12 · Icons from `material-icons-extended`

**Decision.** Depend on `androidx.compose.material:material-icons-extended`
(pinned separately; it left the Compose BOM).

**Why.** The previous Kotlin attempt kept hitting icons that "weren't in the
free version". They were in the core `Icons.Default` set, which is tiny; the
extended artifact has every Material icon and costs nothing.

## D10 · 2026-10-12 · No DI framework; manual wiring in `AppGraph`

**Decision.** One `AppGraph` class constructs every singleton by hand in
dependency order. ViewModels get dependencies through a tiny factory.

**Rejected.** Hilt, Dagger, Koin.

**Why.** Annotation-driven injection is exactly the kind of hidden control
flow that made the last Kotlin attempt feel like flying blind. A file you can
read top to bottom is the architecture.

## D11 · 2026-10-12 · Rote libraries only; no application framework

**Decision.** OkHttp, kotlinx.serialization, Room, DataStore, Coil, coroutines.
Each solves one solved problem and imposes no architecture.

**Rejected.** Retrofit, Ktor, Gson/Moshi, Realm/SQLDelight, RxJava/LiveData.

**Why.** Erez's standing position: a framework is "someone else's opinion",
fine for rote parts, harmful where it hides control flow.

## D12 · 2026-10-12 · Shared debug keystore committed to the repo

**Decision.** `keystore/debug.keystore` is committed; both machines' debug
builds sign with it. The release key stays outside the repo on both machines
(`keystore.properties`, gitignored) and release falls back to the debug key
when absent.

**Rejected.** Per-machine default debug keys.

**Why.** Android refuses to update an app signed with a different key. With
two development machines and one phone, per-machine keys would force an
uninstall (and lose the queue) on every switch. Private repo, personal app —
committing a debug key is harmless.

## D13 · 2026-10-12 · `WAKE_MODE_NETWORK`, unconditionally

**Decision.** ExoPlayer holds CPU + Wi-Fi wake locks while playing.

**Rejected.** `WAKE_MODE_LOCAL` (CPU only) or none.

**Why.** A streaming player with the screen off needs the radio awake; the
locks are held only while playing and released on pause, so the battery cost
is the cost of streaming. This is also precisely the mechanism a browser page
could never ask for.

## D14 · 2026-10-12 · Package name `net.erezschatz.resonancedrift`

**Decision.** Reverse of Erez's domain plus the app name. Fixed from the first
install; changing it later makes Android treat the app as a different one.

## D15 · 2026-10-12 · Navigation 3, not Navigation-Compose

**Decision.** `androidx.navigation3` (stable 1.2.0).

**Rejected.** `navigation-compose` (Navigation 2).

**Why.** Navigation 2 is in maintenance mode. Nav3's model — the back stack is
a plain list the app owns — also fits the no-hidden-control-flow stance.
Supersedes the spec's first draft, which named Navigation-Compose.

## D16 · 2026-10-12 · AGP 9 with built-in Kotlin

**Decision.** AGP 9.x compiles Kotlin itself; no `org.jetbrains.kotlin.android`
plugin. Compose compiler, serialization and KSP plugins remain separate.

**Why.** It is the current default and AGP 9 rejects the old plugin. Versions
are pinned only in `gradle/libs.versions.toml`.

## D17 · 2026-10-17 · Documentation lives in the repo, under the Unities

**Decision.** `CLAUDE.md` is only *how to work here*. The spec, this decision
log, the ordered plan with one file per task, the Ampache contract, the ReDrift
reference and the setup guide live in `docs/`. A changelog is kept at the
root. Every commit updates the relevant ones (see `CLAUDE.md`).

**Rejected.** Keeping spec and rules together in `CLAUDE.md`; keeping decision
history in chat.

**Why.** Erez's working discipline, "the Unities": unity of *place* (everything
about the project is in the repo), *time* (docs, tests, changelog written with
the code, not later — especially with an LLM, whatever is unwritten is lost),
*action* (one account of the work, no parallel narratives). A new session must
be able to pick up from the repo alone.

## D18 · 2026-10-19 · Build JDK pinned in the repo by Daemon JVM criteria

**Decision.** `gradle/gradle-daemon-jvm.properties` is committed and pins the
Gradle daemon to JDK 25 (with per-OS toolchain download links). This file, not
a Studio setting, decides which JDK runs the build.

**Rejected.** The old SETUP step: "set Studio's *Gradle JDK* to the embedded
JetBrains Runtime on each machine". Current Studio no longer has that
dropdown; it has *Gradle JVM criteria*, which writes this file.

**Why.** Unity of place: the build's JDK is a property of the project and
belongs in the repo, not in each machine's IDE settings, where the two
machines drift apart ("it builds for me"). Bytecode target stays Java 17
(`app/build.gradle.kts`); this only chooses the JDK that runs Gradle.

## D19 · 2026-10-19 · The Windows PC is an optional backup

**Decision.** The Mac is the only machine the plan depends on. The Windows PC
may be set up later as a backup; no task's acceptance requires it.

**Rejected.** Task 01 requiring a green build and install from Windows as
well as the Mac.

**Why.** Erez may do little or no work on Windows. Making it mandatory would
block step 01 on a machine that may never be used. The shared debug keystore
(committed) and the JVM criteria file (D18) keep it cheap to add later.

## D20 · 2026-10-19 · Session rules

**Decision.**
- *Logged in* means "credentials are stored", not "the token is valid". A
  restart goes straight past the auth screen, with or without network.
- A dead token (Session Expired envelope, HTTP 401/403, or a `ping` without
  `session_expire`) triggers one silent re-handshake with the stored password
  hash, then one retry of the failed call. Concurrent failures share one
  re-handshake.
- Only a *rejected* re-handshake (the password changed) returns to the auth
  screen, prefilled with server and username. A network failure during
  re-handshake is just an error; the user stays logged in.
- Log out clears local state only; no `goodbye` call.
- A server address typed without a scheme gets `https://`.

**Rejected.** ReDrift's `handleSessionExpired()`, which cleared credentials and
showed the login screen on any expiry (it stored only the token, so it had no
choice). Calling `goodbye` on log out (ReDrift did): it needs the network,
and an orphaned server session expires on its own.

**Why.** Task 02's promise: "I never see a login screen again unless I log out
or the password changes." Storing the password hash is what makes that
possible.

## D21 · 2026-10-19 · Log out keeps server and username

**Decision.** Log out forgets the password hash, token and expiry, and keeps
the server URL and username so the auth screen comes back prefilled. Supersedes
the "clears everything" wording of task 02 and the log-out line of D20.

**Rejected.** Clearing everything (as first built and verified in 1.1).

**Why.** Erez: one server, one user — retyping the address after a log out
is friction with no benefit. The password still has to be typed again.

## D22 · 2026-10-21 · Ampache client shape: stateless calls, lenient numbers, string playlist ids

**Decision.**
- `AmpacheClient` stays stateless: one method per action, each taking
  `server` and `token`. Callers go through `Session.call { server, token -> … }`,
  which owns re-authentication. Paging is a helper (`fetchAllPages`) that takes
  a per-page lambda, so each page re-authenticates on its own.
- Ids and counts are read by small lenient serializers (`model/Lenient.kt`):
  string or number; counts that are `""`/`null`/unreadable become 0; a
  non-numeric id is a shape error (`NotJson`), not a crash.
- Playlist ids are `String`, not `Long`.
- Single-object actions accept `{key: x}`, `{key: [x]}` and a bare top-level `x`.

**Rejected.** A stateful client holding the token (would duplicate
`Session`'s job and its re-auth rule); `Long` playlist ids as `AMPACHE.md`
first said; relying on `Json { isLenient }` alone for quoted numbers.

**Why.** Ampache lists smart playlists with ids like `smart_12`; a `Long` would
fail the whole `playlists` call the day one exists. Ids have been strings in
Ampache 6 and numbers before; one serializer reads both regardless of how the
JSON decoder is configured. The fixtures (task 03) will show which forms this
server actually uses.

## D23 · 2026-10-21 · Playback service wiring details (step 04)

**Decision.**
- `PlayerConnection` (the UI's `MediaController`) is a process singleton in
  `AppGraph`, connected only while the Activity is started (`onStart`/`onStop`).
- The media cache lives in `filesDir/media-cache`, bounded by the 512 MB LRU
  evictor.
- `onTaskRemoved` is not overridden: Media3's default keeps the service when
  swiped from Recents while playing and stops it when paused.
- The notification art URL (`get_art`, 512 px) is fixed when the MediaItem is
  built, token included; only the stream URL is resolved per request.
- `DefaultLoadControl` buffers 120–180 s with time prioritised over the byte cap.
- The notification permission is asked once ever (a flag in `Settings`).

**Rejected.** A controller kept bound for the whole process lifetime (keeps a
paused service alive in the background for no reason); `cacheDir` (Android
may delete files there behind `SimpleCache`'s index); a resolver for art too
(it would mean a custom Media3 `BitmapLoader` now, for a cost — a missing
notification picture after a token change — that task 13 removes by rebuilding
items on restore); asking for notifications on every launch.

**Why.** Smallest proof for step 04 that follows SPEC → Playback; each of these
is cheap to change if the phone says otherwise.
