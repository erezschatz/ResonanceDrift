# 04 · Playback service — one album through a locked screen

## Promise
I pick an album, press play, lock the phone, put it in my pocket, and the whole
album plays — every track boundary, no gaps, no stalls — with lock-screen
controls and a notification. This is the entire reason for the rewrite; if
this step cannot be made to work, nothing after it matters.

## Acceptance
- [x] `PlaybackService` (`MediaSessionService`) with `foregroundServiceType=
      "mediaPlayback"` declared in the manifest; notification permission asked
      once on first launch
- [x] `PlayerFactory` per `docs/SPEC.md` → Playback: audio attributes with
      focus handling, becoming-noisy, `WAKE_MODE_NETWORK`, generous
      `DefaultLoadControl`, `ResolvingDataSource → CacheDataSource → OkHttp`
- [x] `SongUriResolver`: `rd://song/<id>` → stream URL with the current token;
      `customCacheKey` = song id
- [x] `MediaItems`: Song → `MediaItem` with metadata (title, artist, album, art
      URI) so the notification and lock screen are correct
- [x] A **temporary** dev screen: one hard-coded or first-returned album, a
      play button, nothing else
- [ ] On the Pixel: full album plays with screen locked; lock-screen prev/next/
      pause work; headphone unplug pauses; Bluetooth play/pause works
- [ ] Killing the Activity from Recents does not stop playback
- [x] Unit test: `SongUriResolver` resolves with the current token and reflects
      a token change on the next resolve

## Notes
Do not add shuffle, queue persistence or recovery here; keep the first proof
small. The dev screen is deleted in step 07.

Built 1.4 (5), 2026-10-21: the dev screen plays the first album `albums`
returns (offset 0). Not yet run on the phone — the two Pixel criteria are the
real test. When something misbehaves, note what the notification and lock
screen showed; `PlaybackLog` does not exist until step 05.
