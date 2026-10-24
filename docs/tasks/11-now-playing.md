# 11 · Now Playing

## Promise
Big art, what's playing, how far in, and the controls. The artist and album
names take me to their screens; the year sits beside the album; a genre line
appears when the song has one and takes me to that genre. I can see and edit
the queue, skip to the next album when shuffling, and copy a diagnostics
report when something misbehaves.

## Acceptance
- [x] Layout: art, title, `artist` (tappable) · `album · year` (tappable),
      genre line (tappable, first genre; two joined with ` · `; hidden when
      none), seek bar with elapsed/total, prev / play-pause / next
- [x] Tappable text: no underline, no link colour; pressed-state alpha only
- [x] Queue panel: list with current highlighted; tap to jump; swipe/button to
      remove; reorder by drag if cheap, otherwise skip
- [x] Shuffle-album button and "Next album" (wired in step 12)
- [x] Diagnostics button → `PlaybackLog.report()` to clipboard with a toast
- [x] Album-name prefix stripping (`AlbumNames.display`) applied here and in
      the media notification metadata
- [x] Prev while offline plays the previous song from the media cache when
      it was fully loaded (bug from 1.6, see Notes); phone test in airplane mode
- [x] The copied report is complete: its event lines match the header's count
- [x] Log out has a home (it sits on the step-07 Now Playing stand-in; D28)
- [x] Unit test: genre line formatting (none / one / two / three → two)

## Notes
ReDrift TODO steps 1 and 2a are folded in here.

**Bug carried from step 05 (1.6, 2026-10-23):** in airplane mode, going back
to the previous song did not play it, although that song had been fully
loaded and the 512 MB media cache is keyed by song id, so it should play from
storage. Not yet explained, and there is no log of it. To explore: reproduce in
airplane mode, copy diagnostics right after, and see whether the `load` lines
for that song show a cache miss (a network open that fails) or something else,
such as the player sitting in an errored state. The fix belongs in `playback/`,
not in this screen's button.

**Truncated report (1.6):** two copies of one run came out cut mid-line at
~20 KB; a later copy of the same run was complete. Unclear whether the app or
the paste cut it. If the app is at fault, write the report to a shareable file.

## Built 1.12 (13), 2026-10-24
`./gradlew test` (112) and the instrumented tests on the API 37 emulator (28)
pass. The screens could not be checked there (no test server). Decision D34.

- Layout as specified; links close Now Playing and open Artist / Album /
  Genre detail (the genre line shows up to two, a tap goes to the first).
  Album detail's artist uses the same tappable text now.
- Seek bar follows the finger while dragged, seeks on release.
- Queue panel (bottom sheet): current item highlighted and scrolled to; tap
  to play; ✕ to remove. No reordering (not cheap).
- Shuffle and Next album are shown, disabled, until step 12.
- ⋮ menu: Copy diagnostics (Android's own "copied" confirmation is the toast)
  and Log out, behind a confirmation dialog.
- Prefix stripping: already in `MediaItems` for the notification since 1.4;
  the QueueEntry carries the stripped name.
- Report completeness: every report pasted on 2026-10-24 (six) had as many
  event lines as its header said. Not reproduced since 1.6; no change made.

**The offline-previous bug, found.** Media3's OkHttp source takes a song's
length only from the `Content-Length` header; ExoPlayer marks progressive
loads "don't cache if the length is unknown"; so every stream that arrived
without that header was never cached. On the phone the media cache held 27 MB
in about four files after a day of listening (read with `run-as`, read-only).
Step 04's note that "the media cache keeps everything played" was an
assumption, and wrong. `SongUriResolver` now clears that flag (songs are
finite; the cache stores the length at the end of the file). Reproduced and
fixed in `OfflinePreviousTest` against a fake server, with and without the
header. The `stream` log line now says the length, or "no length".

**On the phone, check:** the layout and its three links; seek by dragging;
the queue panel (jump, remove); Copy diagnostics and Log out (Cancel, then
for real if you like — the server and username stay filled in); then play
two songs online, airplane mode, previous → the first song plays.

Phone, 1.12 (13), 2026-10-24 (Erez): **step closed.** Layout and links, seek,
queue panel, diagnostics and log out all good; **airplane mode → previous
played the earlier song from the cache.** Every `stream` line said "no
length": Ampache sends no Content-Length, which is exactly why nothing was
cached before 1.12. Asked for: the screen was bunched at the top with a large
empty gap below, and the buttons were small (the shuffle ones most). 1.13
spreads art / song lines / seek + controls over the height and makes the
controls 40 dp icons in 56 dp targets, play/pause 64 dp.
