# 09 · Playlists (light)

## Promise
My server-side playlists open fast and play from any song. I can remove a
song from a playlist and it stays removed. Kept light on purpose (D31):
album shuffle is the daily habit, playlists are occasional.

## Acceptance
- [ ] Playlists list (cache-first) with name and song count
- [ ] Playlist detail: the full song list (title, artist, duration); Play
      plays from the top
- [ ] Tap a song → queue is the playlist from that index → Now Playing
- [ ] Remove song: `playlist_remove_song`, then the cache (version bump,
      step 06); while it runs the row is disabled; on failure the row stays
      and the error shows
- [ ] A large playlist (a 2000-song smart playlist made for the test) opens,
      scrolls and starts playing without a visible stall — on the phone

## Not in this step (D31)
ReDrift's collapsed "Now playing / Next up" view with "Show full list", and
optimistic removal. Revisit if playlists become a habit again.

## Notes
Test fixtures on the server: a ~2000-song smart playlist for size (smart
playlists usually refuse removal), and a small ordinary playlist to test
removal on.
