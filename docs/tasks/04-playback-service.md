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
- [x] On the Pixel: full album plays with screen locked; lock-screen prev/next/
      pause work; headphone unplug pauses; Bluetooth play/pause works
- [x] Killing the Activity from Recents does not stop playback
- [x] Unit test: `SongUriResolver` resolves with the current token and reflects
      a token change on the next resolve

## Notes
Do not add shuffle, queue persistence or recovery here; keep the first proof
small. The dev screen is deleted in step 07.

Built 1.4 (5), 2026-10-21: the dev screen plays the first album `albums`
returns (offset 0). From 1.6 it picks a random album and has "Another album":
the media cache keeps everything played, so re-testing the same album would
read from storage, not the network. Not yet run on the phone — the two Pixel criteria are the
real test. When something misbehaves, note what the notification and lock
screen showed; `PlaybackLog` does not exist until step 05.

Phone, 1.4 (5), 2026-10-21 (Erez): streaming works, art shows in the
notification; lock-screen prev/next/pause, headphone unplug, Bluetooth
play/pause and swipe-from-Recents all behave. **Open:** a couple of audible
hiccups mid-song with the screen locked, and the whole album has not yet been
played through (the first album has 7–10 minute tracks). With 2–3 minutes of
buffer a short network drop should be inaudible, so the hiccups are not yet
explained — they need `PlaybackLog` (step 05) to diagnose.

Phone, 1.6 (7), 2026-10-23 (Erez): **step closed.** A 10-song album played
through on a drive with the phone locked and dark ~90% of the time, and again
in a pocket, over Bluetooth; car Bluetooth pause/resume and voice input while
playing all behaved. The 1.4 hiccups did not recur. One perceived ~1-minute
silence before the last song ("Saturn Return", 38 s): the log
(`docs/logs/2026-10-23-1015-run.txt`) shows the transition at 10:41:51.181 and
`ENDED` 38.0 s later, with the song fully cached two minutes earlier — the
player was not stalled, so per D24 the log explains it as far as the player
goes. If it recurs, suspect trailing/leading silence in the files or the
audio output, not playback.
