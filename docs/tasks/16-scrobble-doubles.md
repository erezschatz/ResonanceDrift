# 16 · Scrobbling: one play per song

## Promise
Each song I listen to appears once in Ampache's recent plays and counts once
in its play count; a song I never heard does not appear at all.

## Acceptance
- [x] Cause seen in the server log (2026-11-01); a never-heard song confirmed
      in the history (Lush)
- [ ] Cause confirmed by a test: one song played from the app, history
      read before and after (see Test below)
- [ ] The stream URL no longer registers a play; `record_play` (D36) is the
      only source of plays
- [ ] A prefetched song that was never reached (the queue was paused or
      replaced first) leaves no entry
- [ ] Decision entry in `DECISIONS.md`; `AMPACHE.md` and `SPEC.md` say who
      records plays
- [ ] On the phone, one album listened through: each song once in recent
      plays, with a time near when it was heard, not ~3 min earlier

## Observed (2026-10-30, Erez)
"Recently Played" on equinemusic.com, screenshot 13:03: four of the five
Skepta tracks from the 12:20–12:43 run appear twice (Too Many Man, Sticks and
Stones, Looking Out, Oh My Gosh); Are You Ready? appears once. The app log
starts each track once and does not record scrobbles, so it could not say
where the second came from. The server log answers it.

## Evidence
Server log: `logs/ampache.20261030.log` (UTC; the phone is UTC+2, so 12:29
local is 10:29 there). App log: `logs/resonancedrift-diagnostics-2026-10-30.txt`.

Every song has **two** play registrations, ~3 min apart:

1. **At stream start.** Ampache's `PlayAction` registers a play
   (`Registering stream … {id}` then `post_activity: play song`) on any
   `play/index.php` request whose Range starts at byte 0. The app opens that
   request when it **prefetches** the next song, ~2.5–3 min before it is heard.
2. **At `record_play`**, sent when the song starts playing.

Skepta, 2026-10-30 (UTC):

| Song | Stream registered | `record_play` | Gap |
|---|---|---|---|
| Are You Ready? {60950} | 10:25:54 | 10:29:01 | 3:07 |
| Oh My Gosh {60940} | 10:28:19 | 10:31:37 | 3:18 |
| Looking Out {60954} | 10:30:55 | 10:34:10 | 3:15 |
| Sticks and Stones {60938} | 10:36:05 | 10:38:27 | 2:22 |
| Too Many Man {60947} | 10:38:37 | 10:41:46 | 3:09 |

The same pairing holds for all 28 `record_play` calls in the file. Where the
two land within 20 s, the server drops one itself (`Stats`: "Object already
inserted" 15:06:18; "Last song played within skip limit" 15:06:19, 15:14:14,
15:14:15), which is why the first song after choosing an album is not doubled.
Restores and resumed songs are not doubled: their stream starts mid-file and
the server logs "Content-Range doesn't start from 0 … not collecting stats"
(10:20:49, 16:13:09).

Three songs were streamed and **never `record_play`'d**, because the queue
ended or was replaced before they were reached: Lush {60951} 10:42:23, Real
Love {9666} 15:11:51, Cosmic Hero {6573} 15:56:48. Each probably has a play
entry for a song that was not heard. **Confirmed for Lush (2026-11-01,
Erez):** Ampache's recent plays list it directly next to Little Arrow, the
first song played after it (17:06 local). Nothing else was played between,
and the log shows Lush never started (paused during Too Many Man, 12:43:46).

Why Are You Ready? shows once: not explained by the log. Likeliest the
screenshot cut off the oldest entry; unverified.

## Fix (proposed)
Ampache's `PlayAction` (develop branch, read 2026-11-01) skips stats when the
request has `cache=1`; no other parameter does. Append `&cache=1` in
`AmpacheUrls.stream` (`ampache/AmpacheUrls.kt:50`). The cache key is a custom
`song-<id>` (`playback/MediaItems.kt:105`), so existing cached songs stay valid.
`record_play` then stays the single source (D36).

**Check before building:** that Erez's server version honours `cache=1` (the
parameter is read from the develop branch, not from the server he runs), and
that it does not hide the song from "now playing".

**Rejected (proposed).** Dropping `record_play` and letting the stream count:
it registers at prefetch time, counts songs never reached, and misses songs
played from the cache.

## Test
On the server (or equinemusic.com), note the recent plays. Play one album
with the release build, pausing near the end of song 2 so song 3 is
prefetched but not reached. Expected after the fix: songs 1–2 once each,
song 3 absent. Before the fix this reproduces both defects; run it once on
1.0.1 first if the cause is still in doubt.

## Open
- The Ampache UI shows no dates, so timestamps ("~3 min earlier") can only
  be checked on the server side or through the API's `stats`/history calls.
- Does the doubling also reach the play count (`object_count`) and the
  scrobble plugins, or only the recent-plays/activity list? The log shows
  `post_activity` twice per song, which feeds the list; the count is
  unseen. Look at one song's play count before and after.
- Earlier plays (since 1.16 on 2026-10-25) are doubled in the history; not
  worth cleaning unless Erez wants it.
