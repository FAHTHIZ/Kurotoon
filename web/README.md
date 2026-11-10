# KuroToon

<p align="center">
  <strong>Your manga. Your sources. One beautifully simple home.</strong>
  <br />
  Discover manga, manhwa and webtoons across sources, keep your reading life organised, and enjoy everything in one reader.
</p>

<p align="center">
  <a href="https://github.com/FAHTHIZ/Kurotoon">Source code</a> ·
  <a href="https://github.com/FAHTHIZ/Kurotoon/issues">Report a problem</a>
</p>

---

KuroToon is an open-source, source-based manga and webtoon reader built around one simple idea: **you shouldn't need to juggle several apps just to find, organise and read the stories you love.**

Add a source repository, install the sources you want, search across them together, and build one personal library. KuroToon brings discovery, reading, downloads, updates and a little bit of fun into a single interface.

It is designed to feel welcoming from the first tap: expressive visuals, clear navigation, thoughtful motion and a reader that gets out of the way when it's time to read.

> **A note on the name:** The repository and app currently use the name **KuroToon**. If you know it as Kuro-chan, you're in the right place.

## ✨ What KuroToon is

KuroToon is not a manga catalogue or a content-hosting service. It is a reader and organiser that connects to sources you choose to add.

- **One home for your reading:** keep series, progress, favourites and history together.
- **Your sources, your choice:** add repositories and install supported extensions instead of being locked into one catalogue.
- **Find it once, search everywhere:** Universal Search fans a query out across your active installed sources.
- **Read the way you like:** use the dedicated reader with webtoon, vertical, left-to-right and right-to-left modes.
- **Keep reading offline:** download chapters to this device and open saved pages without a network connection.
- **Make it yours:** organise your library, tune reader settings, earn Kuro Points and unlock cosmetic items in the Kuro Shop.

## 💜 The experience

### Discover

Start from the home screen, browse available sources, or search across your installed sources with **Universal Search**. Results arrive source by source, so a slow source does not have to hold up the others.

### Add sources without the fuss

The intended everyday flow is deliberately short:

1. Open **Extensions**.
2. Add a repository you trust.
3. Browse the sources it provides.
4. Install a source.
5. Open **Browse** or **Universal Search** and start discovering titles.

Installed sources can be enabled, disabled, refreshed or removed from the Extensions area. KuroToon does not bundle a catalogue of titles or silently choose sources on your behalf.

### Keep your reading life together

Your Library brings together saved series, reading progress, favourites and history. Series can be organised into categories, and the app remembers where you left off so you can pick up where you stopped.

### Read comfortably

Open a series, choose a chapter and settle in. The reader currently includes:

- Webtoon and vertical scrolling modes.
- Left-to-right and right-to-left paged reading modes.
- Saved page progress and chapter history.
- Reader preferences, including display and keep-awake controls.
- Chapter navigation and completion feedback.
- Offline chapter support.

### Take it offline

Queue chapters for download and follow their progress. Downloaded chapter pages are stored locally in the browser's IndexedDB storage and can be opened from the Downloads area without a network connection. Storage availability depends on the device and browser.

### Stay up to date

The Updates area checks the chapters of series in your library against their sources. You can run a check when you want, see newly discovered chapters and review source errors when a check cannot complete.

### A little reward for reading

KuroToon includes a lightweight rewards layer:

- Earn Kuro Points through supported reading activity and missions.
- Build streaks and collect achievements.
- Visit the **Kuro Shop** to unlock on-device cosmetic items such as stickers, reader themes and profile frames.

The Shop is an in-app rewards feature, not a cash marketplace. Unlocks are local to this device.

## 🧭 Main areas

| Area | What it does |
| --- | --- |
| **Home** | Discover titles from your installed sources and continue reading. |
| **Browse** | Explore installed sources and jump into Universal Search. |
| **Universal Search** | Search active sources together and see results as each source responds. |
| **Extensions** | Add repositories, install and manage supported sources, and review source health. |
| **Library** | Find saved series, continue reading, favourites and history. |
| **Updates** | Check your library for newly available chapters. |
| **Downloads** | Manage the download queue and chapters saved for offline reading. |
| **Reader** | Read in your preferred direction and layout, with progress remembered. |
| **Kuro Shop** | Spend reading-earned Kuro Points on cosmetic unlocks. |
| **Profile** | View reading stats, manage preferences and create or restore a KuroToon backup. |

## 🔄 Moving from Mihon to KuroToon

KuroToon is being shaped for readers who already have a reading life in Mihon and want a simpler, more unified home without losing the things that matter.

The long-term goal is a **guided, low-friction migration**: help readers bring over their library and reading organisation, reconnect the sources they use, and continue in KuroToon's own interface rather than making them rebuild everything by hand.

