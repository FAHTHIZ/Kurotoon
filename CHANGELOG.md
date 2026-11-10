# Changelog

All notable changes to KuroToon are documented here. Versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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

### Changed
- Bottom navigation: Home, Browse, Library, Updates and Me.
- Everything is private and on-device: no accounts, analytics or crash reporting.
