# Ampache API — as ResonanceDrift uses it

The server is Ampache 6 (API version string `6.0.0`), JSON flavour. This file
records **exactly** what ReDrift did against the real server, because that is
known to work. Where the official docs and this file differ, this file wins
until a fixture from the real server proves otherwise. Official reference:
https://ampache.org/api/api-json-methods/ (Ampache 6).

## Base URLs

| What | URL |
|---|---|
| API | `<server>/server/json.server.php?action=<action>&auth=<token>&<params>` |
| Stream | `<server>/play/index.php?ssid=<token>&type=song&oid=<songId>&format=raw` |
| Art | API action `get_art` with `id=<albumId>&type=album&size=<px>` (so it carries `auth` like any other call) |

`<server>` is the user-entered URL with any trailing `/` removed. Everything is
GET. `<token>` is the session token from the handshake.

## Authentication

### Handshake (no timestamp variant)
```
action=handshake
auth=sha256( username + sha256(password) )      -- lowercase hex, no salt, no timestamp
version=6.0.0
```
No `user` and no `timestamp` parameter — ReDrift used the username-in-hash
form and it works on this server. Response (fields we read):
```json
{ "auth": "<token>", "session_expire": "2026-10-12T15:04:05+00:00", "api": "6.x.x",
  "songs": 1234, "albums": 234, "artists": 56, "playlists": 7, ... }
```
`auth` and `session_expire` are required; the counts are useful (library size
for the album pool) but optional.

### Keep-alive
```
action=ping&auth=<token>
```
Returns a fresh `session_expire`. Every API call and every stream request also
extends the session server-side, so active playback keeps itself alive. Policy
(from ReDrift): ping when the app returns to the foreground with < 20 minutes
left, throttled to once per minute. Timers are not needed natively but the
foreground-ping policy is cheap and stays.

### Expiry
A call with a dead token returns the error envelope with
`errorMessage` containing `Session Expired`. A stream URL with a dead token
returns a non-2xx (treat 401/403, or an HTML body, as expiry). Either →
re-handshake with stored credentials → retry the call once.

### Error envelope
Any action may return, with HTTP 200:
```json
{ "error": { "errorCode": 4701, "errorMessage": "Session Expired", "errorAction": "albums", "errorType": "account" } }
```
ReDrift also rejected any response whose `Content-Type` is not
`application/json` (misconfigured proxies return HTML login pages).

## Actions

Common params: `filter` (an id), `offset`, `limit` (server max **5000**; a
larger value is silently capped, so page until a short page comes back).

| Action | Params | Response key | Notes |
|---|---|---|---|
| `albums` | `offset`, `limit` | `album[]` | Albums screen uses a random `offset` for variety |
| `album` | `filter=id` | `album` or `album[]` | Server sometimes wraps a single object in an array — unwrap both |
| `album_songs` | `filter=albumId` | `song[]` | Track order as given |
| `artists` | `offset`, `limit` | `artist[]` | |
| `artist` | `filter=id` | `artist` / `artist[]` | |
| `artist_albums` | `filter=artistId` | `album[]` | Keep server order |
| `artist_songs` | `filter=artistId` | `song[]` | Unused in phase 1 UI; kept in the client |
| `genres` | `offset`, `limit` | `genre[]` | |
| `genre` | `filter=id` | `genre` / `genre[]` | |
| `genre_songs` | `filter=genreId` | `song[]` | Genre detail "Play all" |
| `genre_albums` | `filter=genreId` | `album[]` | **New in ResonanceDrift** (ReDrift TODO step 2b); sort by year asc, then name |
| `playlists` | `offset`, `limit` | `playlist[]` | |
| `playlist` | `filter=id` | `playlist` / `playlist[]` | |
| `playlist_songs` | `filter=playlistId` | `song[]` | 2000+ rows is normal |
| `playlist_remove_song` | `filter=playlistId`, `song=songId` | success envelope | Only write call in the app |
| `song` | `filter=id` | `song` / `song[]` | Also where `lyrics` would be (phase 2 check) |
| `record_play` | `id=songId`, `client=ResonanceDrift` | success envelope | Fire and forget, once per song start |
| `advanced_search` | see below | `album[]` or `song[]` | |

### advanced_search
Four queries run in parallel for one search string `q`:

| Purpose | Params |
|---|---|
| Albums by title | `type=album&operator=and&rule_1=title&rule_1_operator=0&rule_1_input=q&limit=50` |
| Albums by artist | `type=album&operator=and&rule_1=artist&rule_1_operator=0&rule_1_input=q&limit=50` |
| Albums containing a matching song | `type=song&operator=and&rule_1=title&rule_1_operator=0&rule_1_input=q&limit=50` → take each song's `album` |
| Songs by title (new) | same as above, but the songs themselves are the result |

`rule_1_operator=0` means *contains*. Albums are merged by id (title results
first, then artist, then from songs), capped at 50. An album synthesised from a
song result has only `{ id, name, artist, year }` — enough for a card.

## Object shapes (fields we read)

Only fields ReDrift actually read plus the ones phase 1 adds. Everything else
is ignored (`ignoreUnknownKeys`). Ids are integers in Ampache 6 JSON but have
been strings in older versions — parse leniently and store as `Long`.

```jsonc
// album
{ "id": 123, "name": "Blue", "artist": { "id": 45, "name": "Joni Mitchell" },
  "year": 1971, "songcount": 10, "time": 2156, "art": "https://…/get_art…",
  "genre": [ { "id": 7, "name": "Folk" } ] }
// NOTE: "name" may arrive as "Artist - Album" when the server disambiguates
//       duplicate titles → AlbumNames.display() strips it.

// song
{ "id": 9876, "title": "River", "track": 9, "time": 240, "year": 1971,
  "artist": { "id": 45, "name": "Joni Mitchell" },
  "albumartist": { "id": 45, "name": "Joni Mitchell" },
  "album": { "id": 123, "name": "Blue" },
  "genre": [ { "id": 7, "name": "Folk" } ],
  "url": "https://…/play/index.php?ssid=…", "art": "https://…" }
// We never use "url" — it carries the token. Stream URLs are built by us.

// artist
{ "id": 45, "name": "Joni Mitchell", "albumcount": 19, "songcount": 210 }

// genre
{ "id": 7, "name": "Folk", "albums": 31, "artists": 12, "songs": 410 }

// playlist
{ "id": 3, "name": "Driving", "items": 2148, "owner": "erez", "type": "private" }
```

## Fixtures

`app/src/test/fixtures/` holds verbatim responses from the **real** server (task
03). Every DTO has a test that parses its fixture. When the server is upgraded
and something breaks, the first move is to re-capture the fixture and diff it.
