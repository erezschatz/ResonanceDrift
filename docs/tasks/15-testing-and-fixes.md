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
  cannot say. Fix: log `load canceled <key>: N KB in X ms`.
- **W2 · Stream connection held open across long pauses** — watch. First
  seen 2026-10-26. A song's download runs as long as it plays (buffer is
  ~3 min ahead), so a pause holds the HTTP connection: song-17069 finished
  after 89 min, across lunch, without error. Risk: a server, proxy or
  mobile-network drop during a long pause shows up as a load error or a
  `recovery:` line on resume. Settled by a week with long pauses and no
  such line, or by the first one.
- **W3 · One slow first READY** — test. 2026-10-26 07:32: "United in
  Grief" sent first bytes in 263 ms but was READY 5.2 s later; other starts
  0.4–2 s. Test: start that album again. If it repeats, look at the file on
  the server (format, embedded art); if not, explained as a one-off.
- **W4 · 3-second service runs** — watch. 2026-10-25 19:36 and 2026-10-26
  10:52: restore, a prefetch started, `service destroyed` 3 s later. Fits
  opening the app and closing it while paused (D37). If Erez did not open
  the app at those times, find what starts the service.
- **W5 · "headphones disconnected" logged while already paused** — watch,
  cosmetic. Media3 reports a change of reason with `playWhenReady` already
  false. Harmless noise; fix only if it gets in the way of reading a log.
- **W6 · Transient focus loss right after a user pause** — watch, low.
  2026-10-26 16:11:59: user pause, then `suppressed: transient audio focus
  loss` 0.3 s later (likely an assistant or notification). Note it if it
  recurs with a pattern.

## Log
- 2026-10-25: 1.0.1 (19) built — exception names kept by R8, shuffle starts
  from the stored album pool (D37). The week starts when it is on the phone.
- 2026-10-26: day 1 of the week on 1.0.1 (19), clean. Three diagnostics
  copies (morning drive, lunch, afternoon): no load errors or stalls,
  gapless auto-advance throughout, restore resumed at the saved song and
  position, the stored-pool shuffle queued the next album in under a second.
  Peculiarities moved to the watch list as W1–W6.
