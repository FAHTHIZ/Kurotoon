# Native Android foundation

KuroToon is moving toward a native Android application based on the upstream
[Mihon project](https://github.com/mihonapp/mihon).

## Pinned upstream

The `mihon/` directory is a Git submodule pinned to:

- Repository: https://github.com/mihonapp/mihon
- Commit: `524fa53043f2f5b2321ea13027e44f093ab4245c`
- Upstream license: Apache-2.0 (see `mihon/LICENSE`)

The pin is deliberate: it makes the upstream source tree reproducible and
keeps its history and license notices intact. Update it only after reviewing
the upstream changes and recording the new commit.

## Migration status

This is the native foundation import, **not a completed KuroToon/Mihon
integration**. The existing React/PWA application and its Aidoku runtime have
not yet been removed. Removing them before the Kotlin application, KuroToon
branding, data migration, and build pipeline are implemented would leave the
current product broken.

## Integration sequence

1. Establish a reproducible Android build from the pinned Mihon source.
2. Rebrand the native application as KuroToon while preserving required
   upstream notices and attribution.
3. Keep Mihon's native source, extension, reader, storage, and backup
   implementations as the canonical Android runtime.
4. Port KuroToon's product layer (visual identity, onboarding, Library
   presentation, Shop, Rewards, and profile) onto native application
   architecture; do not create a parallel source runtime.
5. Implement and test Mihon backup import, including library entries and
   reading progress, before advertising one-tap migration.
6. Remove the old Aidoku runtime and PWA engine only after all Android
   workflows have replacements and the migration/build checks pass.

## Build

Build instructions and required toolchain are maintained by upstream in
[`mihon/README.md`](mihon/README.md) and its Gradle wrapper. This repository
does not yet provide a verified KuroToon-branded APK build.
