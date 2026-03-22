# Implementation Plan: Performance & Stability Polish

## Phase 1: Storage Layer (SAF) Caching
- [x] Implement `LRUCache<String, UniFile>` in `DownloadProvider.kt` for frequent directory lookups (Source/Anime folders).
- [x] Research: Benchmarking SAF traversal overhead with and without caching.

## Phase 2: Download Engine - Pre-allocation
- [x] Modify `Downloader.kt` to use `outChannel.setLength(totalSize)` at download start.
- [x] Implement immediate "Disk Full" check before chunking begins.

## Phase 3: Player - Thermal-Aware Shaders
- [x] **Manual FPS Limit**: Add slider for user-defined FPS cap (60/90/120) to save performance on 120Hz displays.
- [x] **Adaptive Scaling (Optional)**: Implement "Smart Anime4K" that monitors frame drops and downgrades quality automatically if enabled by user.
- [x] **Performance Intelligence (Thermal-Aware)**: Automatically downgrade shaders when OS reports thermal throttling (API 29+) to prevent overheating.

## Phase 4: Baseline Profiles
- [ ] Generate Baseline Profiles using Jetpack Macrobenchmark for:
    - [ ] Library scroll (Precision.INEXACT path).
    - [ ] PlayerActivity cold launch.
    - [ ] Extension repository indexing.