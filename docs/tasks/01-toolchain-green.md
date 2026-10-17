# 01 · Toolchain green

## Promise
On both machines, the project opens in Android Studio, syncs, builds, passes
its unit tests and installs on the Pixel 9 showing the placeholder screen.
Last time, toolchain setup was the biggest hurdle; this step exists so it is
faced first and alone.

## Acceptance
- [ ] Mac: `docs/SETUP.md` steps 1, 3, 4, 5 completed; Gradle sync green
- [ ] Mac: `./gradlew test` passes (`AlbumNamesTest`, 5 tests)
- [ ] Mac: Run ▶ installs on the phone; the "ResonanceDrift" placeholder shows
- [ ] Windows: steps 2, 3, 4, 5; sync green; Run ▶ installs **over** the Mac
      build without an uninstall (proves the shared debug key)
- [ ] Any version pinned in `gradle/libs.versions.toml` that Studio flags as
      non-existent or incompatible is corrected, with a changelog line
- [ ] `docs/SETUP.md` corrected wherever reality differed from the guide

## Notes
Expected failure modes and fixes are in `docs/SETUP.md` §9. If sync fails,
paste the *full* Build-tab output into the session, not the summary line.
