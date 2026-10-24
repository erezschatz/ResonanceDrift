# 11 · Now Playing

## Promise
Big art, what's playing, how far in, and the controls. The artist and album
names take me to their screens; the year sits beside the album; a genre line
appears when the song has one and takes me to that genre. I can see and edit
the queue, skip to the next album when shuffling, and copy a diagnostics
report when something misbehaves.

## Acceptance
- [ ] Layout: art, title, `artist` (tappable) · `album · year` (tappable),
      genre line (tappable, first genre; two joined with ` · `; hidden when
      none), seek bar with elapsed/total, prev / play-pause / next
- [ ] Tappable text: no underline, no link colour; pressed-state alpha only
- [ ] Queue panel: list with current highlighted; tap to jump; swipe/button to
      remove; reorder by drag if cheap, otherwise skip
- [ ] Shuffle-album button and "Next album" (wired in step 12)
- [ ] Diagnostics button → `PlaybackLog.report()` to clipboard with a toast
- [ ] Album-name prefix stripping (`AlbumNames.display`) applied here and in
      the media notification metadata
- [ ] Prev while offline plays the previous song from the media cache when
      it was fully loaded (bug from 1.6, see Notes); phone test in airplane mode
- [ ] The copied report is complete: its event lines match the header's count
- [ ] Log out has a home (it sits on the step-07 Now Playing stand-in; D28)
- [ ] Unit test: genre line formatting (none / one / two / three → two)

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
