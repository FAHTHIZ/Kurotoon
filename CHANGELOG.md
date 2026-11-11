# Changelog

All notable changes to KuroToon are documented here. Versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Search bars put the keyboard away on Enter, and it no longer pops back up after leaving a series opened from the results ([mihon#3124](https://github.com/mihonapp/mihon/issues/3124))
- Chapters hidden by a filter (e.g. Unread) are no longer reported as missing chapters ([mihon#87](https://github.com/mihonapp/mihon/issues/87))
- Adding a series to the library from the reader now links enhanced trackers, like the series page does
- Rotating the screen no longer cancels the sleep timer or stops auto-scroll
- Rotating the screen no longer unmutes the reader's music
- The sound sheet now says when an episode's music couldn't be checked (offline) and offers Retry, instead of showing nothing
- The end-of-series card now actually shows the usual update day (its chapter lookup always failed before)
- Home no longer crashes on launch when your sources start loading their rows before Home finished setting up
- Muting the soundtrack (badge, sheet or sleep timer) fades the music and ambience out instead of cutting them off mid-note
- The screen no longer times out while you study a dense page: the reader stays lit for 3 minutes after your last touch or page turn, then lets the phone sleep
- Jumping with the page slider right after opening a long-strip chapter is no longer pulled back to the saved panel
- Keep reading on the sleep timer's rest card now brings the soundtrack back instead of leaving it muted

### Added
- Long strip can use a different side padding in landscape, so sideways pages stop filling the whole screen ([mihon#155](https://github.com/mihonapp/mihon/issues/155))
- A "Recently added" shelf on Home lists what you saved this month, newest first, so a fresh add is easy to find again ([mihon#2529](https://github.com/mihonapp/mihon/issues/2529))
- Staggered downloads: Settings > Downloads > Pause between chapters makes each source download one chapter at a time, waiting up to a minute between them ([mihon#1958](https://github.com/mihonapp/mihon/issues/1958))
- Settings > Library can name local chapters after their files instead of the ComicInfo.xml title, so renaming a file renames the chapter ([mihon#2393](https://github.com/mihonapp/mihon/issues/2393))
- Check only some series for new chapters: select them in the library and pick "Check for new chapters" from the overflow menu ([mihon#2933](https://github.com/mihonapp/mihon/issues/2933))
- Lock zoom for the paged readers too: pages stay fitted, with no pinch or double-tap zoom ([mihon#1397](https://github.com/mihonapp/mihon/issues/1397))
- Incognito categories: tap the glasses on a category in Edit categories, and its series are read without saving history or progress ([mihon#3367](https://github.com/mihonapp/mihon/issues/3367))
- Updates show the scanlator of each new chapter ([mihon#1914](https://github.com/mihonapp/mihon/issues/1914))
- Reading mode per source: tick "Use for every series from …" in the reader's reading mode sheet, and series from that source without their own pick open in it ([mihon#2140](https://github.com/mihonapp/mihon/issues/2140))
- A failed download with some pages saved can be kept as is from its menu in the download queue, so a source that won't serve a page or two no longer blocks the chapter ([mihon#587](https://github.com/mihonapp/mihon/issues/587))
- The series page's Resume button picks a re-read back up at the page you left, even with every chapter read
- Bookmark notes: in the reader's menu, write why a chapter is bookmarked; the note shows under the chapter on the series page ([mihon#1509](https://github.com/mihonapp/mihon/issues/1509))
- Find duplicates also matches copies linked to the same tracker entry, even when their titles differ ([mihon#84](https://github.com/mihonapp/mihon/issues/84))
- Re-reading a finished chapter remembers your page: History and the reader resume there instead of skipping to the next chapter or page 1 ([mihon#698](https://github.com/mihonapp/mihon/issues/698))
- Per-source default category: in a source's menu, choose where series added from it land; used by the series page, browse, history, home and the reader ([mihon#336](https://github.com/mihonapp/mihon/issues/336))
- Turn incognito on or off from the library's overflow menu ([mihon#1577](https://github.com/mihonapp/mihon/issues/1577))
- Series page shows when you last read it, e.g. "12 of 80 read · 3h 20m · last read 2 weeks ago" ([mihon#1123](https://github.com/mihonapp/mihon/issues/1123))
- Smart update can skip only series with 3/5/10/25+ unread chapters instead of any unread ([mihon#2539](https://github.com/mihonapp/mihon/issues/2539))
- Settings → Downloads → Pause when this many are unread: stop auto-downloading new chapters for a series with a big unread backlog ([mihon#122](https://github.com/mihonapp/mihon/issues/122))
- Settings → Data and storage → Remove downloads not in library, with a list of what will go first ([mihon#565](https://github.com/mihonapp/mihon/issues/565))
- In-app list of series that failed to update, grouped by error, with Open, Migrate and Hide; reachable from the error notification and a banner in Updates ([mihon#132](https://github.com/mihonapp/mihon/issues/132))
- Long-press the music badge to open the sound settings; the badge is also easier to hit
- Finishing a chapter of a series you haven't added offers "Add to library" right on the end-of-chapter card, like WEBTOON's Subscribe
- History rows say "Finished" or the page you stopped on, so half-read chapters stand out
- Ongoing series show the day they usually update under the chapter count ("New chapters usually every Tuesday")
- Stop a running library update right from the "Updating library" snackbar in Library and Updates
- Downloads that stopped because the connection dropped (or Wi-Fi went away with Wi-Fi only on) resume by themselves when it's back
- A one-time hint when episode music starts but your media volume is at zero
- The reader opens muted when your phone is on silent or vibrate (turn off in the sound sheet)
- README redesign: a new hero, a twelve-screen tour and a five-app comparison, all from real screenshots of the current build with Webtoons.com, MangaDex and Asura Scans installed
- A jump button on long chapter lists scrolls straight to the chapter you're up to
- Lock zoom in the long strip so a stray two-finger touch never zooms (Settings > Reader or the reading-mode sheet)
- The end of the latest chapter says you're all caught up and which day new chapters usually arrive
- Undo after swiping a chapter to read, unread or bookmarked
- Home's Continue reading cards show the page you stopped on ("Chapter 37 · p. 12")
- Each series remembers the ambience you picked for it in the reader
- Go to page: tap the page number in the reader menu to type the page you want
- Pages and episode music that failed when the connection dropped reload by themselves as soon as you're back online, and timeouts or busy servers get two quiet retries before a Retry button appears
- Resume where you left off (Settings > Reader): Resume opens the last chapter you opened, even after skipping ahead, instead of an earlier unread one
- Your panels on Home: the latest pages you kept from the reader as a poster row; tap one to jump back into its chapter, See all opens the gallery (hide it in Settings > Appearance > Home)
- Ignore repeated volume-key presses (Settings > Reader > Navigation): one page per press on bouncy buttons
- The reader's top bar says how many chapters are left after this one ("Chapter 37 · 13 chapters left")
- The Resume / Start button on a series names the chapter it opens ("Resume · Ch. 37")
- Bedtime warmth (Settings > Reader): pages warm up and dim slightly from 9:30 PM, easing back by 6:30 AM
- Opening a chapter part-way through shows "Picked up on page N" with a Start over button for a second look from the top
- Reader clock and battery: the time and battery level sit next to the page number, since the reader hides the status bar (Settings > Reader > Time and battery)
- Long-strip chapters reopen on the exact panel you left, not the top of a page that can be many screens tall
- Sound overhaul: episode music now plays through ExoPlayer (reliable HLS on every phone), crossfades between tracks, is buffered two panels early so it lands on its panel, retries broken streams with a fresh token, and is cached so downloaded chapters play their music offline. Calls and other apps pause it (and a short alert resumes it), unplugging headphones never blasts the speaker, Bluetooth hiccups recover by themselves, and the volume sliders follow how loud things actually sound. New: Play alongside other apps, a page-turn volume, loading and retry states, and a resume badge after interruptions.
- Edit info on series pages (overflow menu): change the title, author, artist, tags and description; your edits survive every library refresh
- Continue reading home-screen widget: your last-read titles with their chapter, one tap resumes the chapter
- Saved panels: long-press a page and Keep panel to collect favourite pages in a gallery (Me > Saved panels) that jumps straight back to the chapter
- Onboarding: Next on the storage step now opens the folder picker instead of sitting greyed out; friendlier empty Extensions screen
- Data saver (off / on mobile data / always) with a quality slider: pages are shrunk to WebP through the wsrv.nl proxy, with automatic fallback for sites that refuse it
- Reader sleep timer (15/30/45/60 min or end of chapter) in the Sound sheet: stops auto-scroll, mutes the soundtrack and shows a dim "Time to rest" card
- Reader soundtrack: WEBTOON episode music plays in KuroToon, starting and stopping on the same panels as the official app, with a music badge to mute it. Ambience for any chapter (rain, night, fireplace, ocean, focus, or matched to the series' genres), generated on the phone, plus an optional page-turn sound. Open it from the music button in the reader menu.
- Optional daily reading reminder (Settings > Appearance): one quiet notification at your chosen hour, only on days you haven't read yet, with a Continue button straight into your next chapter. Skipped in incognito.
- Native Android app in Kotlin and Jetpack Compose, replacing the web app.
- Home tab with a featured title, popular rows from your sources and Continue reading.
- Universal Search card at the top of Browse.
- Me tab with an editable nickname and your library numbers.
- KuroToon crimson-on-black theme, icon and splash screen.
- Help guides in [`docs/`](docs/).

- Catalogue-aware browsing: each source's own facets (genres, themes, status, type, demographic…)
  appear as tabs above the results, with stackable tags that include or exclude and a row of what
  is applied.
- Kuro Points, daily missions, achievements and the Kuro Shop: earn points by reading and spend them on
  avatars, frames, themes, particle colours, stickers and badges. Items come from packs and you can import
  your own ([format](docs/shop-packs.md)).
- Local chapter reconciliation for series you keep on the device.
- Webtoon detection: manhwa, manhua and webtoons open in long-strip mode unless you picked a mode.
- Series pages take their accent colours from the cover (Settings > Appearance).
- More like this: a Similar button on series pages lists AniList recommendations and searches your
  sources for any of them.
- Reader extras: hands-free auto-scroll, a reading progress orb, a small celebration when you finish a
  chapter, and tap sparks across the app.
- Saved searches per source: keep a query and its tags as a one-tap chip, rename or delete it, and choose
  whether it shows on Home.
- Home rows for saved searches: each pinned search loads as its own row, and See all opens that search.
- Find a source: once you have a handful of sources, type to narrow the list by name or language.
- Recent searches: your last Universal Search queries show as one-tap pills in Browse and on the search
  screen.
- Hidden tags (Settings > Browse): tags you never want to see are excluded from filtered searches and
  Home's saved-search rows, and titles listing them are dropped from results.
- Popular / Latest switch on each Home source row, remembered per source.
- Tags on a series page work from anywhere: they open the series' source filtered by that tag.
- Continue reading launcher shortcut: long-press the app icon to jump back into your last chapter.
- Read progress bars on library covers (Settings > Appearance).
- Reading activity heatmap in Statistics with chapters, active days and streaks.
- Library tag chips: your library's most common genres sit above the shelf; tap to stack them and narrow
  to series that have all of them.
- Tag menu on series pages: Search all sources, or Hide this tag in one tap.
- Series pages show how many chapters you have read and how long you have spent on the series.
- Daily reading goal in Statistics: a ring fills as you read today's chapters, and the reading reminder
  keeps nudging until the goal is met. Meeting it pays Kuro Points once a day and unlocks On target.
- History day headers show that day's chapters and reading time, like "Today · 5 chapters · 42m".
- Time left in chapter: the reader learns your pace and shows "~4 min left" next to the page number,
  ignoring pauses and quick skims (Settings > Reader).
- Your year in reading: a Statistics recap of this year's time, chapters, most-read series, top genres
  and busiest month. Share it as an image.
  and busiest month.
- New chapters row on Home: library series with unread chapters from the last two weeks, with a count.
- "Because you read X" rows on Home, built from the most common tags in your library and searched on a source that has that tag.
- Hidden tags are now managed as chips in Settings > Browse, with one-tap suggestions from your library tags.
- Ready to binge row on Home: finished series in your library that you have not started yet.
- Saved searches on Home work like feeds: titles that are new since the last visit get a NEW badge and a count.
- Pick back up row on Home: series you started that still have chapters left but have not been opened in three weeks.
- Find box in the expanded tag panel when a source offers lots of tags, so a genre can be picked by typing.
- Find duplicates in the Library menu: series saved more than once (matched by title across sources), with the most-read copy first.
- Choose which Home sections show in Settings > Appearance > Hidden on Home.
- Browse a tag on every source: "Browse on all sources" from a series tag, or your library tags in Universal Search, opens one row per source whose catalogue has that tag (its own genre filter, not a title search).
- Covers in Home and tag rows show a bookmark badge when the series is already in your library.
- Updates folds several same-day chapters of one series into a single row with a "+N more" pill to expand it (toggle in the Updates filter sheet).
- Pull down on Home to reload every row. Hiding the "Because you read" section now also skips its searches.
- "Browse on all sources" moves sources with results to the top as they load.
- Release schedule (Updates > clock icon): library series laid out by the weekday they usually update on, worked out from their recent chapter dates, opening on today. An "Out today" row on Home lists today's series.
- Long-press a cover in any Home source, saved-search or "Because you read" row to add it to your library, or remove it if it's already there.
- Hidden categories: Library menu > Hide this category keeps a category's tab out of sight. Show hidden categories brings them back until the app restarts.
- Chapter list inside the reader: tap the list button in the reader menu to jump to any chapter without leaving the reader.
- Group library by: Library menu > Group library by switches the tabs from your categories to one tab per source, one per publishing status, or a single list.
- Source badge: Library settings > Display > Source name shows which source each cover comes from.
- Add several titles at once: in a source, menu > Select several, tap the covers you want, then Add to library.
- Switch extension stores off: each store in Browse > Extensions > menu > Extension stores has a switch that hides its extensions without removing it.
- 18+ extensions are hidden on a fresh install, so the first extension list isn't a wall of adult sources. Turn them on in Settings > Browse > Allowed content warnings.

### Changed
- Bottom navigation: Home, Browse, Library, Updates and Me.
- Everything is private and on-device: no accounts, analytics or crash reporting.
