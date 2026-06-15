<div align="center">

<img src="./.github/assets/icon.png" alt="AniZen Logo" width="100"/>

# AniZen

### A personal, high-performance anime & movie client for Android.
#### AniZen × Miyomi
*A custom media player built with Jetpack Compose, designed for fluid interactions, smart offline playback, and experimental integrations.*

[![Discord](https://img.shields.io/discord/1242381704459452488?label=Discord&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF&style=flat-square)](https://discord.gg/J2wmZqEJnS)
[![Preview Build](https://img.shields.io/github/actions/workflow/status/salmanbappi/AniZen/preview.yml?branch=preview&label=Preview%20Build&style=flat-square)](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml)
[![Release](https://img.shields.io/github/v/release/salmanbappi/AniZen?style=flat-square)](https://github.com/salmanbappi/AniZen/releases)
[![License](https://img.shields.io/github/license/salmanbappi/AniZen?label=License&color=0877d2&style=flat-square)](/LICENSE)

</div>

---

## 📖 About AniZen

AniZen is a solo-built passion project that started on the foundation of Aniyomi and Anikku. It was created to explore how far an Android media client can go when designed with modern programming principles, deep player optimizations, and a focus on fluidity. 

It aims to offer a premium, responsive, and completely personalized viewing space while remaining lightweight and respectful of hardware resources.

---

## ✨ Key Highlights

*   **🎬 Optimized Video Engine:** Powered by a tuned MPV core with zero-lag hardware optimizations. Features real-time **Anime4K Neural Upscaling** to breathe life into classic series and motion interpolation for consistent 60fps output.
*   **📥 Resilient Downloader:** A multi-threaded, chunked download engine featuring intelligent byte-position resume, RAM-optimized dynamic buffering, and automatic fail-safes for unstable servers.
*   **🤖 Helper Integrations:** Optional conversational AI helper to diagnose logs/stack traces and recommend content based on your library context.
*   **📊 Personal Watch Insights:** Generates visual breakdowns (radar and bar charts) of your viewing stats, genre distribution, and source performance.
*   **📰 Adaptive UI & Feeds:** Build your own homepage feeds using custom search/filter rows with drag-and-drop ordering. The player and app interface dynamically shift colors to match cover art.
*   **🔌 Localized Extension Support:** Designed to play nicely with custom extensions optimized for localized high-speed servers.

---

## 🛠️ Technical Architecture

AniZen follows **Clean Architecture** principles to separate business logic, UI, and data handling into a modular structure:

```
app                   # Entry point, dependency injection configuration
├── core              # Shared helpers, common extensions, and base utilities
├── data              # Repositories, database (SQLDelight), and networking (OkHttp)
├── domain            # Core business logic, use cases, and domain models
├── presentation-core # Reusable UI components, themes, and design tokens (Compose)
├── source-api        # Extension API interfaces and definitions
└── source-local      # Local storage and media indexers
```

### Technical Stack & Decisions
*   **Development Platform:** 100% Kotlin with Jetpack Compose for declarative UI.
*   **Concurrency:** Kotlin Coroutines & Flow for asynchronous tasks and state streaming.
*   **Database:** SQLDelight for compile-time safe SQL queries.
*   **Core Shaders:** High-quality `ewa_lanczossharp` scaling for sharpest anime lines.
*   **Scrolling Performance:** Implements `Precision.INEXACT` cover scaling to delegate image processing to the GPU, removing micro-stutter and keeping scrolling fluid on 120/144Hz displays.

---

## 🚀 Getting Started

### For Users
1. Head over to the [Releases](https://github.com/salmanbappi/AniZen/releases) tab.
2. Download the latest `arm64-v8a` release APK.
3. Install the APK (requires enabling *Install from Unknown Sources*).
4. Installs under package ID `app.anizen` — runs side-by-side with official Anikku without issues.

### For Developers
Clone the repository and build using Gradle:
```bash
git clone https://github.com/salmanbappi/AniZen.git
cd AniZen
./gradlew assembleDebug
```
Automated preview builds can also be found in the [Actions tab](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml).

---

## 🤝 Contributing & Support

*   **Bug Reports:** Report issues and attach logs via [GitHub Issues](https://github.com/salmanbappi/AniZen/issues).
*   **Community:** Join our [Discord Server](https://discord.gg/J2wmZqEJnS) to ask questions, chat, or suggest features.

---

## 💖 Credits

AniZen would not be possible without the incredible open-source projects it builds upon:
*   [Aniyomi](https://github.com/aniyomiorg/aniyomi) & [Anikku](https://github.com/komikku-app/anikku) (Core codebase foundations)
*   [Anime4K](https://github.com/bloc97/Anime4K) (Real-time shaders)
*   [mpvEx](https://github.com/marlboro-advance/mpvEx) (MPV integration patterns)

---

## 📄 License

AniZen is open-source software licensed under the [Apache-2.0 License](LICENSE).

---

<div align="center">
<sub>AniZen × Miyomi</sub><br>
<sub>Designed, directed, and built solo. Every detail intentional.</sub>
</div>
