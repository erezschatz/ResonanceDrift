# 13 · Queue persistence and scrobbling

## Promise
Open the app tomorrow and it shows what I was listening to, at the spot I left
it, paused — one tap resumes. The server's play counts stay correct.

## Acceptance
- [ ] Save on every transition, on pause, and every ~10 s while playing: song
      ids, index, position, shuffle flag (`QueueStateStore`)
- [ ] On service create: rebuild the playlist, `seekTo` saved position,
      **paused** (D8); song metadata rehydrated from Room, falling back to the
      API for ids not cached
- [ ] `record_play` once per song on `onMediaItemTransition`, fire and forget
- [ ] Shuffle flag restored means shuffle continues when the queue ends
- [ ] Phone test: force-stop the app mid-track, relaunch, position within 10 s
