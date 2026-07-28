# Features

Overview of playback capabilities, downloader resilience, UI customization, and monitoring tools in **AniZen**.

---

## Video player & engine

AniZen features a heavily optimized media player built on `mpv-android`.

### Upscaling & Shaders
- **Anime4K Neural Shaders**: Built-in real-time upscaling with processing modes `A`, `B`, `C`, `A+`, `B+`, and `C+`.
- **Adaptive Shader Scaling**: Dynamically adjusts upscaling quality if frame drops are detected during heavy scenes.
- **Dynamic MediaCodec Switching**: Automatically falls back to software decoding if active GLSL filters exceed hardware limits.

### Smooth Motion & Playback
- **Motion Interpolation**: Generates intermediate frames matching device refresh rates (60Hz, 90Hz, 120Hz, 144Hz).
- **AniSkip Integration**: Displays a one-tap skip button for opening and ending themes powered by AniSkip.
- **Filler Episode Skipping**: Identifies and skips filler episodes based on online tracking data.
- **Default Stream Memory**: Remembers preferred hoster and quality settings per series for seamless playback.

---

## Downloader & storage engine

The download subsystem is designed for reliability over poor network conditions:

- **Chunked Downloading**: Multi-threaded downloader utilizing HTTP byte-range requests.
- **Part-File Recovery**: Validates chunk sizes automatically and retries failed segments up to 5 times with backoff.
- **Native HLS & DASH**: Muxes video, audio, and AES-128 encrypted streams cleanly into local MP4 containers.
- **External Downloader Handoff**: Passes download tasks directly to external apps like **1DM** or **ADM**, preserving headers and cookies.
- **Pre-Flight Storage Protection**: Verifies available device storage before starting downloads to prevent corruption.

---

## Customization & UI

- **Category Feeds**: Personalize your home screen by pinning saved searches and categories as custom feed rows.
- **Theme Engine**: Includes 22 curated color palettes alongside dynamic color extraction from cover art.
- **Glassmorphism Styling**: Optional Haze blur effects for top app bars and bottom navigation.
- **Custom Player Layout**: Reorder action buttons and player controls directly from the settings menu.

---

## Maintenance & Analytics

- **Watch Statistics**: Detailed heatmaps, genre breakdowns, viewing time trends, and rating distribution graphs.
- **Extension Health Monitor**: Reports real-time latency, node status, and response metrics for all installed extensions.
- **Diagnostics Export**: One-tap tool to format and copy error logs and extension diagnostics to your clipboard.
