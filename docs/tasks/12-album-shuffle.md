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
- [ ] Phone test: ≥ 3 album boundaries with the screen locked and the app
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
