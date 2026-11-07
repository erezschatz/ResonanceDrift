# 17 · Now Playing: queue list opens at the current song

## Promise
Opening the queue on Now Playing shows the song that is playing, highlighted,
without scrolling.

## Acceptance
- [x] Cause found and written here (see Open) — narrowed, not proven on a
      device; the fix does not depend on it (2026-11-07)
- [x] The queue panel opens with the current song in view, on a short queue
      and on a long one (28 songs, current item 16) — 1.0.2, Erez 2026-11-07
- [x] After an auto-advance or a pick while the panel is open, it follows (or
      knowingly does not; say which in the task) — follows; 1.0.2, Erez
      2026-11-07
- [x] A test for the scroll target, if it can be written without a device —
      `ui/nowplaying/QueuePanelTest` on `queueScrollTarget` (2026-11-07)
- [x] Run on the phone — 1.0.2 (20), 2026-11-07

## Observed (2026-10-30, Erez)
The song list on Now Playing opens at the first song of the queue, not at the
playing track; you scroll down to find it. Expected: scroll to and highlight
the current track. Seen with a restored queue and with a freshly started
shuffled album (e.g. the 17:06 run, where the current track moved by
auto-advance and by picks, such as "Unforgiving Girl (She's Not an)" at
17:53:57, item 8 of 12).

## Fix (built 2026-11-07, 1.0.2 (20), D40)
`QueuePanel` now scrolls in a `LaunchedEffect(currentIndex, queue.size)`
inside the sheet's content: it waits (`snapshotFlow` on `layoutInfo`) for the
list to have a real viewport, then calls
`listState.scrollToItem(queueScrollTarget(currentIndex))`, with
`queueScrollTarget` = two rows of context above the current song. It runs
again whenever the current index changes (auto-advance, a pick) and when the
queue's size changes under it (shuffle appending an album). The highlight was already right: `QueuePanel` paints
the row whose index equals `currentIndex` with `primaryContainer`.

## Open (cause, as far as it can be seen without a device)
The old code passed `currentIndex - 2` as `initialFirstVisibleItemIndex` to
`rememberLazyListState`. That value is applied by the list's first layout
pass. Inside `ModalBottomSheet` the content is composed and laid out while
the sheet is still hidden and animating in; the most likely reading is that
the first pass happened with constraints under which the index could not be
honoured and was clamped to 0, and the state remembered 0 from then on. The
`currentIndex` the screen passes comes from `PlayerConnection.currentIndex`,
which is published on every player event and was right in every log line
(`→ item N`), so a stale index is unlikely. The effect waits for a layout
with a viewport before it scrolls, so a clamped first pass cannot touch it. If the panel still opens at the top on the phone, the
remaining suspect is the `itemsIndexed` key `"$index-${entry.mediaId}"`
rebuilding the list on a re-publish, and the next look is at
`PlayerConnection.publishQueue`.
