# 13 · Queue persistence and scrobbling

## Promise
Open the app tomorrow and it shows what I was listening to, at the spot I left
it, paused — one tap resumes. The server's play counts stay correct.

## Acceptance
- [x] Save on every transition, on pause, and every ~10 s while playing: song
      ids, index, position, shuffle flag (`QueueStateStore`)
- [x] On service create: rebuild the playlist, `seekTo` saved position,
      **paused** (D8); song metadata rehydrated from Room, falling back to the
      API for ids not cached
- [x] `record_play` once per song on `onMediaItemTransition`, fire and forget
- [x] Shuffle flag restored means shuffle continues when the queue ends
- [x] Phone test: force-stop the app mid-track, relaunch, position within 10 s

## Built 1.16 (17), 2026-10-25
`./gradlew test` (135; 13 new in `ScrobblerTest` and `QueuePersistenceTest`)
and the instrumented tests on the API 37 emulator (31; 2 new in
`QueuePersistencePlayerTest`: a queue saved from one real player comes back
in a fresh one at its song and position, paused; a new empty player does not
overwrite the saved queue) pass. Not yet on the phone. Decision D36.

- `playback/QueuePersistence.kt`, `playback/Scrobbler.kt`, wired in
  `PlaybackService`; `LibraryRepository.songsForQueue`, `Session.awaitLogin`.
- Saves also on seeks, queue edits, and in `onDestroy`; writes on the app scope.
- `record_play` counts a song when it starts playing (see D36 for what is not
  counted). Failures appear as `record_play song-N failed (…)` in the log;
  successes are not logged.
- The log says `queue restored: N songs, item i at Xs[, shuffle on][, M not found]`.
- **On the phone, check:** play something, force-stop the app mid-track
  (Settings → Apps → Force stop), relaunch: the same song within 10 s of where
  it was, paused; one tap plays. Same with shuffle on: shuffle lit, and the
  next album is queued near the end. Swipe the app away while paused, reopen:
  the same. Ampache's recent plays show each song once, and not the restored
  one again on resume.

Phone, 1.16 (17), 2026-10-25 (Erez): **passes**. Force-stop mid-track and
relaunch: the same song, within 10 s, paused; one tap plays. The same with
shuffle on: shuffle lit and carrying on. Ampache's recent plays show each song
once, the restored one not again on resume. Step 13 closed.
