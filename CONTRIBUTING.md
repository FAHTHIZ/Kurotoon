# Contributing to KuroToon

Thanks for helping out. Bug reports and feature requests go in
[issues](https://github.com/FAHTHIZ/Kurotoon/issues/new/choose); pull requests are welcome.

## Before you start

- Comment on the issue you want to work on so others know.
- KuroToon does not host or fix extensions. Source problems belong to the extension's repository.

## Development

Requirements: JDK 21 and the Android SDK (Android Studio works out of the box).

```sh
./gradlew assembleDebug                 # build a debug APK
./gradlew testDebugUnitTest             # unit tests
./gradlew spotlessApply                 # format Kotlin and Gradle files
```

The app is written in Kotlin with Jetpack Compose, Voyager navigation, Metro dependency injection,
SQLDelight and moko-resources for strings. KuroToon-specific UI lives under
`app/src/main/java/kurotoon/`.

- Keep changes focused and test them on a phone and a tablet layout.
- New user-facing text goes in `i18n/src/commonMain/moko-resources/base/strings.xml`.
- Run `./gradlew spotlessCheck testDebugUnitTest` before opening a pull request; CI runs the same.

## Translations

Edit the files under `i18n/src/commonMain/moko-resources/<language>/` and open a pull request.
