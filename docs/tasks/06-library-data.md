# 06 · Library data layer

## Promise
Lists open instantly from what the app already knows and quietly refresh
behind the scenes. Playlist edits never get undone by a slow background fetch.

## Acceptance
- [x] Room `Db` with tables `albums`, `artists`, `genres`, `songs`, `playlists`,
      `playlist_songs`, `shuffle_history`, `queue_state`; schema exported to
      `app/schemas/`
- [x] `LibraryRepository`: emit cached → fetch → upsert → emit; Albums view
      path is API-only (random page), as in ReDrift
- [x] `playlist_songs` version counter: a removal bumps the version; a refresh
      that started before the bump does not overwrite
- [x] `ShuffleHistory`: last 50 album ids, trimmed on insert
- [x] `QueueStateStore`: song ids, index, position, shuffle flag
- [x] Album art via Coil with the shared OkHttp client; no art blobs in Room
- [x] Instrumented tests: DAO round-trips; version-counter semantics; 50-cap

## Notes
Built 2026-10-24, no UI (screens arrive in 07–11), not installed on the phone;
`versionCode` unchanged. Verified by `./gradlew test` (94 JVM tests) and
`connectedDebugAndroidTest` on the API 37 emulator (13 Room tests). Choices
with rejected alternatives are D27.

- `data/entity/` one file per table; `data/dao/` four DAOs (`LibraryDao`,
  `PlaylistSongsDao`, `ShuffleHistoryDao`, `QueueStateDao`); `data/Db.kt`
  holds the database and its two converters.
- Cache-first, as `Flow<Listing<T>>` (items + `Refresh` state): artists,
  genres, playlists (full lists, all pages), an artist's albums, an album's
  songs, a playlist's songs. API-only: the random Albums page (offset range
  from the handshake's album count, now in `StoredLogin`), Genre detail's
  albums and songs. Search stays in step 10.
- `ui/components/AlbumArt.kt` builds the Coil request with an album-id cache
  key; the loader (`AppGraph.imageLoader`) is installed by `ResonanceDriftApp`.
  First used on screen in step 08.
- Not done here: wiring `ShuffleHistory` (step 12) and `QueueStateStore`
  (step 13) into the service; clearing the cache on log out (single account,
  same server — D21).
