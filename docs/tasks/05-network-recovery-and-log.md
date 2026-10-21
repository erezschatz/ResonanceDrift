# 05 · Network recovery and playback log

## Promise
Driving through a tunnel or a dead spot pauses the music at worst; it never
*stops* it. When the signal returns, playback continues from where it was
without me touching the phone. And when something does go wrong, I can tap one
button and paste a report that explains it.

## Acceptance
- [x] Custom `LoadErrorHandlingPolicy`: I/O errors retry with backoff for a long
      time instead of failing the item
- [x] `NetworkRecovery`: on `STATE_IDLE` with a network-class error, keep
      position, `prepare()` + `play()` on backoff, and immediately when
      `ConnectivityManager` reports a network; 401/403 from the stream triggers
      re-handshake then retry; **never** skip to the next item on error
- [x] `PlaybackLog`: 300-entry ring buffer (state changes, transitions, errors
      with cause, recovery attempts) persisted to a file, rotated on service
      start; `report()` includes device, app version, current state, previous
      run, this run
- [x] Dev screen gains a "copy diagnostics" button (moves to Now Playing in 11)
- [ ] Tested on the phone with airplane mode toggled mid-track for 10 s, 60 s,
      and across a track boundary
- [x] Unit tests: ring buffer caps and rotation; recovery backoff schedule with
      a fake clock; policy returns retry for I/O and no-retry for 4xx

## Notes
This is the one place we still "fight" anything. Keep it in `playback/` and
keep it small; if it grows past ~200 lines, something is being solved in the
wrong layer.

Built 1.5 (6), 2026-10-21. Recovery is `RecoveryPolicy.kt` (pure decisions +
the loader policy, 92 lines) and `NetworkRecovery.kt` (wiring, 120 lines).
`PlaybackEvents.kt` turns ExoPlayer's analytics events into log lines.
Started before step 04 closed (D24): the first phone job for 1.5 is the full
locked-screen album from step 04, then copy diagnostics after any hiccup —
`state BUFFERING` = data ran out, `audio underrun` = CPU starved,
`suppressed` / `playWhenReady … (audio focus lost)` = another app or the system.
