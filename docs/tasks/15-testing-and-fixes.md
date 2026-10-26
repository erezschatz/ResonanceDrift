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

## Log
- 2026-10-25: 1.0.1 (19) built — exception names kept by R8, shuffle starts
  from the stored album pool (D37). The week starts when it is on the phone.
- 2026-10-26: day 1 of the week on 1.0.1 (19), clean. Three diagnostics
  copies (morning drive, lunch, afternoon): no load errors or stalls,
  gapless auto-advance throughout, restore resumed at the saved song and
  position, the stored-pool shuffle queued the next album in under a second.
  Noted, not failures: "load done" times run as long as playback (one held
  89 min across a pause); one slow first READY (5.2 s, "United in Grief");
  restore logs `load start` twice for the restored song; two 3 s service
  runs (app opened and closed while paused).
