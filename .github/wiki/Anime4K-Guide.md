# Anime4K guide

Understanding real-time neural upscaling modes, performance presets, and configuration options.

---

## What is Anime4K?

**Anime4K** is an open-source set of high-performance real-time upscaling and denoising GLSL shaders designed specifically for anime. AniZen integrates these shaders directly into the media player pipeline.

---

## Upscaling modes

Anime4K includes different processing algorithms tailored to specific source content qualities:

### Mode A (Faithful Upscaling)
- **Target Content**: Modern 720p or 1080p anime.
- **Description**: Reconstructs line edges faithfully without altering original artistic detail.
- **Use Case**: Best for high-quality recent releases viewed on 1440p or 4K mobile displays.

### Mode B (Perceptual Deblur)
- **Target Content**: 720p or older anime (1990s–2000s).
- **Description**: Applies perceptual sharpening to correct soft or out-of-focus source material.
- **Use Case**: Recommended for older anime titles that appear blurry.

### Mode C (Denoise & Restore)
- **Target Content**: Low-bitrate 480p streams, DVD rips, or heavily compressed videos.
- **Description**: Cleans compression artifacts and blocking noise before performing upscaling.
- **Use Case**: Best for web streams with visible JPEG/MPEG compression artifacts.

### Mode A+, B+, and C+
- Runs the reconstruction pass twice for maximum sharpness.
- **Hardware Requirement**: Snapdragon 8 Gen 1/2/3 or equivalent flagship processors.

---

## Quality profiles

Select a profile based on your device's GPU capabilities:

| Profile | Network Size | Processing Power | Recommended Hardware |
|:---|:---|:---|:---|
| **Fast (S)** | Small | Low | Mid-range SoCs (Snapdragon 7xx series) |
| **Balanced (M)** | Medium | Moderate | Upper mid-range / older flagships (Snapdragon 865/870/888) |
| **High (L)** | Large | High | Modern flagship SoCs (Snapdragon 8 Gen 1+) |

---

## Troubleshooting performance issues

If you encounter video stuttering or audio desync while using Anime4K:

1. Lower the preset quality from **High (L)** to **Balanced (M)** or **Fast (S)**.
2. Switch from a **Plus (+)** mode variant to the standard mode (e.g., Mode A+ to Mode A).
3. Disable **High-quality scaling** (`ewa_lanczossharp`) to reduce GPU pipeline load.
4. Enable **Adaptive Shader Scaling** in **Settings → Player → Anime4K** to let AniZen automatically scale down shaders during heavy scenes.

> [!NOTE]
> Anime4K shaders require the standard `gpu` renderer. AniZen automatically switches off `gpu-next` when Anime4K is enabled to prevent render pipeline crashes.
