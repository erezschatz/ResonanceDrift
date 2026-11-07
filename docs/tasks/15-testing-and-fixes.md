# 15 · Testing and fixes

## Promise
The release build is what I listen to, every day, instead of ReDrift. When
something goes wrong, the log says why, and it gets fixed or knowingly
accepted.

## For Erez
Everything below the Acceptance section is Claude's bookkeeping; this is the
whole of what is asked of you. Claude tells you in chat, at the moment, when
something here needs you. Nothing is due on a schedule.

- **Keep copying diagnostics** as before, with a line on what you heard.
- **Say so only if it happens again:** a glitch or jump you can hear (W9), a
  song that takes several seconds to start (W3). Until then both count as
  explained one-offs; if they were corrupt files they would repeat.
- **W7: done.** The 2026-11-07 test passed (paused 10:28, played 15:59, the
  token renewed before the first stream). Nothing more to do for this step.
- **One question (W6):** when a pause shows up a fraction of a second after a
  play, was something speaking (a voice prompt, the car, the headset)? An
  answer from memory is enough, or "no idea".

## Week review (2026-11-07)
The whole week's logs are in, reviewed in `logs/2026-11-07-week-review.md`:
28 runs, no playback failure, one audible 1.3 s gap on 11-06 caused by a
real bug (the restore streams with a dead token, F1 there). The review's
Plan section holds the proposed 1.0.2 contents, a proposed step 18, and the
questions still owed by Erez. The 2026-11-01 agenda it replaces is answered
there item by item.

## Acceptance
- [x] One week of daily use on the release build with no playback failure
      that ReDrift would also not have had (moved from step 14, D38) —
      2026-10-26 to 2026-11-06 on 1.0.1 (19); `logs/2026-11-07-week-review.md`
- [x] Any failure found goes through `PlaybackLog` and ends in a fix or a
      `DECISIONS.md` entry explaining why it is accepted — the 403 on restore
      is fixed in 1.0.2 (D40) and verified on the phone 2026-11-07 (W7 test:
      5.5 h paused, handshake before the first stream, no 403), the
      connection resets accepted (D40)
- [x] Findings that become work of their own go to their own steps (D39):
      scrobbling doubles → [16](16-scrobble-doubles.md), Now Playing queue
      position → [17](17-now-playing-current-song.md) — both done 2026-11-07
- [x] ReDrift's home-screen icon removed from the phone (moved from step 14) —
      long gone; Erez, 2026-11-07

## How
- Erez listens as usual and copies diagnostics every couple of hours and
  right after anything odd, with a line on when and what was heard; once a
  day the copies come to Claude.
- Each fix is a release build: `versionName` 1.0.x, `versionCode` +1,
  installed with Build Variants → release, Run ▶ (`docs/SETUP.md` §6.4).
- Findings, fixes and accepted failures are written here, dated, as they
  happen.
- Anything peculiar in a log that is not (yet) a failure goes on the watch
  list below, with what would settle it. An item leaves the list as a fix,
  a `DECISIONS.md` entry, or "explained" with the reason.
- Claude raises the list with Erez when one of these happens, not before:
  the week's evidence is in (review due 2026-11-01); a bug is proven; or a
  specific test on the phone is needed to settle an item.

## Watch list
Status: **watch** (gather evidence), **test** (needs a run on the phone),
**address** (real but small; goes in the next 1.0.x build).

- **W1 · Canceled loads are not logged** — fixed in 1.0.2 (20), `load canceled
  <key>: N KB in X ms`; leaves the list when a restore on the phone shows it.
  First seen 2026-10-26.
  `PlaybackEvents` has no `onLoadCanceled`, so an abandoned load leaves a
  `load start` with no end: every restore shows `load start` twice for the
  restored song, and prefetches dropped by Next album just stop. Likely
  Media3 reading the file head for the seek map, then restarting at the
  saved position (only restores with position > 0 show it), but the log
  cannot say. Fix: log `load canceled <key>: N KB in X ms`. 2026-10-27:
  both restores again (positions 258.4 s and 3.4 s). 2026-11-07: all 28
  restores of the week; and on 11-03 17:19:45 a canceled load's DNS failure
  showed as a `stream … failed` with no `load error`, which misleads.
