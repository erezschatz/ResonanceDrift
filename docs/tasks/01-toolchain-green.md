# 01 · Toolchain green

## Promise
On the Mac, the project opens in Android Studio, syncs, builds, passes
its unit tests and installs on the Pixel 10 showing the placeholder screen.
Last time, toolchain setup was the biggest hurdle; this step exists so it is
faced first and alone.

## Acceptance
- [x] Mac: `docs/SETUP.md` steps 1, 3, 4, 5 completed; Gradle sync green
- [x] Mac: `./gradlew test` passes (`AlbumNamesTest`, 5 tests)
- [x] Mac: Run ▶ installs on the phone; the "ResonanceDrift" placeholder shows
- [x] Any version pinned in `gradle/libs.versions.toml` that Studio flags as
      non-existent or incompatible is corrected, with a changelog line
- [x] `docs/SETUP.md` corrected wherever reality differed from the guide

## Notes
The Windows PC is a backup, not part of this step (D19). If it is ever set
up: `docs/SETUP.md` steps 2, 3, 4, 5, and Run ▶ should install **over** the
Mac build without an uninstall (proves the shared debug key).
Expected failure modes and fixes are in `docs/SETUP.md` §9. If sync fails,
paste the *full* Build-tab output into the session, not the summary line.