There are important compatibility details to be transparent about:

- **Current source runtime:** the codebase currently uses the Aidoku extension runtime and its supported package format.
- **Mihon APK extensions:** standard Mihon/Tachiyomi Android APK extensions are not currently interchangeable with Aidoku packages, and KuroToon does not yet claim full Mihon extension compatibility.
- **Mihon backup import:** a complete Mihon backup importer is a future goal, not a feature available in this version.
- **Current backup support:** KuroToon can create and restore its own backup format, including local app data, repositories, installed-source information, preferences and source settings. Source packages are re-downloaded from their repositories during restore.

We want migration to feel easy, but we will only call it complete when the compatibility and data-transfer paths are implemented and tested. Until then, please keep your original Mihon installation and backups.

## 🔐 Privacy and content

- KuroToon does not host manga, manhwa, webtoons or other reading content.
- You choose which repositories and sources to add. Only add sources you trust.
- Library data, progress, preferences, rewards and downloads are stored locally by the current app.
- Repository and source requests necessarily contact the services you choose to use. The app's relay may be used for supported network requests.
- KuroToon is not affiliated with Mihon, source operators or content providers.

Please respect creators, publishers, source terms and the laws that apply where you live. Availability and reliability depend on third-party sources, which can change or stop working independently of KuroToon.

## 🛠️ Current implementation

The current repository is a TypeScript/React application built with Vite and TanStack Start. It is delivered as a browser-based app with Progressive Web App support.

| Layer | Current implementation |
| --- | --- |
| UI | React, TypeScript, Tailwind CSS and Framer Motion |
| Routing | TanStack Router / TanStack Start |
| Source runtime | `@nemu.pm/aidoku-runtime` |
| Local app state | IndexedDB and browser local storage |
| Offline app shell | Service worker and web app manifest |
| Tests | Bun test runner |

**Platform status:** this repository currently contains the web/PWA application. A fully native Kotlin Android application is a planned direction; this README does not imply that a native Android build is already available.

## 🚀 Run it locally

### Requirements

- [Bun](https://bun.sh/) (the project uses Bun scripts and a Bun lockfile).
- A recent version of [Git](https://git-scm.com/).

### Setup

```bash
git clone https://github.com/FAHTHIZ/Kurotoon.git
cd Kurotoon
bun install
bun run dev
```

Use the local URL printed by Vite to open the app.

### Project checks

```bash
bun run typecheck
bun run lint
bun test
bun run build
```

The project also includes a production preview script:

```bash
bun run preview
```

## 🗺️ Roadmap

KuroToon is a work in progress. These are directions we want to pursue, not promises that a feature is already shipped or has a release date.

### In progress / planned

- **Native Android:** move toward a polished Kotlin Android app with platform-native storage, downloads, background work and navigation.
- **Mihon migration:** investigate and implement a reliable migration path for library data, reading progress and source setup, with clear compatibility reporting.
- **Mihon extension compatibility:** pursue a first-class Android-compatible path for Mihon/Tachiyomi extensions, with appropriate licensing, attribution, security boundaries and compatibility tests.
- **Better source management:** clearer repository discovery, health reporting, updates and recovery when a source fails.
- **A smoother first run:** make adding trusted repositories and getting to the first chapter as effortless as possible.
- **More reader controls:** continue improving reading layouts, gestures, accessibility and personalisation.
- **Stronger backup and restore:** validate imports more thoroughly and make migrations safer and easier to understand.
- **More discovery tools:** improve search, filtering, recommendations and cross-source organisation.
- **More personalisation:** expand themes, profile cosmetics, rewards and quality-of-life options.

We will update this roadmap as work lands. Features move from planned to available only when they exist in the code and have been verified.

## 🤝 Contributing

Ideas, bug reports and contributions are welcome.

- Browse [open issues](https://github.com/FAHTHIZ/Kurotoon/issues).
- Open an issue with clear reproduction steps for a bug.
- For feature ideas, describe the problem and the experience you would like to see.

Please keep changes focused and include tests when practical.

## 📜 Credits and licensing

KuroToon uses open-source software and depends on third-party projects. Their respective licences and notices remain applicable. In particular, the current source runtime is based on the Aidoku ecosystem; see the relevant package and upstream project notices before redistributing builds.

Mihon is an independent open-source project. Any future use or adaptation of Mihon code must preserve the applicable licence, copyright notices and attribution. KuroToon is not an official Mihon product.

## Disclaimer

KuroToon is a reader and library manager. It does not provide, host or endorse third-party content or sources. You are responsible for the repositories and sources you choose to install and for ensuring your use complies with applicable law and third-party terms.
