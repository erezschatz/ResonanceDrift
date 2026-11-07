# Week review, 2026-10-26 to 2026-11-06 (1.0.1 build 19)

Compiled 2026-11-07 from the complete set of diagnostics copies (sixteen
files, all in this folder) and Erez's note on each. Supersedes the partial
review of 2026-11-01 for anything the two disagree on. Phone time is UTC+2.

## Sources
| File | Covers | Erez's note | Reviewed before |
|---|---|---|---|
| `…-2026-10-26.txt` | 10-25 19:36, 10-26 06:52–16:58 | picked an album, skipped several shuffle picks | day 1 (chat, 2026-10-26) |
| `…-2026-10-27.txt`, `…-10-27-batch2.txt` | 10-27 08:47–~16:00 | glitch felt near "Footballer's Wife" | day 2 (chat, 2026-10-27) |
| `…-2026-10-28.txt`, `…-10-28-batch2.txt` | 10-28 06:46–~15:30 | car autoplays on connect; headset multipoint trouble with the laptop | **no** |
| `…-2026-10-29.txt` | 10-29 06:50–09:03 | "absolutely nothing to report … zero gaps" | **no** |
| `…-2026-10-30.txt`, `…-10-30-batch2.txt`, `…-2026-11-01.txt` | 10-30, 11-01 | see `2026-11-01-review.md` | yes |
| `…-2026-11-02.txt`, `…-11-02-batch2.txt` | 11-02 13:12–17:46 | songs started that he did not mean to start, while removing from a playlist | **no** |
| `…-2026-11-03.txt`, `…-11-03-batch2.txt` | 11-03 07:02–08:25, 17:17–18:17 | 20 min of hopping, then a playlist; the playlist view does not show the current song | **no** |
| `…-2026-11-04.txt` | 11-04 07:14–07:40, 13:13 | the car's Bluetooth misbehaved; gave up | **no** |
| `…-2026-11-05.txt` | 11-05 08:32–10:07, 10:25 | car ↔ earphones many times; "seamless and very enjoyable" | **no** |
| `…-2026-11-06.txt` | 11-06 08:43–09:42, 11:59–12:20 | a hiccup "during Brave and Strong": a short pause, then a slow start | **no** |

No run exists on 10-31 (the 11-01 restore follows the 10-30 17:57 run
directly), so nothing is missing from the week. The 10-26 and 10-27 files
were reviewed from chat at the time but not kept; they are now in the repo.

The two notes that came with the logs (`resonancedrift-scrobbling-notes.md`,
`resonancedrift-ui-issues.md`) are not copied: the scrobbling note is wholly
superseded by task 16 (its open questions are answered there by the server
log); the UI note's first item is task 17, and its second item (2026-11-03,
the playlist view) is new and is proposed below as step 18.

## The week in numbers
Events deduplicated across overlapping copies.

| | |
|---|---|
| Runs (each starts with a restore) | 28 |
| Restores at the saved song and position | 28 of 28 |
| Gapless auto-advances | 265 |
| New queues (album, playlist, search, shuffle pick) | 66 |
| Loader errors (all retried by the loader, nothing heard) | 13 |
| Player errors (recovery path) | 1, back in 1.3 s |
| Stops that needed the user | 0 |

**Verdict.** The week of daily use is done with no playback failure. One
audible defect was found, and it is an app bug with a clear fix (F1). The
rest of the week's noise is the network and the Bluetooth devices, and the
retry layers absorbed all of it unheard.

## Findings

### F1 · Restore streams with a dead token; the recovery path has now run in the field (W7 → bug)
Five of the 28 restores got `Response code: 403` on the first stream request
of the run, every time within 0.9 s of `run started`:

| Restore | Idle since the last stream request | Which load | Heard? |
|---|---|---|---|
| 10-27 08:47:17 | ~15 h | prefetch of the next song | no |
| 10-28 12:27:34 | ~2 h (paused since 09:12) | the restored song | no (a new album 11 s later) |
| 10-29 06:50:44 | ~18 h | the next song | no |
| 11-02 13:12:05 | ~26 h | the next song | no |
| 11-06 11:59:11 | ~2.5 h (paused since 09:28) | the restored song | **yes**, 12:01:41 |

Mechanism: `PlaybackService.restoreQueue` awaits `Session.awaitLogin()`, which
returns the *stored* login without checking it, and `SongUriResolver` builds
the URL with that token. The launch handshake happens elsewhere
(`MainActivity.onStart` → `pingIfStale` → the dead ping triggers a
re-handshake) and takes 0.5–1 s. Whether the restore's first request goes
out before or after it is a race: the restores after a day's idle that did
*not* 403 (10-30 08:56, 11-01 09:58, 11-05 08:32, 11-06 08:43) all had the
restored song fully cached, so their first request left 1–2 s later than the
losing cases. Ampache's session seems to outlive ~1 h of silence but not
~2 h; the exact length does not matter for the fix.

