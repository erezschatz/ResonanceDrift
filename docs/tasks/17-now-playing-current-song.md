# 17 · Now Playing: queue list opens at the current song

## Promise
Opening the queue on Now Playing shows the song that is playing, highlighted,
without scrolling.

## Acceptance
- [ ] Cause found and written here (see Open)
- [ ] The queue panel opens with the current song in view, on a short queue
      and on a long one (28 songs, current item 16)
- [ ] After an auto-advance or a pick while the panel is open, it follows (or
      knowingly does not; say which in the task)
- [ ] A test for the scroll target, if it can be written without a device
- [ ] Run on the phone

## Observed (2026-10-30, Erez)
The song list on Now Playing opens at the first song of the queue, not at the
playing track; you scroll down to find it. Expected: scroll to and highlight
the current track. Seen with a restored queue and with a freshly started
shuffled album (e.g. the 17:06 run, where the current track moved by
auto-advance and by picks, such as "Unforgiving Girl (She's Not an)" at
17:53:57, item 8 of 12).

## Open
`ui/screens/nowplaying/QueuePanel.kt:34` already starts the list at
`currentIndex - 2` (`rememberLazyListState(initialFirstVisibleItemIndex = …)`),
so the report means that value is wrong or stale when the panel opens, or the
list is replaced after it. Things to check, in order: what `currentIndex` is
at that moment after a restore and after a shuffle's "new queue"
transition (the log shows a doubled transition there, D36); whether the
`itemsIndexed` key `"$index-${entry.mediaId}"` makes the list rebuild and
reset its position when the queue is re-published; whether the sheet is
composed before the queue flow has its first value. The highlight is a
separate question; look at what `QueuePanel` marks as current.
