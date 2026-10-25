# Plan

Phase 1, in order. One file per step in `tasks/`; each states the promise from
the user's point of view and the acceptance criteria that make it done. Steps
are done strictly in order unless a decision in `DECISIONS.md` says otherwise.

Status is kept **here only**: `todo` · `in progress` · `blocked (why)` · `done (date)`.
A step is `done` when every acceptance criterion in its file is ticked and the
result has been run on the phone (not just compiled).

| # | Step | Status |
|---|------|--------|
| 00 | [Skeleton and documentation](tasks/00-skeleton.md) | done 2026-10-17 (skeleton pushed 2026-10-12; docs restructured 2026-10-17) |
| 01 | [Toolchain green](tasks/01-toolchain-green.md) | done 2026-10-19 (Mac; Windows optional per D19) |
| 02 | [Login and session](tasks/02-login-and-session.md) | done 2026-10-19 |
| 03 | [Ampache client](tasks/03-ampache-client.md) | done 2026-10-21 (no UI; verified by fixtures from the real server) |
| 04 | [Playback service — one album through a locked screen](tasks/04-playback-service.md) | done 2026-10-23 (1.6 on phone: album through a locked screen, Bluetooth) |
| 05 | [Network recovery and playback log](tasks/05-network-recovery-and-log.md) | done 2026-10-23 (1.6 on phone: airplane mode, handovers; two open notes in the task) |
| 06 | [Library data layer](tasks/06-library-data.md) | done 2026-10-24 (no UI; JVM tests + Room tests on the emulator) |
| 07 | [Navigation, bottom bar, mini player](tasks/07-navigation-and-chrome.md) | done 2026-10-24 (1.7 on phone) |
| 08 | [Browse: Albums, Artists, Genres and their detail screens](tasks/08-browse.md) | done 2026-10-24 (1.8 on phone) |
| 09 | [Playlists (light)](tasks/09-playlists.md) | done 2026-10-24 (1.10 on phone) |
| 10 | [Search](tasks/10-search.md) | done 2026-10-24 (1.11 on phone) |
| 11 | [Now Playing](tasks/11-now-playing.md) | done 2026-10-24 (1.12 on phone; 1.13 spacing) |
| 12 | [Album shuffle](tasks/12-album-shuffle.md) | done 2026-10-25 (1.14 field test on phone; 1.15 log tweaks) |
| 13 | [Queue persistence and scrobbling](tasks/13-queue-persistence.md) | done 2026-10-25 (1.16 on phone) |
| 14 | [Release build and phase-1 sign-off](tasks/14-release.md) | todo |

## Milestones

- **After 04:** the reason for the rewrite is proven or disproven — an album
  plays through a locked screen, track to track, with no UI beyond a play
  button. Nothing after 04 is worth building if 04 fails.
- **After 11:** daily use is possible (browse, play, Now Playing). ReDrift can
  be left behind at this point if shuffle is not missed.
- **After 14:** phase 1 complete; the project goes quiet.

## Not in phase 1

Lyrics, ratings, dark theme, tablet, Auto, Play Store, offline. See
`DECISIONS.md` D4–D7. Phase 2 (portfolio polish) is not planned here.
