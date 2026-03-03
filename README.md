<div align="center">

<img src="./.github/assets/icon.png" alt="AniZen Logo" width="100"/>

# AniZen

### A high-performance anime tracking and discovery application for Android.
Built on Clean Architecture and optimized for a refined, modern user experience.

[![Discord](https://img.shields.io/discord/1242381704459452488?label=Discord&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF&style=flat-square)](https://discord.gg/m29fe9Vdt)
[![Preview Build](https://img.shields.io/github/actions/workflow/status/salmanbappi/AniZen/preview.yml?label=Preview%20Build&style=flat-square)](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml)
[![License: MIT](https://img.shields.io/github/license/salmanbappi/AniZen?label=License&color=0877d2&style=flat-square)](/LICENSE)

</div>

---

## Overview

AniZen is an advanced anime application focused on stability, modularity, and high-fidelity playback. Currently in active development, it combines an intuitive interface with professional-grade video features and robust tracking capabilities.

## Key Features

### 🎬 Advanced Playback Engine
*   **MPV-Powered**: Robust video decoding with broad format support and low overhead.
*   **Anime4K Integration**: Real-time upscaling and denoising algorithms specifically tuned for anime. [Read the Anime4K Guide](docs/ANIME4K_GUIDE.md).
*   **Pro Player Suite**: Zero-lag optimizations, intelligent font caching, and high-quality Jinc-based scaling (`ewa_lanczossharp`). [Read the Pro Player Guide](docs/PRO_PLAYER_GUIDE.md).
*   **Intuitive Controls**: Long-press for 2x speed, YouTube-style gestures, and configurable auto-minimize UI timers.

### 📥 Specialized Downloads
*   **External Downloader Support**: Seamless handoff to 1DM and ADM.
*   **Path Synchronization**: Includes a unique clipboard-based path fallback to ensure external downloads land in the correct local library folders on modern Android versions.
*   **Resumable Internal Downloads**: Intelligent pause/resume logic with persistent notification progress.

### 📊 Management & Discovery
*   **Smart Tracking**: Auto-sync viewing progress with Anilist, MyAnimeList, Kitsu, and more.
*   **Feed Management**: A dedicated Feed tab for saved searches and latest updates, now featuring a fully draggable reordering system.
*   **Anime Suggestions**: Automatically provides related entries and community recommendations.
*   **Library Control**: Options to toggle sync during library updates and hide "Latest" buttons for a cleaner browsing experience.

## Technical Architecture

AniZen follows **Clean Architecture** principles to maintain a highly modular and testable codebase.

```text
app                  # Main application module and dependency injection
├── core             # Shared utilities and base logic
├── data             # Repository implementations and data sources
├── domain           # Core business logic and use cases
├── presentation-core # Reusable UI components and design system
├── source-api       # Interface definitions for the extension system
└── source-local     # Specialized logic for local media handling
```

*   **Language**: 100% Kotlin with Coroutines for efficient concurrency.
*   **UI**: Jetpack Compose for a modern, reactive interface.
*   **Storage**: SQLDelight for type-safe local persistence.

## Getting Started

### For Users
1. Visit the [Releases](https://github.com/salmanbappi/AniZen/releases) page.
2. Download the latest `arm64-v8a` APK (recommended for modern devices).
3. Enable "Install from Unknown Sources" if required and install the application.

### For Developers
1. Clone the repository:
   ```bash
   git clone https://github.com/salmanbappi/AniZen.git
   ```
2. Open the project in the latest version of **Android Studio**.
3. Synchronize Gradle and build using:
   ```bash
   ./gradlew assembleDebug
   ```

## Contributing

We welcome contributions that improve the application's stability or features. Please ensure your contributions align with the existing modular architecture and coding standards.

-   **Bug Reports**: Use the GitHub issue tracker with detailed reproduction steps and logs.
-   **Translations**: Help us reach more users by contributing to our localization efforts.

## Community & Support

Join our official channels for development updates and technical support.

*   **Discord**: [Join the AniZen Community](https://discord.gg/m29fe9Vdt)
*   **GitHub Issues**: [Report problems or suggest features](https://github.com/salmanbappi/AniZen/issues)

## Credits

AniZen is built upon the hard work of numerous open-source contributors and inspired by projects like Aniyomi and Anikku.

## License

AniZen is open-source software licensed under the [MIT License](LICENSE).
