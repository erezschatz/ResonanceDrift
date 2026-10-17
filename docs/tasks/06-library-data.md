# 06 · Library data layer

## Promise
Lists open instantly from what the app already knows and quietly refresh
behind the scenes. Playlist edits never get undone by a slow background fetch.

## Acceptance
- [ ] Room `Db` with tables `albums`, `artists`, `genres`, `songs`, `playlists`,
      `playlist_songs`, `shuffle_history`, `queue_state`; schema exported to
      `app/schemas/`
- [ ] `LibraryRepository`: emit cached → fetch → upsert → emit; Albums view
      path is API-only (random page), as in ReDrift
- [ ] `playlist_songs` version counter: a removal bumps the version; a refresh
      that started before the bump does not overwrite
- [ ] `ShuffleHistory`: last 50 album ids, trimmed on insert
- [ ] `QueueStateStore`: song ids, index, position, shuffle flag
- [ ] Album art via Coil with the shared OkHttp client; no art blobs in Room
- [ ] Instrumented tests: DAO round-trips; version-counter semantics; 50-cap
