# 07 · Navigation, bottom bar, mini player

## Promise
Five tabs along the bottom — Albums, Artists, Genres, Playlists, Search — and
whatever is playing shows in a strip above them on every screen, tap to open
Now Playing. Back behaves like Android, not like a web page.

## Acceptance
- [ ] Navigation 3: route keys, `NavDisplay`, back stack owned by the app;
      system back pops it; predictive back works
- [ ] Scaffold with bottom bar and `MiniPlayer` (art, title, artist, play/pause)
      above it; both hidden on the auth screen and on Now Playing
- [ ] `PlayerConnection`: `MediaController` wrapped as `StateFlow`s (current
      item, is playing, position, queue); nothing in `ui/` touches ExoPlayer
- [ ] Each tab shows a placeholder screen; Now Playing is a full-screen route
- [ ] The step-04 dev screen is deleted
- [ ] Icons come from `material-icons-extended`; nothing else
