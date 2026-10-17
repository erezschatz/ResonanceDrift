# 12 · Album shuffle

## Promise
"Shuffle all" plays a random album from my whole library; when it ends, another
starts — seamlessly, phone locked, app closed, hours on end — never repeating
any of the last 50. "Next album" skips to a fresh one instantly.

## Acceptance
- [ ] `AlbumShuffle` is a `Player.Listener` in the service; talks to the player
      only through `QueueController` (our interface)
- [ ] Pool: all albums, paged from the API (no 5000 cap), cached in Room
- [ ] Trigger: when the current item is the last or last-but-one in the
      playlist, pick next album (pool minus history), fetch songs, append
- [ ] History: 50, in Room, written when an album *starts*
- [ ] Custom session commands: `SHUFFLE_START(albumId?)`, `SHUFFLE_STOP`,
      `NEXT_ALBUM`; state exposed to the UI via session extras
- [ ] "Shuffle all" (Albums) and shuffle / Next album (Now Playing) wired
- [ ] Any `setQueue` from outside shuffle turns shuffle off
- [ ] Phone test: ≥ 3 album boundaries with the screen locked and the app
      swiped away from Recents
- [ ] Unit tests against `FakeQueueController`: appends near the end and not
      before; respects history; Next album replaces the tail; stop stops
      appending; a failed fetch is retried
