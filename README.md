<div align="center">

<img src="./.github/assets/icon.png" alt="AniZen Logo" width="100"/>

# AniZen

### A high-performance, intelligent anime & movie platform for Android.
#### AniZen × Miyomi
*Built from the ground up for fluidity, intelligence, and total control.*

[![Discord](https://img.shields.io/discord/1242381704459452488?label=Discord&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF&style=flat-square)](https://discord.gg/J2wmZqEJnS)
[![Preview Build](https://img.shields.io/github/actions/workflow/status/salmanbappi/AniZen/preview.yml?branch=preview&label=Preview%20Build&style=flat-square)](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml)
[![Release](https://img.shields.io/github/v/release/salmanbappi/AniZen?style=flat-square)](https://github.com/salmanbappi/AniZen/releases)
[![License](https://img.shields.io/github/license/salmanbappi/AniZen?label=License&color=0877d2&style=flat-square)](/LICENSE)

</div>

---

## What is AniZen?

AniZen is a solo-built anime and movie application for Android. Starting from the Aniyomi/Anikku foundation, it has evolved into a comprehensive media platform featuring an integrated AI assistant, a real-time network operations dashboard, rich behavioral analytics, and a performance-oriented custom player.

Every aspect of the application was conceived, designed, and implemented to feel fluid, responsive, and completely user-controlled.

---

## Features

### 🎬 Pro Video Engine
*   **MPV-Powered Core:** High-performance playback with zero-lag optimizations tuned for Android hardware.
*   **Anime4K Neural Upscaling:** Real-time upscaling presets (Fast / Anime / Cinematic / High) ideal for classic anime. [Read the Anime4K Guide](docs/ANIME4K_GUIDE.md)
*   **Smooth Motion & Scaling:** Motion interpolation for consistent 60fps output and high-quality `ewa_lanczossharp` scaling. [Read the Pro Player Guide](docs/PRO_PLAYER_GUIDE.md)
*   **MPVFX Filter Suite:** Custom shaders (Sharpen, Blur, Debanding) with an interactive card-based control UI.
*   **Fluid Gestures:** Long-press for jitter-free 2x speed, horizontal slide for custom speed control.
*   **Dynamic UI:** Player and application interface colors adapt dynamically to the active cover art.

### 📥 High-Speed Download Engine
*   **Multi-Threaded Chunking:** Concurrent asynchronous downloading with byte-range splitting.
*   **Resilient Downloader:** Intelligent per-part resume tracking, auto-retries for unstable servers, and RAM-optimized dynamic buffer sizing.
*   **Content Guard:** Detects and prevents downloading server error pages masquerading as video files.
*   **External Support:** Seamless handoff to popular downloaders like 1DM and ADM.

### 🤖 AI Diagnostics & Assistant
*   **Diagnostic Assistant:** Conversational AI that reads error logs and stack traces directly to troubleshoot issues.
*   **Smart Recommendations:** Contextual recommendations based on library ingestion and watch habits.
*   **Configurable LLMs:** Supports multiple backends (Gemini, Groq, etc.) with custom system prompt overrides.
*   **Smart Release Notes:** AI-summarized changelogs written in plain, human-readable language.

### 📡 Network Operations Dashboard
*   **Real-time Infrastructure Monitoring:** Visualizes live network latency, node health indicators, and bandwidth saturation.
*   **Global CDN & Logs:** Real-time health monitoring of endpoints and live error alerts with response-time tracking.

### 📊 Detailed Behavioral Statistics
*   **Rich Visualizations:** Interactive radar, pie, and bar charts for genres, collection status, and score distributions.
*   **Comprehensive Metrics:** Detailed insights into watch time, temporal viewing habits, and preferred viewing cycles.
*   **Infrastructure Analytics:** Performance metrics (latency and reliability) analyzed per-source.

### 🔄 Automatic Watch Tracking
Status updates happen silently in the background:

| Trigger | Status |
|---|---|
| Added to library | Plan to Watch |
| 15 seconds of playback | Watching |
| Final episode finished | Completed |
| 1 month no activity on ongoing series | On Hold / Hiatus |
| Detected drop pattern | Dropped |

*Note: All automatic tracking features can be individually toggled in Settings.*

### 📰 Customizable Feed System
*   **Dynamic Feed Rows:** Display popular/latest content, or build custom rows from saved searches and filters.
*   **Tailored Layout:** Organise content rows by category or source with draggable reordering.
*   **Flexible Placement:** Set the feed as your homepage, in the main navigation, or within the browse tab.

### 📚 Library & Trackers
*   **Sync Integration:** Supports 5 major trackers (**AniList, MAL, Kitsu, Shikimori, Simkl**) with automatic progress synchronization.
*   **Local Management:** Multi-select bulk operations, offline tracking support, and an Incognito mode to pause history.
*   **Enhanced Navigation:** Built-in season switchers, airing timers, and AI-powered recommendations on detail pages.

### 🔌 Custom Extension Ecosystem
*   **Tailored Extensions:** Supports a dedicated extension repository optimized for high-speed local media servers and networks.

---

## Performance Optimizations

| Optimization | Description | Impact |
|---|---|---|
| `Precision.INEXACT` | Skips exact pixel math, offloads cover scaling to GPU | Eliminates micro-stutter while scrolling |
| **Dynamic Parallelism** | `limitedParallelism(coreCount.coerceIn(4,12))` | Leverages all CPU cores for image decoding |
| **Optimized Cache** | 25% memory cache / 500MB disk cache | High cover retention, balanced for MIUI memory constraints |
| **Refresh Rate Override** | Requests highest mode Android allows | Smooth 120/144Hz navigation without draining battery |
| **Kernel-Level Merge** | `transferTo` for part-file merging | Zero-overhead file assembly on download completion |

---

## Technical Architecture

AniZen follows **Clean Architecture** principles for a modular, maintainable codebase.

```
app                   # Main module, dependency injection
├── core              # Shared utilities, base logic
├── data              # Repository implementations, data sources
├── domain            # Business logic, use cases
├── presentation-core # Reusable UI components, design system
├── source-api        # Extension system interface definitions
└── source-local      # Local media handling
```

**Stack:** 100% Kotlin · Jetpack Compose · Coroutines · SQLDelight · MPV · Coil 3 · OkHttp

---

## Getting Started

### For Users
1. Visit the [Releases](https://github.com/salmanbappi/AniZen/releases) page.
2. Download the latest `arm64-v8a` APK.
3. Enable *Install from Unknown Sources* in Android settings and install the APK.
4. Package ID is `app.anizen` — installs alongside official Anikku without conflict.

### For Developers
```bash
git clone https://github.com/salmanbappi/AniZen.git
# Open in Android Studio
./gradlew assembleDebug
```

Preview builds are available in the [Actions tab](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml).

---

## Contributing

Contributions that improve stability, performance, or features are welcome.

*   **Bug Reports:** Open a [GitHub Issue](https://github.com/salmanbappi/AniZen/issues) with detailed reproduction steps and logs.
*   **Translations:** Help reach more users by contributing to localization.

---

## Community & Support

*   **Discord:** [Join the AniZen Community](https://discord.gg/J2wmZqEJnS)
*   **GitHub Issues:** [Report problems or suggest features](https://github.com/salmanbappi/AniZen/issues)

---

## Credits

Built on the work of:
*   [Aniyomi](https://github.com/aniyomiorg/aniyomi)
*   [Anikku](https://github.com/komikku-app/anikku)
*   [Anime4K](https://github.com/bloc97/Anime4K)
*   [mpvEx](https://github.com/marlboro-advance/mpvEx)

---

## License

AniZen is open-source software licensed under the [Apache-2.0 License](LICENSE).

---

<div align="center">
<sub>AniZen × Miyomi</sub><br>
<sub>Designed, directed, and built solo. Every detail intentional.</sub>
</div>
