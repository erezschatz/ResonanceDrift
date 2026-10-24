# 09 · Playlists (light)

## Promise
My server-side playlists open fast and play from any song. I can remove a
song from a playlist and it stays removed. Kept light on purpose (D31):
album shuffle is the daily habit, playlists are occasional.

## Acceptance
- [x] Playlists list (cache-first) with name and song count
- [x] Playlist detail: the full song list (title, artist, duration); Play
      plays from the top
- [x] Tap a song → queue is the playlist from that index → Now Playing
- [x] Remove song: `playlist_remove_song`, then the cache (version bump,
      step 06); while it runs the row is disabled; on failure the row stays
      and the error shows
- [x] A large playlist (a 2000-song smart playlist made for the test) opens,
      scrolls and starts playing without a visible stall — on the phone

## Not in this step (D31)
ReDrift's collapsed "Now playing / Next up" view with "Show full list", and
optimistic removal. Revisit if playlists become a habit again.

## Notes
Test fixtures on the server: a ~2000-song smart playlist for size (smart
playlists usually refuse removal), and a small ordinary playlist to test
removal on.

Built 1.10 (11), 2026-10-24. `./gradlew test` (107) and the instrumented
tests on the API 37 emulator (21, two new for removal: accepted → cache
updated; refused → cache and version untouched) pass. Screens unchecked
there (no test server).

- Removal is behind each row's ⋮ menu ("Remove from playlist"): two taps, so
  a stray tap never deletes. Not offered on smart playlists (`smart_…` ids).
- **On the phone, check:** the Playlists list; the 2000-song smart playlist
  opening, scrolling and playing from a song deep in the list; removing a
  song from the small ordinary playlist (row dims, then leaves); reopening it
  later — the song stays gone.

Phone, 1.10 (11), 2026-10-24 (Erez): **step closed.** Playlists list,
removal (stays gone), and the new icon all landed. A 2718-song smart
playlist played from item 694: queue set, stream answered in 0.64 s on a
new connection (0.24 s of it connecting), playing 1.3 s after the tap; the
next song started loading 2.6 s later.
