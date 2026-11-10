# KuroToon

**Your manga. Your sources. One beautifully simple home.**

KuroToon is an open-source, source-based manga, manhwa and webtoon reader for Android, written in
Kotlin and Jetpack Compose. It is private by design: no accounts, no analytics, and your library
never leaves your phone. Home, Browse, Library, Updates and Me sit behind a dark crimson look built
for reading at night. Guides live in [`docs/`](docs/).

## Status

The app is being moved from the previous React/TypeScript PWA (kept under [`web/`](web/) as a
reference until every feature has a Kotlin replacement) to this native Kotlin codebase, one piece
at a time. See [`web/NATIVE_PLAN.md`](web/NATIVE_PLAN.md) for the original plan.

## Build

Requirements: JDK 21 and the Android SDK.

```sh
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease      # release APKs (signed with the debug key unless keystore.properties exists)
```

Every push to `main` is built by GitHub Actions; the APK is attached to the workflow run as an artifact.

## License

KuroToon is distributed under the Apache License 2.0. It includes code from the
Mihon open source project (Apache-2.0). See [LICENSE](LICENSE).
