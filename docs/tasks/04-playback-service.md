# 04 · Playback service — one album through a locked screen

## Promise
I pick an album, press play, lock the phone, put it in my pocket, and the whole
album plays — every track boundary, no gaps, no stalls — with lock-screen
controls and a notification. This is the entire reason for the rewrite; if
this step cannot be made to work, nothing after it matters.

## Acceptance
- [ ] `PlaybackService` (`MediaSessionService`) with `foregroundServiceType=
      "mediaPlayback"` declared in the manifest; notification permission asked
      once on first launch
- [ ] `PlayerFactory` per `docs/SPEC.md` → Playback: audio attributes with
      focus handling, becoming-noisy, `WAKE_MODE_NETWORK`, generous
      `DefaultLoadControl`, `ResolvingDataSource → CacheDataSource → OkHttp`
- [ ] `SongUriResolver`: `rd://song/<id>` → stream URL with the current token;
      `customCacheKey` = song id
- [ ] `MediaItems`: Song → `MediaItem` with metadata (title, artist, album, art
      URI) so the notification and lock screen are correct
- [ ] A **temporary** dev screen: one hard-coded or first-returned album, a
      play button, nothing else
- [ ] On the Pixel: full album plays with screen locked; lock-screen prev/next/
      pause work; headphone unplug pauses; Bluetooth play/pause works
- [ ] Killing the Activity from Recents does not stop playback
- [ ] Unit test: `SongUriResolver` resolves with the current token and reflects
      a token change on the next resolve

## Notes
Do not add shuffle, queue persistence or recovery here; keep the first proof
small. The dev screen is deleted in step 07.
