<div align="center">

<img src="./.github/assets/icon.png" alt="AniZen Logo" width="100"/>

# AniZen

### A high-performance anime & movie platform for Android.
#### AniZen × Miyomi
*Built from the ground up for fluidity, intelligence, and total control.*

[![Discord](https://img.shields.io/discord/1242381704459452488?label=Discord&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF&style=flat-square)](https://discord.gg/J2wmZqEJnS)
[![Preview Build](https://img.shields.io/github/actions/workflow/status/salmanbappi/AniZen/preview.yml?branch=preview&label=Preview%20Build&style=flat-square)](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml)
[![Release](https://img.shields.io/github/v/release/salmanbappi/AniZen?style=flat-square)](https://github.com/salmanbappi/AniZen/releases)
[![License](https://img.shields.io/github/license/salmanbappi/AniZen?label=License&color=0877d2&style=flat-square)](/LICENSE)

</div>

---

## Overview

AniZen is a solo-built anime and movie application for Android, built on the Aniyomi/Anikku foundation. It is redesigned from the user interface to the network layer to operate as a high-performance media platform.

---

## Technical Architecture

AniZen is designed using Clean Architecture principles to maintain a highly modular and decoupled codebase.

```
app                   # Application entry point, dependency injection
├── core              # Shared utilities, common extensions, and base logic
├── data              # Repositories, database (SQLDelight), and network layer (OkHttp)
├── domain            # Core business logic, domain models, and use cases
├── presentation-core # UI components, themes, and design system (Jetpack Compose)
├── source-api        # Extension & source interface definitions
└── source-local      # Local media parsing and storage handling
```

### Technology Stack
*   **Language:** 100% Kotlin
*   **UI Framework:** Jetpack Compose (declarative UI)
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Database:** SQLDelight
*   **Playback Core:** MPV (via native libmpv bindings)
*   **Image Loading:** Coil 3
*   **Networking:** OkHttp

---

## Getting Started

### For Users
1. Head over to the [Releases](https://github.com/salmanbappi/AniZen/releases) section.
2. Download the latest `arm64-v8a` APK.
3. Install the APK on your device (ensure *Install from Unknown Sources* is enabled).
4. Installs as `app.anizen` without conflicting with official Anikku.

### For Developers
Clone the repository and open it in Android Studio:
```bash
git clone https://github.com/salmanbappi/AniZen.git
cd AniZen
./gradlew assembleDebug
```
Preview builds are generated via GitHub Actions and are accessible in the [Actions tab](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml).

---

## Contributing & Support

*   **Bug Reports:** Submit issue reports on [GitHub Issues](https://github.com/salmanbappi/AniZen/issues).
*   **Community:** Join discussions and get help in the [Discord Server](https://discord.gg/J2wmZqEJnS).

---

## Credits

Built on top of excellent open-source projects:
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
