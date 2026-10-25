# 12 · Album shuffle

## Promise
"Shuffle all" plays a random album from my whole library; when it ends, another
starts — seamlessly, phone locked, app closed, hours on end — never repeating
any of the last 50. "Next album" skips to a fresh one instantly.

## Acceptance
- [x] `AlbumShuffle` is a `Player.Listener` in the service; talks to the player
      only through `QueueController` (our interface)
- [x] Pool: all albums, paged from the API (no 5000 cap), cached in Room
- [x] Trigger: when the current item is the last or last-but-one in the
      playlist, pick next album (pool minus history), fetch songs, append
- [x] History: 50, in Room, written when an album *starts*
- [x] Custom session commands: `SHUFFLE_START(albumId?)`, `SHUFFLE_STOP`,
      `NEXT_ALBUM`; state exposed to the UI via session extras
- [x] "Shuffle all" (Albums) and shuffle / Next album (Now Playing) wired
- [x] Any `setQueue` from outside shuffle turns shuffle off
- [x] Phone test: ≥ 3 album boundaries with the screen locked and the app
      swiped away from Recents
- [x] Unit tests against `FakeQueueController`: appends near the end and not
      before; respects history; Next album replaces the tail; stop stops
      appending; a failed fetch is retried

## Built 1.14 (15), 2026-10-24
`./gradlew test` (121; 9 new in `AlbumShuffleTest`) and the instrumented
tests on the API 37 emulator (29) pass — among them `AlbumShufflePlayerTest`:
the real player, one-song albums of 1.5 s, three album boundaries pass on
their own with no album twice. Not yet on the phone. Decision D35.

- `playback/AlbumShuffle.kt` (+ `AppShuffleLibrary`), `QueueController.kt`
  (+ `PlayerQueueController`), `Commands.kt`; wired in `PlaybackService`.
- Pool: `LibraryRepository.albumPool()` pages every album and caches them;
  offline it falls back to the cached ids. Loaded once per service run.
- Four commands, not three: `SHUFFLE_CONTINUE` is Now Playing's shuffle
  button — keep the queue, random albums follow it.
- Next album replaces the queue with a fresh album (and turns shuffle on).
- A queue set by a screen (any controller `setMediaItems`/`addMediaItems`)
  turns shuffle off; shuffle's own appends go to the player directly.
- Stopping shuffle leaves an album it already queued where it is.
- A failed next-album fetch is logged and retried after 30 s; if the queue
  ran out meanwhile, the appended album starts by itself.
- **On the phone, check:** Shuffle all (Albums) and Shuffle (Album detail)
  start an album and the shuffle button lights; Next album; the shuffle
  button off and on in Now Playing; tapping a song in any list turns it off;
  then the real test — ≥ 3 album boundaries with the screen locked and the
  app swiped away from Recents, and a diagnostics copy after (`shuffle:` lines).

Phone, 1.14 (15), 2026-10-24 (Erez), first pass: checks 1–4 pass (Shuffle
all, Album detail's Shuffle, Next album twice, toggle off/on keeping the
queue — next album queued at the last-but-one song — and a song from a list
turning shuffle off). The server answered streams slowly this session
(1.4–3.9 s against 0.2–0.35 s earlier). **Still open:** the field test (≥ 3
album boundaries, screen locked, app swiped from Recents) — Erez, next day,
with a diagnostics copy. With it, two log tweaks: "1 songs" → "1 song", and
the album name in the `shuffle:` lines.

Phone, 1.14 (15), 2026-10-25 (Erez), field test — **passes**. Two runs, screen
locked and the app swiped from Recents. Morning: one service run for 2 h+
(06:51–09:01, no restart), albums 4876 → 9345 → 9896 → the next, each
queued at its last-but-one song; boundaries 28 → 29 and 52 → 53 on their own
(14 → 15 was a skip of the last song). A load timed out twice at 08:06 and
recovered on the third try with 65 s still buffered, unheard. Evening: 9 → 10
on its own; Now Playing's shuffle-on queued at once on the last song; shuffle
off, list taps and Shuffle all as expected. No album twice across nine.
Two oddities, not shuffle's, left open: at 16:29:59 the queue was replaced
three times in 210 ms and the position jumped to 23.6 s (probably taps and a
seek-bar drag; seeks were not logged); at 16:37:02 song 2158 took 40 s to
READY although its first bytes came at 0.2 s (server or file format — check
that song's file type if it recurs).

Built 1.15 (16), 2026-10-25, the log tweaks from the field test: `shuffle:`
lines name the album and say "1 song"; seeks within a song are logged
(`seek @x → y`); the log keeps 1500 events (300 lost the start of a two-hour
run). `./gradlew test` passes (122). Not yet on the phone.
