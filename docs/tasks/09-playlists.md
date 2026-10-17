# 09 · Playlists

## Promise
My server-side playlists, including the 2000-song ones, open fast and play
from any song. I can remove a song from a playlist and it stays removed.

## Acceptance
- [ ] Playlists list (cache-first) with name and song count
- [ ] Playlist detail: default collapsed view ("Now playing" + "Next up") when
      this playlist is what is playing, with "Show full list"; otherwise the
      full list; Play button plays from the top
- [ ] Remove song: `playlist_remove_song`, optimistic UI, version bump (step 06)
- [ ] Tap a song → queue is the playlist from that index → Now Playing
- [ ] Error toast on failed removal; the row comes back