- **W2 · Stream connection held open across long pauses** — explained
  2026-11-07, accepted (D40). Closed. First
  seen 2026-10-26. A song's download runs as long as it plays (buffer is
  ~3 min ahead), so a pause holds the HTTP connection: song-17069 finished
  after 89 min, across lunch, without error. Risk: a server, proxy or
  mobile-network drop during a long pause shows up as a load error or a
  `recovery:` line on resume. Settled by a week with long pauses and no
  such line, or by the first one. 2026-10-27: pauses of 5, 10 and 1–2 min
  mid-download, all resumed without a load error. 2026-11-07: the drop
  happened three times (10-28 10:02, 11-05 08:54 and 09:17), after pauses
  of 8–50 min, as `Connection reset`; the loader's retry reconnected in
  under a second with 95–136 s still buffered. Nothing heard.
- **W3 · One slow first READY** — explained as a one-off (closed
  2026-11-07, no recurrence in ten more days). 2026-10-26 07:32: "United in
  Grief" sent first bytes in 263 ms but was READY 5.2 s later; other starts
  0.4–2 s. Test: start that album again. If it repeats, look at the file on
  the server (format, embedded art); if not, explained as a one-off.
- **W4 · 3-second service runs** — explained (closed 2026-11-07). 2026-10-25
  19:36 and 2026-10-26 10:52: restore, a prefetch started, `service
  destroyed` 3 s later. Fits opening the app and closing it while paused
  (D37). Recurred all week exactly when a diagnostics copy was taken right
  after opening the app (11-02 17:46, 11-03 17:17, 11-04 13:13, 11-05 10:25).
- **W5 · "headphones disconnected" logged while already paused** —
  explained 2026-10-27, closed 2026-11-07 (39 such lines in the week, same
  pattern). Media3 reports a change of reason with
  `playWhenReady` already false. The disconnect comes 35 s after a user
  pause every time (five times on 10-27, once on 10-26): the headset
  switching itself off when paused. Harmless noise; fix only if it gets in
  the way of reading a log.
- **W6 · Focus changes next to a "user" pause** — explained 2026-11-07:
  media-button PAUSE from the Bluetooth device being left behind (11-05
  09:25, three in a row while moving between car and earphones, then the
  headset disconnect). Closed. 2026-10-26
  16:11:59: user pause, then `suppressed: transient audio focus loss` 0.3 s
  later. 2026-10-27, twice the other way round: focus comes back, playing
  resumes, and a `(user)` pause follows 0.3 s (09:01:45) and 0.04 s
  (14:53:03) later, then play again. 40 ms is not a hand. The app pauses
  only from its Play/pause button, so the pause came from outside, most
  likely a media button sent by the headset or car when a voice prompt
  ends. Ask Erez what was speaking at those times; settled by naming the
  source.
- **W7 · 403 on the restored queue's next song** — **bug, fixed in 1.0.2 (20),
  verified 2026-11-07** (D40; `Session.freshLogin()` before the restore,
  handshakes logged). Test on the phone: pause with the current song not
  fully downloaded, leave it 3+ hours, open the app, press play within a
  second, let it run past the buffered point; expected a
  `session: handshake (stored token expired)` line before the first `stream`
  line, no 403, no `recovery:`. Result
  (`logs/resonancedrift-diagnostics-2026-11-07.txt`): paused 10:28:06 at
  11 s of "Lost Dreams" with 6.1 MB downloaded (the download was cut when
  the service died at 10:39), app swiped from recents; launched 15:59:00,
  5.5 h later: `session: handshake (stored token expired)` 20 ms after start
  and 1.4 s before the restore, the stream request with the new token
  answered in 161 ms, play pressed 1.1 s after the restore, buffered 73 s
  from the cache then 210 s from the network by the copy. No 403, no
  `recovery:`. Closed. Cause (review
  F1, 2026-11-07): the restore builds stream URLs from the stored
  token before the launch handshake has replaced it; 5 of 28 restores lost
  the race, and on 11-06 12:01 the restored song outran its cache, the
  player failed and `NetworkRecovery` brought it back in 1.3 s — the only
  audible gap of the week, and proof that the recovery path works. First seen
  2026-10-27 08:47:17. On the first restore after 15.5 h idle, the prefetch
  of the next song got `Response code: 403` with 0 KB: the stored token had
  expired. The restored song played from the cache, so nothing was heard.
  Erez started a new album 8 s later, and its stream worked, so the token
  was renewed somewhere in between (probably `MainActivity.onStart` →
  `Session.pingIfStale`). The log does not show that, because `Session`
  does not write to `PlaybackLog`. A 4xx fails the loader at once
  (`loadRetryDelayMs`), so the error would surface when playback reaches
  that song; then `NetworkRecovery` should re-handshake and retry
  (`recovery: stream refused…`). That path has never run in the field.
  Test: leave a song paused near its end overnight, and next morning press
  play and let it run into the next song. Expected: the `recovery:` lines
  and a short gap, not a stop. Whatever the result, consider logging
  session renewals to `PlaybackLog`.
