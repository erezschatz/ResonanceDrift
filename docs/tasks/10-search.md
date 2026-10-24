# 10 · Search

## Promise
One search box. Albums come first — by album title, by artist, and albums that
contain a matching song — and below them the matching songs themselves, which
I can play directly.

## Acceptance
- [x] Four `advanced_search` queries in parallel per `docs/AMPACHE.md`
      (album/title, album/artist, song/title → albums, song/title → songs);
      albums de-duplicated by id, capped at 50
- [x] Albums grid, then a Songs section (title, artist · album) hidden when
      empty; "No results" only when both are empty
- [x] Tap album → Album detail; tap song → `setQueue([song])` → Now Playing
- [x] Debounced input; a failed sub-query degrades that section, not the screen

## Notes
ReDrift TODO step 4 is folded in here.

Built 1.11 (12), 2026-10-24. `./gradlew test` (110) and the instrumented
tests on the API 37 emulator (23) pass, including a search where two of the
three queries fail and only their sections degrade. Not yet on the phone.
Decisions in D33.

- Three requests, not four: "albums containing a matching song" and "songs
  by title" are the same `advanced_search`, sent once and read twice.
- Searches from 2 characters, 350 ms after the last keystroke; a newer query
  cancels the older one's search.
- The query and results survive switching tabs (the Search ViewModel belongs
  to the Activity, not the back-stack entry).
- **On the phone, check:** typing gives albums then songs; an album opens
  Album detail; a song plays alone and opens Now Playing; switch tabs and
  back — the search is still there; a nonsense query says "No results".
