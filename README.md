<div align="center">

<img src="./.github/assets/icon.png" alt="AniZen Logo" width="100"/>

# AniZen

### A high-performance anime & movie platform for Android.
*Built from the ground up for fluidity, intelligence, and total control.*

[![Discord](https://img.shields.io/discord/1242381704459452488?label=Discord&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF&style=flat-square)](https://discord.gg/J2wmZqEJnS)
[![Preview Build](https://img.shields.io/github/actions/workflow/status/salmanbappi/AniZen/preview.yml?branch=preview&label=Preview%20Build&style=flat-square)](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml)
[![Release](https://img.shields.io/github/v/release/salmanbappi/AniZen?style=flat-square)](https://github.com/salmanbappi/AniZen/releases)
[![License](https://img.shields.io/github/license/salmanbappi/AniZen?label=License&color=0877d2&style=flat-square)](/LICENSE)

</div>

---

## What is AniZen?

AniZen is a solo-built anime and movie application for Android. While based on the Aniyomi/Anikku foundation, it has been redesigned from the UI to the network layer into something significantly beyond a fork — a full media platform with its own AI assistant, network infrastructure dashboard, behavioral analytics, and a custom extension ecosystem built specifically for BDIX servers.

Every feature was conceived, designed, and directed by one developer.

---

## Features

### 🎬 Pro Video Engine
- **MPV-Powered** with Zero-Lag optimizations tuned for mid-range hardware
- **Anime4K Neural Upscaling** — real-time presets (Fast / Anime / Cinematic / High), ideal for 480p classic anime. [Read the Anime4K Guide](docs/ANIME4K_GUIDE.md)
- **Motion Interpolation** — smooth frame generation for choppy camera pans, consistent 59.7/60fps output
- **MPVFX Filter Suite** — Sharpen, Blur, Debanding, and Anime4K with card-based UI and custom presets
- **Dynamic Mediacodec Switching** — automatically enables/disables hardware decoding based on active filters
- **High-Quality Scaling** — `ewa_lanczossharp` for sharpest anime lines. [Read the Pro Player Guide](docs/PRO_PLAYER_GUIDE.md)
- **Intuitive Gesture Controls**:
  - Long-press → 2x speed with smooth release animation (zero jitter)
  - Slide left/right → speed control
  - All gestures individually toggleable
- **Dynamic Player Theme** — player UI colors adapt to the current anime cover art

### 📥 1DM-Style Download Engine
- **Multi-threaded chunked downloading** — byte-range splitting with concurrent async threads
- **Intelligent resume** — per-part file tracking, resumes from exact byte position on restart
- **5x auto-retry** — for unstable BDIX/FTP servers
- **Dynamic buffer sizing** — 32KB/64KB/128KB buffers scale with thread count to protect RAM
- **HTML content-type guard** — detects server error pages masquerading as video files
- **Instant cancellation** — `ensureActive()` inside the byte loop for immediate stop
- **External downloader support** — seamless handoff to 1DM and ADM with clipboard-based path fallback

### 🤖 AI Diagnostics & Assistant
- **Diagnostic Assistant** — conversational AI that reads your actual error logs and stack traces
- **Library Context Ingestion** — AI analyzes your collection for recommendations and insights
- **LLM Processor selector** — choose your backend (Gemini, Groq for high-speed inference, etc.)
- **Custom System Prompt** — override default assistant behavior
- **Behavioral Analytics** — AI generates watch pattern insights on demand
- **Groq-summarized release notes** — changelogs written in plain language, not raw git commits

### 📡 Infrastructure Command Center
A full network operations dashboard built into the app:
- **BDIX Nodes** — real-time latency per BDIX server, live health indicators, local saturation %, active node count
- **Global CDN** — endpoint cluster health visualization, latency matrix per source, Endpoint Reliability Index
- **System Logs** — live error alerts with source status and response times

### 📊 Behavioral Statistics
- Genre Distribution radar chart
- Collection Status pie chart (Completed / Ongoing / On Hold / Dropped / Planned)
- Score Distribution bar chart
- Core Metrics — watch time, episode count, mean score, source count
- Source & Extension Infrastructure Analytics — per-source latency and reliability scores
- Temporal Patterns — preferred viewing cycle, sessions/week, peak focus title, dominant series (30d)
- Analytics Persona & Avatar — personalized identifier in system reports

### 🔄 Automatic Watch Tracking
Status updates happen silently with no manual input required:

| Trigger | Status |
|---|---|
| Added to library | Plan to Watch |
| 15 seconds of playback | Watching |
| Final episode finished | Completed |
| 1 month no activity on ongoing series | On Hold / Hiatus |
| Detected drop pattern | Dropped |

All automatic tracking is individually toggleable in settings.

### 📰 Feed System
A fully customizable content discovery homepage:
- **Default rows** — Popular and Latest from your sources, works immediately out of the box
- **Saved searches** — search anything, save as a live updating feed row
- **Saved filters** — lock in genre, year, status, type per row
- **Category-based or source-based** — organize rows your way
- **Draggable reordering** — manage layout with drag handles
- **Flexible placement** — Feed tab in main navbar, browse section, or set as start screen

### 🎨 Dynamic Theming
- **Dynamic Anime Theme** — app-wide colors adapt to the current anime's cover art
- **Dynamic Player Theme** — video player colors match independently
- **Panorama Cover** — landscape cover mode for wide artwork
- Container style, action row spacing, animated transitions — all configurable

### 📚 Library & Tracking
- Series Season Switcher on detail pages
- AI-powered recommendations on every detail page
- Multi-select library with bulk operations
- 5 working trackers: **MAL, AniList, Kitsu, Shikimori, Simkl**
- Auto-sync progress on episode completion
- Local tracking — fully offline, independent of external services
- Next episode airing time display
- Incognito mode — silently pauses history

### 🔌 Custom BDIX Extension Ecosystem
A full extension repository built for Bangladesh's BDIX infrastructure (`@salmanbappi`):

| Extension | Type |
|---|---|
| DhakaFlix 2 (4 servers: English / Hindi / Anime / TV) | Multi · BDIX |
| Cineplex BD | Multi · BDIX |
| Dflix | Multi · BDIX |
| FtpBd | Multi · BDIX |
| FM FTP | Multi · BDIX |
| Udvash | Multi · BDIX |
| Amader FTP | Multi · BDIX |
| Bas Play | Multi · BDIX |
| RoarZone | Multi · BDIX |
| Nagordola | Multi · BDIX |
| IccFtp | Multi · BDIX |
| InfoMedia | Multi · BDIX |
| Jellyfin Bijoy | Multi · Local |
| BDIX Live TV | Multi · BDIX |
| Live Sports | Multi · BDIX |
| Fanush | Multi · BDIX |
| AnimeKai | English |

---

## Performance Optimizations

| Optimization | What it does |
|---|---|
| `Precision.INEXACT` | Skips exact pixel math, offloads cover scaling to GPU — eliminates micro-stutter while scrolling |
| Dynamic parallelism | `limitedParallelism(coreCount.coerceIn(4,12))` — uses all CPU cores for image decoding |
| 25% memory cache | Balanced for MIUI's RAM usage — hundreds of covers cached without triggering force-close |
| 500MB disk cache | High cover retention without excessive storage |
| Refresh rate override | Requests highest mode Android allows — respects system 60Hz lock, never drains battery |
| Kernel-level merge | `transferTo` for part-file merging — minimal CPU overhead on download completion |

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
1. Visit the [Releases](https://github.com/salmanbappi/AniZen/releases) page
2. Download the latest `arm64-v8a` APK
3. Enable *Install from Unknown Sources* and install
4. Package ID is `app.anizen` — installs alongside official Anikku without conflict

### For Developers
```bash
git clone https://github.com/salmanbappi/AniZen.git
# Open in Android Studio
./gradlew assembleDebug
```

Preview builds available in the [Actions tab](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml).

---

## Contributing

Contributions that improve stability, performance, or features are welcome.

- **Bug Reports** — Use [GitHub Issues](https://github.com/salmanbappi/AniZen/issues) with detailed reproduction steps and logs
- **Translations** — Help reach more users via localization

---

## Community & Support

- **Discord** — [Join the AniZen Community](https://discord.gg/J2wmZqEJnS)
- **GitHub Issues** — [Report problems or suggest features](https://github.com/salmanbappi/AniZen/issues)

---

## Credits

Built on the work of:
- [Aniyomi](https://github.com/aniyomiorg/aniyomi)
- [Anikku](https://github.com/komikku-app/anikku)
- [Anime4K](https://github.com/bloc97/Anime4K)
- [mpvEx](https://github.com/marlboro-advance/mpvEx)

---

## License

AniZen is open-source software licensed under the [Apache-2.0 License](LICENSE).

---

<div align="center">
<sub>Designed, directed, and built solo. Every detail intentional.</sub>
</div>