Four of the five were unheard because the restored song played from the
cache and Erez picked something new before the queue reached the failed
song. On 11-06 the restored song (Africa Talks to You, cached to 468 s) ran
out of cached bytes at 12:01:41, the player failed with the 403,
`NetworkRecovery` renewed the token and `prepare()`d, and audio was back at
12:01:43.2: **1.3 s of silence, and the recovery path works exactly as
designed** (W7's "never run in the field" is closed). The hiccup Erez placed
"during Brave and Strong" is this one: it is the only gap in the run, 58 s
before Brave and Strong started (gapless, per the log).

**Fix (1.0.2).** Before the restore's items are set, make the service wait
for a token that is not past its stored expiry: a `Session.freshLogin()`
that re-handshakes when `expiresAtMillis` is in the past (or within a
minute), awaited in `restoreQueue` in place of `awaitLogin()`. The stored
expiry is a lower bound (streams extend the session server-side without the
app knowing), so this costs at most one handshake per launch after an idle
hour, which the activity would do anyway. The 403 recovery stays as the
backstop. Log the handshake to `PlaybackLog` at the same time (W7/W11).

**Test on the phone.** Pause with the current song not fully downloaded
(the `player:` line shows "buffered to" short of the song's length), leave
it 3+ hours, open the app and press play within a second, let it run past
the buffered point. Expected: a `session: handshake` line before the first
`stream` line, no 403, no `recovery:` line.

### F2 · W2 settled: the server drops the stream connection during long pauses, the loader retries unheard
Three times the download of the current song, held open across a pause,
came back as `SocketException: Connection reset`: 10-28 10:02:09 (after a
~50 min pause), 11-05 08:54:37 (after a 9 min pause) and 11-05 09:17:03
(after ~8 min of pauses). Each time `load start (retry 1)` followed in 1 s
and connected in 0.6–0.7 s, with 95–136 s still buffered. Nothing heard.
That is W2's open risk realised and handled; the behaviour is accepted as
is (a `DECISIONS.md` entry, below). Shorter pauses (≤ 5 min) never reset.

### F3 · 10-28 06:49: a two-minute network outage, absorbed
Leaving home: the home Wi-Fi (192.168.0.59) stopped routing, then DNS
failed, then the network flapped (`network: lost/available` twice). The
loader retried four times over 50 s, connected in 4 s, lost the socket
again ("Software caused connection abort"), retried once more and connected
in 10 s. Buffer never went below 94 s; nothing heard. This is what step 05
was built for, and the first time the field has shown it end to end.
It also explains **W10**: the week's slow connects (7.1 s on 10-27 14:11,
4 s and 10 s here, 2 s on 11-03 07:39, 1.7 s on 11-03 17:41) are Wi-Fi ↔
mobile handovers; the app cannot see them, and none of them was heard.

### F4 · 11-02 17:17:31: one HTTP 429, after a doubled "new queue"
Two `→ item 1 … (new queue)` lines 73 ms apart, so two identical stream
requests; the first was abandoned (`failed after 68 ms (IOException)`), the
second got `429` from the server, and the loader's retry a second later
played. Every other list Play in the week sets the queue once, so this is
most likely two taps on Play (Erez's own suspicion for that session is
accidental taps). The retry policy already treats 429 as retriable. New
watch item **W12**; no change unless it recurs without a double tap.

The same session's "songs that started on their own" (17:24:09 and
17:24:58) are both `(new queue)` + `playWhenReady true (user)`: a tap on a
song row, which plays the list from that song. While removing songs with
the row's ⋮ menu, a tap that lands on the row instead starts it. Not a bug;
noted for step 18 (a visible current-song mark makes a stray start obvious).

### F5 · Bursts of transient audio-focus loss (new, W13) and W6 explained
11-03 07:32–07:46, in the car: eleven `suppressed: transient audio focus
loss` events in fourteen minutes, each pausing 6–34 s and resuming by
itself at the same position. Smaller bursts: 10-28 07:40 (1), 11-05
09:45–09:46 (2), 11-06 09:25 (1), 10-26 16:11 and 16:20. The player did
what Android asked (pause, resume at the same spot); the question is who
took focus. A navigation prompt normally asks to *duck*, which would lower
the volume without pausing; a full transient loss is a call, an assistant,
or an app set to "play voice as a phone call" over Bluetooth. **Question
for Erez:** what was speaking in the car at 07:32 on 11-03?

W6 (a `(user)` pause 0.04–0.4 s after a play): 11-05 09:25:02–09:25:15
shows it three times in a row, exactly while Erez was moving between the
car and the earphones, followed by `headphones disconnected`. The app only
pauses from its own button, so these are media-button PAUSE commands from
the Bluetooth device being left behind. Explained; outside the app.

### F6 · Quiet items
- **W1** (canceled loads not logged): all 28 restores show the doubled
  `load start`. Worse, on 11-03 17:19:45 a canceled restore load's DNS
  failure was logged as `stream … failed (UnknownHostException)` with no
  `load error`, which reads like a silent failure until one works out that
  the user had replaced the queue 16 s earlier. Address in 1.0.2.
