# 14 · Release build and phase-1 sign-off

## Promise
A small, fast, signed build on my phone that I live with, and a repo that is
finished: everything documented, nothing half-done.

## Acceptance
- [ ] Release key generated once, stored outside the repo on the Mac (and on the Windows PC, if it is in use)
      (`docs/SETUP.md` §6); `assembleRelease` builds with R8 and no crashes
      attributable to shrinking (run through every screen once)
- [x] `versionName` 1.0, `versionCode` as it stands
- [ ] One week of daily use with no playback failure that ReDrift would also
      not have had; any failure found goes through `PlaybackLog` and either a
      fix or a `DECISIONS.md` entry explaining why it is accepted
- [ ] `docs/SPEC.md` matches the code; `docs/PLAN.md` all done; `CHANGELOG.md`
      has a `1.0` section
- [ ] ReDrift's home-screen icon removed from the phone

## Started 2026-10-25 · 1.0 (18)
- `versionName` 1.0; `versionCode` 18 (one on from 1.16's 17, as for every
  installable build).
- `assembleRelease` with R8 and resource shrinking builds with no warnings
  and no rules added to `proguard-rules.pro`: 5.1 MB against the debug
  build's 75 MB. Signed with the debug key for now (no `keystore.properties`
  yet).
- Smoke test on the API 37 emulator, that build: installs, launches, the
  auth screen draws, the playback service binds; no crash in logcat. Past
  login it is not checked: no test server, and the real login is Erez's.
- `docs/SETUP.md` §6.1–6.4: choosing the password, generating the key with
  `keytool`, `keystore.properties`, and installing the release build (first
  time: uninstall the debug build; then Build Variants → release, Run ▶).

**Erez, next:** §6.1–6.3 (key, password, backup); §6.4 (uninstall, install
release, log in); go through every screen once on the release build (Albums,
an album, Artists, an artist, Genres, a genre, Playlists, a playlist, Search,
Now Playing and its queue, Shuffle all, Next album, Copy diagnostics). The
version line in the report should read `1.0 (18)` with no `-debug`. Then the
week of daily use.

## 1.0 on the phone, 2026-10-25 (Erez) → 1.0.1 (19)
Release key made (SETUP §6), debug build uninstalled, 1.0 (18) installed and
logged in; every list loaded (first opens slow on a weak home network, as
expected after the uninstall: Artists, the 2300-song playlist). Two fixes:
- **Shrinking renamed exception classes in the log** (`n41` for Media3's
  `HttpDataSourceException`). `proguard-rules.pro` now keeps the names of
  every `Throwable` subclass; the mapping confirms it. +16 KB.
- **The first shuffle of a service run waited for the whole album list** (over
  a minute here). It now starts from the stored ids and refreshes behind them
  (D37). `AlbumShuffleTest` +2.

`./gradlew test` (137) passes; `assembleRelease` signs with the release key.
Not yet on the phone. Then the week of daily use begins, with diagnostics
copies every couple of hours and right after anything odd.
