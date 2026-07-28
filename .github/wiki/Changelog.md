# 📋 Changelog

All notable changes to AniZen are documented here.

Format follows a modified [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — categories: **Added**, **Changed**, **Improved**, **Removed**, **Fixed**, **Other**.

---

## [Unreleased]

### Fixed
- Resolve 0 MB downloads by adding HLS master playlist support in native engine
- Ensure new episodes are enqueued when **Auto Download** is enabled
- Decrease long-press speed sliding sensitivity
- Fix episode number parsing ([@Secozzi](https://github.com/Secozzi)) ([#2096](https://github.com/aniyomiorg/aniyomi/pull/2096))
- Fix stop/continue anime download button ([@Secozzi](https://github.com/Secozzi)) ([#2099](https://github.com/aniyomiorg/aniyomi/pull/2099))

### Added
- External player: add mpvKt ([@Secozzi](https://github.com/Secozzi)) ([#1674](https://github.com/aniyomiorg/aniyomi/pull/1674))
- Player: video filters ([@abdallahmehiz](https://github.com/abdallahmehiz)) ([#1698](https://github.com/aniyomiorg/aniyomi/pull/1698))
- Player: better auto sub select ([@Secozzi](https://github.com/Secozzi)) ([#1706](https://github.com/aniyomiorg/aniyomi/pull/1706))
- Downloader: copy file location when using external downloader ([@quickdesh](https://github.com/quickdesh)) ([#1758](https://github.com/aniyomiorg/aniyomi/pull/1758))

### Improved
- Show "Now" instead of "0 minutes ago" ([@Secozzi](https://github.com/Secozzi)) ([#1715](https://github.com/aniyomiorg/aniyomi/pull/1715))

### Fixed
- Fix enhanced tracking for Jellyfin ([@Secozzi](https://github.com/Secozzi)) ([#1656](https://github.com/aniyomiorg/aniyomi/pull/1656), [#1658](https://github.com/aniyomiorg/aniyomi/pull/1658))
- Fix airing time not showing on anime screen ([@Secozzi](https://github.com/Secozzi)) ([#1720](https://github.com/aniyomiorg/aniyomi/pull/1720))
- Fix hidden categories getting reset after delete/reorder ([@cuong-tran](https://github.com/cuong-tran)) ([#1780](https://github.com/aniyomiorg/aniyomi/pull/1780))
- Fix episode progress not being saved and duplicate tracks ([@perokhe](https://github.com/perokhe)) ([#1784](https://github.com/aniyomiorg/aniyomi/pull/1784), [#1785](https://github.com/aniyomiorg/aniyomi/pull/1785))

### Other
- Merge from mihon until 0.16.5 → 0.17.0 ([@Secozzi](https://github.com/Secozzi)) ([#1663](https://github.com/aniyomiorg/aniyomi/pull/1663), [#1693](https://github.com/aniyomiorg/aniyomi/pull/1693), [#1804](https://github.com/aniyomiorg/aniyomi/pull/1804))

---

## [v0.16.4.3] — 2024-07-01

### Fixed
- Fix extensions disappearing due to ClassLoader errors ([@jmir1](https://github.com/jmir1)) ([`959f84a`](https://github.com/aniyomiorg/aniyomi/commit/959f84ab41859f90c458c076d83d363ae086e47f))

---

## [v0.16.4.2] — 2024-07-01

### Fixed
- Hotfix: eliminate all Proguard issues causing errors and crashes ([@jmir1](https://github.com/jmir1))

---

## [v0.16.4.1] — 2024-07-01

### Fixed
- Hotfix: address errors with extensions ([@jmir1](https://github.com/jmir1))

---

## [v0.16.4.0] — 2024-07-01

### Fixed
- PiP not broadcasting intent on Android 14+ ([@quickdesh](https://github.com/quickdesh)) ([#1603](https://github.com/aniyomiorg/aniyomi/pull/1603))
- Advanced player settings crash on Android ≤ 10 ([@perokhe](https://github.com/perokhe)) ([#1627](https://github.com/aniyomiorg/aniyomi/pull/1627))

### Improved
- Hide skip intro button when skipped amount equals 0 ([@abdallahmehiz](https://github.com/abdallahmehiz)) ([#1598](https://github.com/aniyomiorg/aniyomi/pull/1598))

### Other
- Merge from mihon until 0.16.2 → 0.16.4 ([@Secozzi](https://github.com/Secozzi)) ([#1578](https://github.com/aniyomiorg/aniyomi/pull/1578), [#1601](https://github.com/aniyomiorg/aniyomi/pull/1601))

---

> For the full git history, see [GitHub commits](https://github.com/salmanbappi/AniZen/commits/master).
