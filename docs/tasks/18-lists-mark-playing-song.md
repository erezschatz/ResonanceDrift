# 18 · Lists mark the song that is playing

## Promise
Opening an album or a playlist while something from it is playing shows that
song highlighted, in the middle of the screen. The app feels like one piece:
every list knows what the player is doing.

## Acceptance
- [x] Album detail and playlist detail highlight the row whose song is the
      player's current item, the way the queue panel does; the highlight
      moves when the song changes while the screen is open — both screens,
      1.0.3 on the phone, Erez 2026-11-07
- [x] On opening either screen with the current song in its list, the list is
      scrolled so that row sits in the middle of the viewport (not the top);
      a list whose current song is already in view does not jump — both
      screens, 1.0.3 on the phone, Erez 2026-11-07
- [x] The list does not scroll by itself after that: an auto-advance while
      the screen is open moves the highlight only (D42) — Erez, 2026-11-07
- [x] Matching is by song id, not by queue: an album reached from a shuffle
      queue, or a song in several playlists, is marked wherever it appears —
      by construction (`playingRowIndex` on the song id, `PlayingSongTest`);
      the phone run covered an album and a playlist
- [x] A JVM test for the pure parts (which row, what scroll offset centres
      it), as `QueuePanelTest` does for the queue panel —
      `ui/components/PlayingSongTest` (2026-11-07)
- [x] Run on the phone: an album mid-play opened from Albums and from the
      Now Playing links; a long playlist (hundreds of songs) with the current
      song deep inside it; an album with nothing of it playing (no highlight,
      no scroll) — 1.0.3 (21), Erez 2026-11-07: album and playlist both
      land, the list stays put on an auto-advance, an unrelated album shows
      no mark

## Observed (2026-11-03, Erez; review 2026-11-07, F4 and the UI note)
The playlist view does not show which song is playing; after twenty minutes
of hopping between lists, nothing but Now Playing says where the player is.
On 11-02 two taps on a song row while removing songs with the ⋮ menu started
songs "on their own"; a visible mark of the current song in the list makes
such a stray start obvious. Erez, 2026-11-07: a highlight, centred on the
screen, and no more; the collapsed "Now playing / Next up" view D31 rejected
is not wanted ("next up was never implemented, and if it was I never noticed
it").

## Fix (built 2026-11-07, 1.0.3 (21), D42)
`AlbumDetailViewModel` and `PlaylistDetailViewModel` expose `currentSongId`,
the player's current media id as a song id. Both screens paint the `SongRow`
whose song has that id with `primaryContainer`, as `QueuePanel` does, and
call `ScrollToPlayingRowOnce(listState, row + 1)` (the header is item 0) from
`ui/components/PlayingSong.kt`. That effect waits for the list to have a
viewport (task 17's lesson), then, unless the row is already fully visible or
the user has scrolled, scrolls the row to the top to learn its height and
scrolls again with `centeredScrollOffset(viewport, rowHeight)`, a negative
offset of half the spare height. It runs once per screen (`rememberSaveable`,
so a rotation does not repeat it) when the row index first becomes valid,
which is when the songs arrive. `playingRowIndex` and `centeredScrollOffset`
are the pure parts, tested. No service change.

Not verified without a device: that the two instant scrolls in one effect do
not show as a flicker (both happen before the next frame is drawn, as far as
the `LazyListState` contract says), and the look of `primaryContainer` behind
a playlist row's ⋮ button.

## Log
- 2026-11-07: opened from the week review's proposal and Erez's decision
  (D42). Built the same day as 1.0.3 (21): helper, view-model flows, the two
  screens, four JVM tests (148 pass). Awaiting the phone.
- 2026-11-07: 1.0.3 (21) on the phone (install slow after a power failure,
  nothing to do with the app). Erez: album and playlist "both landed" — the
  playing song highlighted and centred on opening, on both screens. No
  flicker reported. Left to confirm: the list stays put on an auto-advance,
  and an album with nothing of it playing shows no mark.
- 2026-11-07: both confirmed by Erez. **Step 18 done.**
