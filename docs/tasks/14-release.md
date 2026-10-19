# 14 · Release build and phase-1 sign-off

## Promise
A small, fast, signed build on my phone that I live with, and a repo that is
finished: everything documented, nothing half-done.

## Acceptance
- [ ] Release key generated once, stored outside the repo on the Mac (and on the Windows PC, if it is in use)
      (`docs/SETUP.md` §6); `assembleRelease` builds with R8 and no crashes
      attributable to shrinking (run through every screen once)
- [ ] `versionName` 1.0, `versionCode` as it stands
- [ ] One week of daily use with no playback failure that ReDrift would also
      not have had; any failure found goes through `PlaybackLog` and either a
      fix or a `DECISIONS.md` entry explaining why it is accepted
- [ ] `docs/SPEC.md` matches the code; `docs/PLAN.md` all done; `CHANGELOG.md`
      has a `1.0` section
- [ ] ReDrift's home-screen icon removed from the phone
