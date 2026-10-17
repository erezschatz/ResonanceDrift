# 10 · Search

## Promise
One search box. Albums come first — by album title, by artist, and albums that
contain a matching song — and below them the matching songs themselves, which
I can play directly.

## Acceptance
- [ ] Four `advanced_search` queries in parallel per `docs/AMPACHE.md`
      (album/title, album/artist, song/title → albums, song/title → songs);
      albums de-duplicated by id, capped at 50
- [ ] Albums grid, then a Songs section (title, artist · album) hidden when
      empty; "No results" only when both are empty
- [ ] Tap album → Album detail; tap song → `setQueue([song])` → Now Playing
- [ ] Debounced input; a failed sub-query degrades that section, not the screen

## Notes
ReDrift TODO step 4 is folded in here.
