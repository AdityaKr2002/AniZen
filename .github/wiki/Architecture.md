# 🏗️ Architecture

AniZen follows **Clean Architecture** with a strict multi-module Gradle structure. Each module has a single responsibility, making the codebase maintainable, testable, and scalable.

---

## 📦 Module Map

```
AniZen/
├── app/                    # Entry point · DI configuration · Navigation (Voyager)
├── core/                   # Shared helpers · common extensions · base utilities
├── core-metadata/          # Metadata models shared across modules
├── data/                   # Repositories · SQLDelight DB · OkHttp networking
├── domain/                 # Business logic · Use Cases · Domain Models
├── presentation-core/      # Reusable Compose UI · Themes · Design tokens
├── source-api/             # Extension API interfaces & definitions
├── source-local/           # Local storage · media indexers
├── anikku-tracker/         # Tracker integrations (AniList, MAL, etc.)
├── i18n/                   # Core internationalization strings
├── i18n-ank/               # Anikku-specific i18n strings
├── i18n-sy/                # Sy-specific i18n strings
├── flagkit/                # Feature flag management
├── telemetry/              # Analytics & crash reporting
├── macrobenchmark/         # Performance benchmarks
├── presentation-widget/    # Android homescreen widget UI
└── buildSrc/               # Shared Gradle build logic & dependency versions
```

---

## 🔄 Dependency Flow

```
app
 └── presentation-core
      └── domain
           └── data
                ├── source-api
                ├── source-local
                └── core
```

- **`app`** depends on everything (DI wiring)
- **`domain`** has **zero** Android/framework dependencies — pure Kotlin
- **`data`** implements domain interfaces using Room/SQLDelight + OkHttp
- **`presentation-core`** consumes domain via ViewModels/StateFlows

---

## 🛠️ Technology Stack

| Layer | Technology | Rationale |
|:---|:---|:---|
| **Language** | 100% Kotlin | Coroutines, type safety, expressiveness |
| **UI** | Jetpack Compose | Declarative, less boilerplate, Compose animation |
| **Navigation** | Voyager | Lightweight, multi-stack, Compose-native |
| **Concurrency** | Kotlin Coroutines + Flow | Structured concurrency, state streaming |
| **Database** | SQLDelight | Compile-time SQL safety, multiplatform-ready |
| **Networking** | OkHttp + Ktor | Interceptor-based, coroutine-friendly |
| **DI** | Koin | Lightweight, Compose-aware, no code generation |
| **Image Loading** | Coil | Coroutine-native, Compose integration |
| **Video Player** | MPV-Android (libmpv) | Most capable Android media engine |
| **Upscaling** | Anime4K GLSL Shaders | Open-source, real-time neural upscaling |
| **Scaling** | `ewa_lanczossharp` | Jinc-based, preserves anime line sharpness |

---

## 🎬 Player Internals

The player is built on top of [mpv-android](https://github.com/mpv-android/mpv-android) with significant custom layers:

```
PlayerActivity (Compose)
 └── MPVView (SurfaceView + libmpv bindings)
      ├── ShaderManager          # Anime4K / ewa_lanczossharp loading
      ├── FilterController       # MPVFX filter pipeline (Copy Mode switching)
      ├── InterpolationManager   # Frame generation + display-resample switching
      ├── SubtitleLoader         # External subtitle URI injection
      ├── GestureHandler         # All touch gestures (pinch, swipe, long-press)
      └── StatsPoller            # Efficient Page 6 stats polling
```

---

## 📥 Downloader Internals

```
DownloadManager
 ├── ChunkedDownloader          # Byte-range splitter + BufferPool
 │    └── PartRecovery          # Per-part size verification + retry logic
 ├── HLSDownloader              # Segment fetcher + AES-128 decryption
 ├── DASHDownloader             # FFmpeg muxer + progress estimation
 ├── ExternalHandoff            # 1DM/ADM delegation with headers
 ├── StreamPrefetcher           # Background URL pre-resolution
 └── StorageGuard               # 200 MB safety buffer check
```

---

## 📊 Key Patterns

| Pattern | Usage |
|:---|:---|
| **Repository** | All data sources abstracted behind interfaces in `domain` |
| **Use Cases** | Single-responsibility business operations in `domain` |
| **StateFlow** | UI state management from ViewModels to Compose |
| **Interceptors** | OkHttp chain for auth, retry, Cloudflare bypass |
| **SQLDelight Migrations** | All schema changes via `.sqm` migration files |

---

## 🗂️ Credits & Upstream

AniZen is built on top of:

| Project | Role |
|:---|:---|
| [Aniyomi](https://github.com/aniyomiorg/aniyomi) | Core codebase foundation |
| [Anikku](https://github.com/komikku-app/anikku) | Additional anime-specific features |
| [Anime4K](https://github.com/bloc97/Anime4K) | Real-time GLSL upscaling shaders |
| [mpvEx](https://github.com/marlboro-advance/mpvEx) | MPV integration patterns |
