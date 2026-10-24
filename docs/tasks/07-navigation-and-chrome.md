# 07 · Navigation, bottom bar, mini player

## Promise
Five tabs along the bottom — Albums, Artists, Genres, Playlists, Search — and
whatever is playing shows in a strip above them on every screen, tap to open
Now Playing. Back behaves like Android, not like a web page.

## Acceptance
- [x] Navigation 3: route keys, `NavDisplay`, back stack owned by the app;
      system back pops it; predictive back works
- [x] Scaffold with bottom bar and `MiniPlayer` (art, title, artist, play/pause)
      above it; both hidden on the auth screen and on Now Playing
- [x] `PlayerConnection`: `MediaController` wrapped as `StateFlow`s (current
      item, is playing, position, queue); nothing in `ui/` touches ExoPlayer
- [x] Each tab shows a placeholder screen; Now Playing is a full-screen route
- [x] The step-04 dev screen is deleted
- [x] Icons come from `material-icons-extended`; nothing else

## Notes
Built 1.7 (8), 2026-10-24. `./gradlew test` passes (100, 6 new for
`Navigator`); the app installs and opens on the API 37 emulator, but the
logged-in screens could not be checked there (no test server). **On the
phone, check:** the tabs and their back behaviour, predictive back (the
swipe shows the screen back will go to), the mini player's art and
play/pause, Now Playing opening from it and closing with back.

- `ui/nav/Navigator.kt` (routes, tabs, the stacks), `ui/nav/AppScaffold.kt`
  (Scaffold + `NavDisplay` + bars), `ui/components/BottomNav.kt`,
  `MiniPlayer.kt`, `Placeholder.kt`.
- The dev screen's jobs moved, not vanished (D28): "Play a random album" sits
  on the Albums placeholder until step 08; Copy diagnostics and Log out sit
  on Now Playing until step 11. Log out has no planned home after that —
  decide in step 11.
