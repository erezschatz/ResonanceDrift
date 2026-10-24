# 08 · Browse: Albums, Artists, Genres and their detail screens

## Promise
Albums shows a fresh random page of my library each time with a "Shuffle all"
button. Artists and Genres are lists I can tap into; each detail screen shows
that artist's or genre's albums as a grid with art and year. Tapping an album
shows its tracks; tapping a track plays the album from there.

## Acceptance
- [x] Albums: random offset page from the API; grid of cards (art, name,
      artist, year); "Shuffle all" button (wired in step 12; disabled until then)
- [x] Album detail: header with art, name, artist, year; track list with
      numbers and durations; tap → `setQueue(album songs, index)` and navigate
      to Now Playing
- [x] Artists list (cache-first) → Artist detail: albums grid, server order
- [x] Genres list (cache-first) → Genre detail: **Play all** button; albums
      grid via `genre_albums` sorted by year ascending then name
- [x] Lists of thousands scroll smoothly (`LazyColumn`/`LazyVerticalGrid`)
- [x] Every server string rendered through `Text` (no HTML path exists)

## Notes
ReDrift TODO step 2b/2c are folded in here (Genre detail, Genres tab).

Built 1.8 (9), 2026-10-24. `./gradlew test` (105) and the Room/repository
tests on the API 37 emulator (19) pass; the app opens there, but the
screens themselves need the phone (no test server). Decisions in D29.

- Artist detail is newest first, then by name: that is how the server sends
  `artist_albums` (the fixture's order), kept by sorting rather than by
  storing the server's positions. Replaces D27's "year, then name".
- Tapping an album opens Album detail, as this task says. ReDrift played the
  album straight from the card; Album detail's Play does that in one more tap.
- "Shuffle all" (Albums) and "Shuffle" (Album detail) are shown disabled until
  step 12.
- **On the phone, check:** the grid's art and the Refresh button; Album
  detail's header, Play, and tap-a-track; artist name → Artist detail; the
  Artists and Genres lists (scroll the full lists — the criterion left open);
  Genre detail's sort and Play all; back through a deep stack (album → artist
  → album).

Phone, 1.8 (9), 2026-10-24 (Erez), first pass: back through Genres → genre →
album → artist and back out again works as expected. The default swipe-back
animation (screen shrinks) was unwelcome; now the same fade as other moves.

Phone, 1.8 (9), 2026-10-24 (Erez): **step closed.** Albums grid and Refresh,
Album detail (Play, tap a track, artist → Artist detail), the full Artists
and Genres lists scroll without hiccups, Genre detail's order, deep back
stacks and the new swipe fade all behave. Open, not blocking: starting an
album takes about 2 s — the log puts it between the stream request and the
first audio (app's own part under 0.1 s), not yet split into server vs
network; "Play all" on Genre detail came from ReDrift's
TODO step 2b, not from a new request.

After sign-off, 1.9 (10): "Play all" removed from Genre detail at Erez's
request (D30; it came from ReDrift's TODO, not from him). The criterion above
is left as written and ticked for the record.
