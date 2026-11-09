# 19 · Skips: counted by outcome, not by button

## Promise
Ampache's skip count means "I left this song before it finished", however I
left it: the next button, choosing another album or playlist mid-song, tapping
a search result. Pausing and resuming is not a skip. Stopping altogether is
not a skip either. With plays and skips both honest, an album's track rows
show where I bail on it, and that is the signal for pruning the library.

## Context (2026-11-09)
Erez reset the Ampache play history to 2026-11-07, the day 1.0.2 (D41) made
plays honest. Checking `object_count` since then: ResonanceDrift wrote 147
`stream` rows and 4 `skip` rows, the Firefox web player 104 `stream` rows, no
doubles (same song within 60 s: none). The 4 skips match the four times the
next button was pressed. Albums abandoned mid-song for another album do not
appear as skips, and that is exactly the case the pruning signal needs.

The app never sends a skip. `record_play` has no skip parameter (D36); the
server must be marking the previous `stream` row as `skip` itself when a new
`record_play` arrives before the previous song could have finished. Why an
album switch does not trigger the same rule is the first thing to find out.

## Acceptance
- [ ] The server rule is known and written down in `AMPACHE.md`: which code in
      Ampache's develop branch turns a `stream` row into `skip`, on what
      condition (time since the last play vs. the song's length? a fixed
      threshold? same client only?), and what it needs from the client
- [ ] The definition above is written in `SPEC.md`, with the ambiguous cases
      decided: pause/resume is not a skip; stop is not a skip; a song left
      at ≥ 90 % counts as played (threshold to be confirmed against the
      server rule, which may make it moot)
- [ ] The app does whatever the server rule needs so that an album switch
      mid-song, a search-result tap and the next button all leave a `skip`,
      and a natural track end and a pause/resume never do. If the server
      rule already covers all of these and the 2026-11-09 reading was wrong,
      this is a no-op and the task closes on the evidence
- [ ] If a client-side definition is needed, it lives in `Scrobbler`
      (playback logic stays in the service, CLAUDE.md), with a JVM test per
      case in `ScrobblerTest`
- [ ] Decision entry in `DECISIONS.md` for the definition and anything
      rejected
- [ ] On the phone, one session: song A to its end, song B skipped with next,
      song C left mid-way by choosing another album, song D paused and
      resumed to its end. Expected in `object_count` (`object_type = 'song'`):
      A `stream`, B `skip`, C `skip`, D `stream` — one row each

## Open
- Does Ampache's rule look at `song.time`? Then a song left at 95 % is a
  skip by the server's count and the 90 % threshold is the client's problem
  to define, or to drop.
- The Firefox web player also writes plays. Its skips follow the server's
  rule only; nothing to do here, but the counts will mix two definitions if
  the app ends up with its own.
- Query for the pruning signal, once the data exists: per album, plays and
  skips per track, ordered by track number — `object_count` joined to `song`,
  `object_type = 'song'`, grouped by `count_type`.
