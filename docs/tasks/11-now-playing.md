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
- [ ] Unit test: genre line formatting (none / one / two / three → two)

## Notes
ReDrift TODO steps 1 and 2a are folded in here.
