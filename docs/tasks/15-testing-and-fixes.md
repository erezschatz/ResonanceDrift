# 15 · Testing and fixes

## Promise
The release build is what I listen to, every day, instead of ReDrift. When
something goes wrong, the log says why, and it gets fixed or knowingly
accepted.

## Acceptance
- [ ] One week of daily use on the release build with no playback failure
      that ReDrift would also not have had (moved from step 14, D38)
- [ ] Any failure found goes through `PlaybackLog` and ends in a fix or a
      `DECISIONS.md` entry explaining why it is accepted
- [ ] ReDrift's home-screen icon removed from the phone (moved from step 14)

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

- **W1 · Canceled loads are not logged** — address. First seen 2026-10-26.
  `PlaybackEvents` has no `onLoadCanceled`, so an abandoned load leaves a
  `load start` with no end: every restore shows `load start` twice for the
  restored song, and prefetches dropped by Next album just stop. Likely
  Media3 reading the file head for the seek map, then restarting at the
  saved position (only restores with position > 0 show it), but the log
  cannot say. Fix: log `load canceled <key>: N KB in X ms`. 2026-10-27:
  both restores again (positions 258.4 s and 3.4 s).
- **W2 · Stream connection held open across long pauses** — watch. First
  seen 2026-10-26. A song's download runs as long as it plays (buffer is
  ~3 min ahead), so a pause holds the HTTP connection: song-17069 finished
  after 89 min, across lunch, without error. Risk: a server, proxy or
  mobile-network drop during a long pause shows up as a load error or a
  `recovery:` line on resume. Settled by a week with long pauses and no
  such line, or by the first one. 2026-10-27: pauses of 5, 10 and 1–2 min
  mid-download, all resumed without a load error.
- **W3 · One slow first READY** — test. 2026-10-26 07:32: "United in
  Grief" sent first bytes in 263 ms but was READY 5.2 s later; other starts
  0.4–2 s. Test: start that album again. If it repeats, look at the file on
  the server (format, embedded art); if not, explained as a one-off.
- **W4 · 3-second service runs** — watch. 2026-10-25 19:36 and 2026-10-26
  10:52: restore, a prefetch started, `service destroyed` 3 s later. Fits
  opening the app and closing it while paused (D37). If Erez did not open
  the app at those times, find what starts the service.
- **W5 · "headphones disconnected" logged while already paused** —
  explained 2026-10-27. Media3 reports a change of reason with
  `playWhenReady` already false. The disconnect comes 35 s after a user
  pause every time (five times on 10-27, once on 10-26): the headset
  switching itself off when paused. Harmless noise; fix only if it gets in
  the way of reading a log.
- **W6 · Focus changes next to a "user" pause** — watch, low. 2026-10-26
  16:11:59: user pause, then `suppressed: transient audio focus loss` 0.3 s
  later. 2026-10-27, twice the other way round: focus comes back, playing
  resumes, and a `(user)` pause follows 0.3 s (09:01:45) and 0.04 s
  (14:53:03) later, then play again. 40 ms is not a hand. The app pauses
  only from its Play/pause button, so the pause came from outside, most
  likely a media button sent by the headset or car when a voice prompt
  ends. Ask Erez what was speaking at those times; settled by naming the
  source.
- **W7 · 403 on the restored queue's next song** — test. First seen
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
- **W8 · Audio underruns with a full buffer** — watch. 2026-10-27 08:54:42
  and 08:55:06: `audio underrun … track buffer 500 ms, last fed 1006 ms /
  502 ms ago` with 3 min buffered. Both came in the first seconds after
  resuming, the first one 5 min after a headset disconnect. Erez then
  paused and restarted the song, so it was probably heard. Not the network:
  the player went 0.5–1 s without feeding the audio output. Settled if it
  recurs only right after a headset reconnects (the audio route, which is
  outside the player) or shows up mid-song (look at the renderer).
- **W9 · Small glitch heard around "Footballer's Wife"** — test.
  2026-10-27 ~09:22, a "missed or jumped a bit". The log is clean from
  09:20 to 09:25: a gapless auto transition, no load error, no underrun, no
  state change. Test: play the song again, from its cache, on the phone
  speaker. If the glitch repeats at the same spot, it is in the file;
  otherwise it is the Bluetooth link and outside the player (explained).
- **W10 · One slow connection** — watch, low. 2026-10-27 14:11:05: the
  next song's stream took 6.8 s to connect, against 0.15–0.5 s
  everywhere else. It was a prefetch, so nothing was heard. Possibly a
  Wi-Fi to mobile handover, which the log does not show. Note it if it
  recurs; the risk is the same delay on a song that has to start now.

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
