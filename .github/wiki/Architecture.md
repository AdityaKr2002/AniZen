# Architecture

Codebase architecture, module dependencies, and technical stack details.

---

## Architecture overview

AniZen is structured around **Clean Architecture** guidelines, splitting domain logic, data persistence, and presentation across isolated Gradle modules.

```
AniZen/
├── app/                    # Application entry point, Koin DI, Voyager Navigation
├── core/                   # Shared utilities, extensions, and core models
├── data/                   # Repositories, SQLDelight database, OkHttp network layer
├── domain/                 # Core business logic, Use Cases, pure Kotlin models
├── presentation-core/      # Jetpack Compose UI components, design tokens, themes
├── source-api/             # Extension interfaces and source abstractions
├── source-local/           # Local media indexing and storage handlers
├── anikku-tracker/         # Tracking service integrations (AniList, MyAnimeList)
└── i18n/                   # String resources and internationalization packages
```

---

## Technical stack

- **Programming Language**: 100% Kotlin with Coroutines and Flow for asynchronous operations.
- **UI Framework**: Jetpack Compose using Material Design 3 guidelines.
- **Navigation**: Voyager multi-stack Compose navigation library.
- **Database Layer**: SQLDelight providing compile-time safe SQL queries.
- **Dependency Injection**: Koin framework.
- **Media Playback**: `mpv-android` (libmpv native bindings) with custom GLSL shader support.

---

## Module rules

- **`domain`** contains no Android framework dependencies and consists strictly of pure Kotlin code.
- **`data`** implements repositories declared in `domain` using SQLDelight and OkHttp.
- **`presentation-core`** depends on `domain` models to expose reactive UI states using `StateFlow`.
