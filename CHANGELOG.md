# Changelog

All notable changes to KuroToon are documented here. Versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- The screen no longer times out while you study a dense page: the reader stays lit for 3 minutes after your last touch or page turn, then lets the phone sleep
- Jumping with the page slider right after opening a long-strip chapter is no longer pulled back to the saved panel
- Keep reading on the sleep timer's rest card now brings the soundtrack back instead of leaving it muted

### Added
- Go to page: tap the page number in the reader menu to type the page you want
- Pages that failed when the connection dropped reload by themselves as soon as you're back online, and timeouts or busy servers get two quiet retries before a Retry button appears
- Resume where you left off (Settings > Reader): Resume opens the last chapter you opened, even after skipping ahead, instead of an earlier unread one
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
