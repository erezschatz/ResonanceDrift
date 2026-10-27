# ResonanceDrift

A native Android music player for a self-hosted [Ampache](https://ampache.org)
server. Kotlin, Jetpack Compose, Media3.

It is built for one person, one phone and one music library, and it is meant
to do one thing without fail: keep playing. Through a locked screen, a dropped
connection in a tunnel, an expired server session or a killed app.

## Why it exists

ResonanceDrift replaces **ReDrift**, a Progressive Web App that did everything
wanted of it and failed about 1% of the time: a freeze, a next track that never
started, a stop mid-song when the phone locked. Every one of those had the same
cause. Android does not let a browser page own audio playback. Half of
ReDrift's player was machinery for surviving that (preload windows, stall
watchdogs, lifecycle hooks), and none of it could win.

So the player moved to the one layer where the problem doesn't exist: an
Android `MediaSessionService` that owns ExoPlayer. None of the web app's
survival code was carried over, because nothing here needs it. The behaviour
came across; the code did not.

## What it does

- **Browse** albums (a random page each visit), artists, genres and playlists,
  each with a detail screen
- **Search** albums by title or artist, albums containing a matching song,
  and songs by title, with each section loading and failing on its own
- **Album shuffle**: whole albums in random order, with a 50-album no-repeat
  history. The next album is queued before the current one ends, so the
  change is gapless and needs no UI.
- **Now Playing** with art, tappable artist / album / genre, seek bar, queue
  panel, and a "next album" button
- **Survives the network.** Long buffers, a byte cache that replays without a
  connection, and a recovery policy that retries rather than skipping the song
  or giving up. A pause pressed during an outage is kept.
- **Remembers the queue** and position across restarts, and restores it
  *paused*: it never starts playing on its own.
- Lock-screen, notification, headset and Bluetooth controls, audio focus,
  pause on unplug (Media3 does all of this)
- Scrobbles each play back to the server
- **A playback log** of the last ~10 hours of player events, copied as a report
  with one tap, so a failure can be diagnosed from what actually happened
  instead of guessed at

What it deliberately does *not* do: offline downloads, dark theme, tablets,
Android Auto, Cast, multiple accounts. Each of those has a recorded reason.

## How it is built

```
┌─────────────── UI ───────────────┐      ┌──────── PlaybackService ────────┐
│ Screens ── ViewModels            │      │ MediaSession ── ExoPlayer       │
│    │            │                │      │      │             │            │
│    │      PlayerConnection ◄─────┼──────┼──────┘         cache ── OkHttp  │
│    │      (MediaController)      │      │                                 │
│ LibraryRepository (Room)         │      │ AlbumShuffle · NetworkRecovery  │
│    │                             │      │ QueuePersistence · Scrobbler    │
│ AmpacheClient ◄──────────────────┼──────┼── SongUriResolver · PlaybackLog │
└──────────────────────────────────┘      └─────────────────────────────────┘
```

- **The service owns the player and the queue.** The UI is a client holding a
  `MediaController`; it renders whatever the session says and never touches
  ExoPlayer. If the Activity dies, playback and shuffle carry on.
- **Token-free media URIs.** Queue items are `rd://song/<id>`; a resolver turns
  them into stream URLs with the *current* session token at request time. A
  re-login never rewrites the queue, and the cache stays valid across sessions.
- **Cache-first library.** Screens show what Room has at once, then refresh from
  the server and show the result, with the old rows kept if the refresh fails.
- **Four packages and a budget.** `ampache/` (the server contract), `data/`
  (Room and DataStore), `playback/` (the service and its listeners), `ui/`
  (Compose screens). No `domain/` or `usecase/` layers, no DI framework (one
  hand-wired `AppGraph`), and only libraries that do a rote job without
  imposing an architecture: Media3, OkHttp, kotlinx.serialization, Room,
  DataStore, Coil, Navigation 3.
- **Tested where it matters.** The server's JSON is parsed in tests against
  responses captured from the real server; album shuffle runs against a fake
  queue controller; the retry policy is pure and unit-tested.

## How it was made

The project is also an experiment in working with an LLM without losing the
plot. The rules, called the Unities, are in [`CLAUDE.md`](CLAUDE.md):

- **Place.** Everything about the project lives in this repo: spec,
  decisions, plan, tasks, tests, changelog. None of it lives in chat.
- **Time.** The test, the doc update and the changelog entry go in the same
  commit as the code they describe.
- **Action.** One account of the work. The plan says where things stand, the
  task file says what "done" means, and the changelog says what happened.

A step counts as done only after it has been run on the phone, not when it
compiles.

The documents are the best way into the code:

| | |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | What the app is: scope, architecture, stack |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Every choice with a rejected alternative, dated, with its reason |
| [`docs/PLAN.md`](docs/PLAN.md) and [`docs/tasks/`](docs/tasks/) | The steps, in order, each with its acceptance criteria |
| [`docs/AMPACHE.md`](docs/AMPACHE.md) | The Ampache API as this app uses it |
| [`CHANGELOG.md`](CHANGELOG.md) | What happened, commit by commit |

## Status

Version 1.0 is the release build on the phone and in daily use. Phase 1 closes
with a week of use and fixes, after which the project is meant to go quiet: a
finished tool rather than an ongoing one.

## License

[GPL-3.0](LICENSE)
