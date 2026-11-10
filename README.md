<div align="center">

<img src=".github/assets/logo.png" alt="KuroToon" width="96" height="96">

# KuroToon

**Your manga. Your sources. One beautifully simple home.**

A private, local-first manga, manhwa and webtoon reader for Android, written in Kotlin and Jetpack Compose.

[![Build](https://github.com/FAHTHIZ/Kurotoon/actions/workflows/build.yml/badge.svg)](https://github.com/FAHTHIZ/Kurotoon/actions/workflows/build.yml)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-FF4D4D)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-A35BE8)

<img src=".github/assets/features-marquee.svg" alt="Private by design · No accounts · No analytics · Local library · Auto-scroll reader · Webtoon mode · Chapter celebrations · Particle effects · Universal search" width="100%">

</div>

## Why KuroToon

- **Private by design.** No accounts, no analytics, no ads. Your library, history and downloads stay on your phone.
- **Your own library.** Add the sources you like, follow series, and get new chapters in Updates.
- **A home that feels alive.** A hero carousel of top titles, continue-reading cards and a row per source.
- **Built for long sessions.** Webtoon and paged modes, hands-free auto-scroll with ten speeds, and a small
  celebration each time you finish a chapter.
- **Smooth, not noisy.** Soft embers drift behind the UI and taps light up, at your screen's refresh rate.
  It all switches off with one toggle, follows the system "remove animations" setting, and does less on
  low-memory phones.
- **A shop you earn, not buy.** Reading, daily visits, missions and achievements earn Kuro Points on your
  phone. Spend them on avatars, animated frames, app themes, particle colours, stickers and badges. All of it
  comes from packs, and you can import your own `.kpack` files ([pack format](docs/shop-packs.md)).
- **Browse the way each catalogue is built.** Every source shows its own facets (genres, themes, status, type,
  demographic) as tabs of one-tap tags. Stack them, long-press to exclude, clear them one by one.
- **Save the searches you keep coming back to.** Save any query plus tags as a chip on that source and pin it
  to Home, where it becomes its own feed row. Hide tags you never want to see, and browse your own library by its genres too.
- **Everything else you'd expect.** Universal search across sources (with recent searches), offline downloads,
  tracker sync, source migration, backups and reading stats.

## Screenshots

<div align="center">
<img src=".github/assets/screenshots/home.png" alt="Home" width="300">
</div>

| Browse a source | Finished a chapter |
| --- | --- |
| <img src=".github/assets/screenshots/browse_chips.png" alt="Sort and genre pills" width="400"> | <img src=".github/assets/screenshots/chapter_complete.png" alt="Chapter complete card" width="400"> |
| **Navigation** | **Tap effect** |
| <img src=".github/assets/screenshots/bottom_bar.png" alt="Bottom navigation" width="400"> | <img src=".github/assets/screenshots/tap_burst.png" alt="Tap burst frames" width="400"> |
| **Reading progress orb** | **Your profile** |
| <img src=".github/assets/screenshots/progress_orb.png" alt="Progress orb at 12, 48 and 100 percent" width="400"> | <img src=".github/assets/screenshots/profile.png" alt="Profile with avatar, frame and badges" width="400"> |
| **Kuro Shop** | **Daily missions** |
| <img src=".github/assets/screenshots/shop.png" alt="Kuro Shop" width="400"> | <img src=".github/assets/screenshots/shop_missions.png" alt="Daily missions" width="400"> |
| **Progress on covers** | **Reading activity** |
| <img src=".github/assets/screenshots/library_progress.png" alt="Library covers with read progress bars" width="400"> | <img src=".github/assets/screenshots/reading_activity.png" alt="Reading activity heatmap" width="400"> |

These are rendered from the real Compose code by the screenshot tests (see [Development](#development)).

## Get started

1. Install the APK from the [latest release](https://github.com/FAHTHIZ/Kurotoon/releases/latest), or from the
   newest [Actions run](https://github.com/FAHTHIZ/Kurotoon/actions/workflows/build.yml) for the bleeding edge.
   Most phones want the `arm64-v8a` file; `universal` works everywhere.
2. Open **Browse → Extensions** and add a repository you trust.
3. Install a source, find a series and tap **Add to library**.

The [guides](docs/) cover [getting started](docs/getting-started.md), [your library](docs/library.md),
[tracking](docs/tracking.md), [storage](docs/storage.md), [moving between sources](docs/source-migration.md),
[privacy](docs/privacy.md) and [troubleshooting](docs/troubleshooting.md).

## Reader tips

- Tap the play button in the reader's bottom bar to start **auto-scroll**. Touching the screen pauses it;
  the speed slider lives in the reader settings under General.
- Volume keys can turn pages (reader settings → Controls). While auto-scroll runs they change its speed instead.
- The orb in the corner shows how far through the chapter you are; tap it for the menu.
- Long-press a page to save, share or set it as the cover.

## Development

Requirements: JDK 21 and the Android SDK.

```sh
./gradlew assembleDebug                 # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease               # release APKs (debug-signed unless keystore.properties exists)
./gradlew spotlessApply                 # format
./gradlew :app:testDebugUnitTest -Pscreenshots --tests 'kurotoon.screenshots.*'
                                        # render UI screenshots to app/build/screenshots/
```

Every push to `main` is built by GitHub Actions and the APK is attached to the run. Pushing a `v*` tag (or running
the Release workflow by hand) publishes a GitHub release with per-ABI APKs and checksums. Add the `SIGNING_KEY`
(base64 keystore), `ALIAS`, `KEY_STORE_PASSWORD` and `KEY_PASSWORD` secrets to sign them with your own key.

## License

KuroToon is distributed under the Apache License 2.0. It is built on code from other Apache-2.0 open-source
Android readers; their copyright notices are kept in the source files. See [LICENSE](LICENSE).