- **W8 · Audio underruns with a full buffer** — watch, low (no recurrence
  since 11-01; four in all). 2026-10-27 08:54:42
  and 08:55:06: `audio underrun … track buffer 500 ms, last fed 1006 ms /
  502 ms ago` with 3 min buffered. Both came in the first seconds after
  resuming, the first one 5 min after a headset disconnect. Erez then
  paused and restarted the song, so it was probably heard. Not the network:
  the player went 0.5–1 s without feeding the audio output. Settled if it
  recurs only right after a headset reconnects (the audio route, which is
  outside the player) or shows up mid-song (look at the renderer).
- **W9 · Small glitch heard around "Footballer's Wife"** — explained as a
  one-off (closed 2026-11-07; the one glitch heard since, 11-06, is W7's).
  2026-10-27 ~09:22, a "missed or jumped a bit". The log is clean from
  09:20 to 09:25: a gapless auto transition, no load error, no underrun, no
  state change. Test: play the song again, from its cache, on the phone
  speaker. If the glitch repeats at the same spot, it is in the file;
  otherwise it is the Bluetooth link and outside the player (explained).
- **W10 · One slow connection** — explained 2026-11-07: Wi-Fi ↔ mobile
  handovers (the 10-28 06:49 outage shows the whole sequence; connects of
  4 s and 10 s inside it, none heard). Closed. 2026-10-27 14:11:05: the
  next song's stream took 6.8 s to connect, against 0.15–0.5 s
  everywhere else. It was a prefetch, so nothing was heard. Possibly a
  Wi-Fi to mobile handover, which the log does not show. Note it if it
  recurs; the risk is the same delay on a song that has to start now.

- **W11 · Library fetch failure not logged** — fixed in 1.0.2 (20): `library:
  <what> failed (…)` on every refresh and API-only fetch, `session: handshake
  (why)` on every handshake (D40). Leaves the list on the first field log that
  shows one. First seen 2026-11-01. Albums did not load after a network problem and came back on
  a manual refresh; `PlaybackLog` has only `network: lost/available`. Same
  gap as W7's session renewals: the log has no line for non-playback API
  calls that fail. Fix with W7: log a failed library fetch and each
  handshake. Erez, 2026-11-01: about a minute before the first song played, so
  around the 09:58 restore, where the log shows only `network: available`.

- **W12 · HTTP 429 after a doubled "new queue"** — closed 2026-11-07, cause
  unknown, nothing heard. 2026-11-02 17:17:31: two identical stream requests
  73 ms apart (two `(new queue)` transitions), the second answered 429; the
  loader's retry played a second later. Most likely two taps on Play. Erez
  cannot say: the daily review did not flag it at the time, so there was
  nothing to comment on, and five days later there is nothing to recall.
  The retry policy already covers 429. Reopen only if it recurs.
- **W13 · Bursts of transient audio-focus loss** — closed 2026-11-07, source
  unknown, outside the app. 2026-11-03 07:32–07:46 in the car: eleven
  `suppressed: transient audio focus loss` pauses of 6–34 s, each resumed at
  the same position; smaller bursts 10-28 07:40, 11-05 09:45, 11-06 09:25.
  The player did as asked (pause on a full transient loss, resume at the same
  spot); not flagged by the daily review at the time either, so Erez has no
  idea what was speaking, and finds the reading plausible. Nothing to change.

Review of 2026-11-01 (`logs/2026-11-01-review.md`): W1, W5, W6, W8 recur;
W2 gained a 27 min pause without error; W7 not exercised, but a restore
after ~40 h streamed at once (the server log shows a handshake on every
launch); W10 did not recur; W3, W4, W9 have no new data. W6 pattern: a
pause 0.2–0.4 s after a play. W8 now also follows a plain 2 s pause, so it
is not only a headset reconnect.

## Log
- 2026-10-25: 1.0.1 (19) built — exception names kept by R8, shuffle starts
  from the stored album pool (D37). The week starts when it is on the phone.
- 2026-10-26: day 1 of the week on 1.0.1 (19), clean. Three diagnostics
  copies (morning drive, lunch, afternoon): no load errors or stalls,
  gapless auto-advance throughout, restore resumed at the saved song and
  position, the stored-pool shuffle queued the next album in under a second.
  Peculiarities moved to the watch list as W1–W6.
- 2026-10-27: day 2 on 1.0.1 (19), no playback failure. Two diagnostics
  copies (morning drive, afternoon). Two restores resumed at the saved
  song and position. Album choice plus "continue with shuffle" queued the
  next album when the last-but-one song started, and survived three quick
  skips across that boundary. Shuffle from a picked album worked the same
  way. Gapless throughout. New on the watch list: W7 (expired token on
  the restored queue's prefetch, recovery not exercised), W8 (underruns
  with a full buffer), W9 (the glitch heard near "Footballer's Wife", not
  in the log) and W10 (one 6.8 s connect). W5 explained; W6 has a pattern.
- 2026-10-30 to 2026-11-01: five more runs reviewed
  (`logs/2026-11-01-review.md`), no playback failure; no copies for 10-28,
  10-29, 10-31. Compared with the Ampache server log of 10-30, the app's
  scrobbling is doubled by the server counting the prefetch stream: step 16.
  Now Playing's queue does not open at the current song: step 17. W11 added.
- 2026-11-07: the complete week reviewed (`logs/2026-11-07-week-review.md`;
  the 13 diagnostics copies not yet in the repo added, 10-26 to 11-06).
  Acceptance criterion 1 met: 28 runs, no playback failure. W7 became a bug
  with a cause and a fix (1.0.2); W2 and W10 explained by the week's three
  connection resets and one two-minute outage, all absorbed by the loader's
  retries; W3, W4, W5, W6, W9 closed; W12, W13 added. Proposed: 1.0.2 with
  the W7 fix, the logging gaps (W1, W7, W11), step 16 and step 17; a new
  step 18 for Erez's 11-03 request (lists mark the playing song). Nothing
  built; decisions in the review's Plan section.
- 2026-11-07: **1.0.2 (20) built**, four changes (D40, D41): the restore waits
  for a live token (W7); canceled loads, handshakes and failed library fetches
  are logged (W1, W7, W11); `cache=1` on the stream URL (step 16); the queue
  panel scrolls to the current song in an effect (step 17). JVM tests added
  for `Session.freshLogin`, `isStale`, the URL and the scroll target.
  Installed the same day with `./gradlew installRelease` (Studio's variant
  had drifted back to debug). Same day, Erez: the step 16 album test and the
  step 17 look both pass; steps 16 and 17 done. The ReDrift icon has been
  gone since the app started playing music. Pending: the W7 overnight test,
  then this step closes.
- 2026-11-07: **the W7 test passed** (15:59, 5.5 h after a pause at 11 s of
  a song still downloading; details under W7). All acceptance criteria
  ticked; **step 15 done, phase 1 complete.** Still owed by Erez, from the
  week review: W13 (what was speaking 11-03 07:32), F4 (double tap), the
  step 18 decision (highlight only, or ReDrift's collapsed view, D31), task
  16's open questions.
- 2026-11-07, later: Erez answered. W13: no idea what was speaking, the
  reading sounds right — closed. F4/W12: no idea, closed unless it recurs.
  Neither was flagged by the daily review when the logs came in, which is
  when he could have said; a lesson for the next watch item, not a fix.
  Step 18: a highlight, centred on the screen, nothing more — opened as
  [18](18-lists-mark-playing-song.md), D42. The watch list is now W8 only.