- **W3** (one slow first READY), **W9** (the Footballer's Wife glitch): no
  recurrence in ten more days. Closed as one-offs.
- **W4** (3-second service runs): recurs (11-02 17:46, 11-03 17:17, 11-04
  13:13, 11-05 10:25), always when a diagnostics copy was taken right after
  opening the app, or the app was opened and closed while paused. Explained
  (D37). Closed.
- **W5** (headset off ~30 s after a pause): 39 such lines in the week, the
  pattern holds. Explained; closed.
- **W8** (underruns with a full buffer): the four known ones (10-27, 11-01),
  none since. Stays on watch, low.
- **W11** (library fetch failure not logged): no new occurrence; fix with
  F1's logging.
- Shuffle: the stored pool answered in under a second on every "next album"
  and refreshed in the background in 3–13 s; 2758 → 2765 albums over the
  week. Album chaining worked at every boundary, including across restores.

### F7 · Scrobbling (step 16): the server-side check is as far as the log allows
The server's AutoUpdate compares itself against `commits/develop`, so Erez's
Ampache tracks the develop branch, where `PlayAction` reads `cache` and sets
`record_stats = false` for `cache=1` (read 2026-11-07; one place, integer
compare, nothing else keyed on it). Two things to know before building:
`record_stats` also gates `Stream::insert_now_playing`, so with `cache=1`
the app's streams **disappear from Ampache's "Now playing" box**; and
`record_play` is unaffected (it is the API, not `PlayAction`). The remaining
check is the test in task 16 itself.

## Plan
Decisions for Erez, in the order they would be built. Nothing here is built.

### 1.0.2 (20) · "the week's fixes"
Four small changes, each its own commit; one install.

1. **F1 — restore waits for a live token.** `Session.freshLogin()`;
   `restoreQueue` awaits it. Unit test on `Session` with a fake clock and
   store (past expiry → handshake; future → none). Phone test above.
2. **Logging gaps — W1, W7, W11.** `PlaybackEvents.onLoadCanceled` →
   `load canceled <key>: N KB in X ms`; `Session` logs each handshake
   (`session: handshake (why)`) and `LibraryRepository` logs a failed
   refresh (`library: <what> failed: <error>`). Pure logging; no behaviour.
3. **Step 16 — `cache=1` on the stream URL.** As proposed in the task, plus
   the "Now playing" trade-off above written into the decision. Test from
   task 16 (one album, pause near the end of song 2; song 3 must not appear).
4. **Step 17 — queue opens at the current song.** Replace the
   `initialFirstVisibleItemIndex` with a `LaunchedEffect(currentIndex)` that
   calls `listState.scrollToItem((currentIndex - 2).coerceAtLeast(0))`: it
   works whether or not the sheet is measured before it is shown, and it
   also follows auto-advance while the panel is open (the task's third
   criterion, answered "yes"). Phone test on the 190-song playlist restore
   (item 10) and on a short queue. If it still opens at the top, the value
   of `currentIndex` at that moment is wrong, and the next look is
   `PlayerConnection.publish`.

Order of 3 and 4 is free; 1 and 2 first, because they touch the service.
**Does the week carry over?** Yes: 1.0.1's week stands as step 15's
evidence. 1.0.2 changes one line of the service's start-up order and adds
log lines; its proof is the targeted tests above plus ordinary use, not
another week.

### Step 18 (proposed) · Lists mark the song that is playing
From Erez's 2026-11-03 note. `PlaylistDetailScreen` and `AlbumDetailScreen`
highlight the row whose song id is the player's current `mediaId`
(`PlayerConnection` already exposes the queue and index), the same way
`QueuePanel` does; nothing scrolls. The note's second half, "the list itself
visible from the playback screen", is what the queue panel on Now Playing
is, so it is covered by step 17 — unless Erez means ReDrift's collapsed
"Now playing / Next up" playlist view, which D31 rejected. **Say which.**
Re-opening D31 would be a new decision entry, not a quiet change.

### Closed, accepted, still watched
- Closed as explained: W3, W4, W5, W6, W9, W10.
- Accepted with a `DECISIONS.md` entry (with 1.0.2): W2, the connection
  reset after long pauses, handled by the loader's retry.
- Watch, low: W8 (underruns), W12 (429 after a double tap), W13 (focus
  bursts; pending Erez's answer).
- Becomes work: W1, W7, W11 → 1.0.2 items 1 and 2.

### Step 15 closes when
1.0.2 is on the phone with its tests done, and the ReDrift icon is off the
home screen (the one unticked criterion that is not code). Then phase 1 is
15, 16, 17 done and 18 wherever Erez puts it.

### Owed by Erez
- W13: what was speaking in the car, 11-03 07:32–07:46?
- F4: two taps on Play at 11-02 17:17?
- Step 18: highlight only, or ReDrift's collapsed playlist view (D31)?
- Task 16 open questions: do Real Love and Cosmic Hero still show in the
  history; does the doubling reach the play count or only the list; is
  losing the "Now playing" box acceptable with `cache=1`?
- Is the ReDrift icon gone from the home screen?
