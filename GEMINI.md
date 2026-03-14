# AniZen Project: Comprehensive Architectural & Structural Map

## 1. Project Overview
AniZen is a high-performance, fluid, and intelligent anime and movie platform for Android. It is a sophisticated fork of Aniyomi and Anikku, engineered for maximum performance, multi-threaded decoding, and architectural cleanliness.

### 🚀 Key Technical Highlights
- **Dynamic FPS Control**: Supports 120Hz/144Hz displays with precision timing.
- **Aggressive Parallelism**: 12+ thread image decoding and multi-threaded HTTP download engine.
- **Clean Architecture**: Strict separation between UI (`app`), Domain Logic (`domain`), and Data/Infrastructure (`data`).
- **Modern UI Stack**: 100% Jetpack Compose with Voyager for robust screen navigation.
- **Pro Video Engine**: Powered by MPV and FFmpeg with custom hardware acceleration optimizations.

---

## 2. Directory Structure & Component Mapping

### 🏛️ Core Modules
The project follows a multi-module architecture to ensure scalability and clean dependency management.

#### `/app` (The Presentation Layer)
- **Purpose**: Contains the Android application code, UI components, and ViewModels (ScreenModels).
- **Key Packages**:
    - `eu.kanade.tachiyomi.ui`: Voyager Screens and ScreenModels for all features (Browse, Library, History, Player).
    - `eu.kanade.presentation`: Pure Compose components and screen layouts.
    - `eu.kanade.tachiyomi.data`: App-level data management (Downloads, Tracking, Updates).
- **Entry Point**: `eu.kanade.tachiyomi.ui.main.MainActivity.kt`

#### `/domain` (The Business Logic Layer)
- **Purpose**: Contains pure Kotlin logic, use cases (Interactors), and domain models. No Android dependencies.
- **Key Components**:
    - `tachiyomi.domain.anime`: Interactors for anime (GetLibraryAnime, SetAnimeCategories, etc.).
    - `tachiyomi.domain.source`: Interactors for managing sources and extensions.
    - `tachiyomi.domain.track`: Logic for syncing with services like MyAnimeList and AniList.

#### `/data` (The Data Infrastructure Layer)
- **Purpose**: Implementation of repositories, database logic (SQLDelight), and network services.
- **Key Components**:
    - `/src/main/sqldelight`: SQL database schemas and queries.
    - `tachiyomi.data.anime`: Repository implementations for anime data.
    - `tachiyomi.data.source`: Logic for handling extensions and source registries.

---

### 🧱 Supporting Modules
- **`/core`**: Common utilities, preferences, and base classes used across all modules.
- **`/presentation-core`**: Base Compose components, themes, and design system elements.
- **`/i18n`**: Internationalization modules (Base, Anikku, Sy, KMK) using Moko-resources for multi-platform string management.
- **`/source-api`**: Common interfaces and models for the extension ecosystem.
- **`/telemetry`**: Logic for error reporting and diagnostics (Firebase/No-op variants).

---

## 3. Feature-Specific Architecture

### 🔄 Migration System
- **Models**: Located in `tachiyomi.domain.source.model`.
- **Logic**: Managed by `MigrateSourceScreenModel.kt` and `MigrateAnimeScreenModel.kt`.
- **UI**: Handled in `eu.kanade.presentation.browse.MigrateSourceScreen.kt`.
- **Data Flow**: `GetSourcesWithFavoriteCount` interactor fetches source data, which is then presented in the migration tab.

### 📝 Notes System
- **Implementation**: `AnimeNotesScreen.kt` and `AnimeNotesTextArea.kt`.
- **Rich Text**: Uses a `RichTextState` for formatted text editing (Bold, Italics, etc.).
- **Persistence**: Notes are stored in the `Anime` domain model and persisted in the SQLDelight database.

---

## 4. Development & Build Workflow

### 🛠️ Key Gradle Tasks
- **Build Debug**: `./gradlew assembleStandardDebug`
- **Build Release**: `./gradlew assembleStandardRelease`
- **Clean**: `./gradlew clean`
- **Lint**: `./gradlew lintStandardDebug`

### 🎨 Theming System
- **Dynamic Theming**: Adapts app colors to the current anime's cover art using palette generation.
- **Player Theming**: Independent color adaptation for the video player UI.

---

## 5. Active Development: Conductor Tracks
Progress is tracked via the `conductor/` directory using specialized "Tracks".
- **Current Track**: `feature-parity-ui-refinement`
- **Objective**: Replicate Komikku's multi-source migration selection and enhanced rich-text notes.

### 📋 TODO List (Feature Parity)
- [x] **Migration**: Implement checkboxes and BulkSelectionToolbar (Select All, None, Enabled, Pinned).
- [x] **Migration**: Sync bottom sheet search with global state.
- [x] **Migration**: Update wording to "Migrating" in active states.
- [x] **Notes**: Reorder layout (Editor on top, Title below).
- [x] **Notes**: Implement character limit (250 chars) excluding spaces.
- [x] **Notes**: Ensure Bold, Italics, Underline, Bullet/Numbered list support.

---

## 6. Security & Identity
- **Signing Fingerprint**: `c7ebe223044970f2f9738f600dc25c180d3ed03994e088aaf5709338c57b93af`
- **Hardcoded IDs**: Always use stable 64-bit Long IDs for sources to prevent Obsolete status in Mihon/Anikku.
