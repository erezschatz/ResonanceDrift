# 08 · Browse: Albums, Artists, Genres and their detail screens

## Promise
Albums shows a fresh random page of my library each time with a "Shuffle all"
button. Artists and Genres are lists I can tap into; each detail screen shows
that artist's or genre's albums as a grid with art and year. Tapping an album
shows its tracks; tapping a track plays the album from there.

## Acceptance
- [ ] Albums: random offset page from the API; grid of cards (art, name,
      artist, year); "Shuffle all" button (wired in step 12; disabled until then)
- [ ] Album detail: header with art, name, artist, year; track list with
      numbers and durations; tap → `setQueue(album songs, index)` and navigate
      to Now Playing
- [ ] Artists list (cache-first) → Artist detail: albums grid, server order
- [ ] Genres list (cache-first) → Genre detail: **Play all** button; albums
      grid via `genre_albums` sorted by year ascending then name
- [ ] Lists of thousands scroll smoothly (`LazyColumn`/`LazyVerticalGrid`)
- [ ] Every server string rendered through `Text` (no HTML path exists)

## Notes
ReDrift TODO step 2b/2c are folded in here (Genre detail, Genres tab).
