# ⚙️ Player Guide

Deep-dive into AniZen's MPV-based player: its optimizations, smart logic, and how to get the best out of every setting.

---

## ⚡ The Zero-Lag Optimization Suite

### Performance-First Video Sync

AniZen's `video-sync` defaults to **`audio`** — not `display-resample` like most apps.

| Mode | Behavior | Use Case |
|:---|:---|:---|
| `audio` (default) | Smooth, CPU-light sync | General playback |
| `display-resample` | Frame-perfect but heavy | **Auto-enabled** when Interpolation is ON |

> Standard `display-resample` causes stuttering when skipping or playing high-bitrate files. AniZen uses `audio` sync by default and only switches when technically required.

### Smart Font Caching

- Original behavior: copies all fonts from storage **on every launch**
- AniZen: checks cache first → **near-instant** player open even with hundreds of custom fonts

### Low-Overhead Technical Stats (Page 6)

High-frequency properties (Real-time FPS, Mistime, Dropped Frames) now use an **efficient polling system**.

- Stats UI closed → zero background CPU drain
- Stats UI open → data fetched only when visible

---

## 🏗️ High-Quality Scaling (`ewa_lanczossharp`)

AniZen replaces standard bilinear scaling with a **Jinc-based scaler** for anime lines.

**Why?** Standard scaling makes diagonals look jagged. `ewa_lanczossharp` preserves edge sharpness.

> ⚠️ **GPU-Next Warning:** Do **NOT** use `gpu-next` renderer with Anime4K. AniZen automatically switches to the standard `gpu` renderer when Anime4K is enabled.

> ⚠️ **Performance Note:** Approximately 3× the GPU load of standard playback. Disable on lower-end devices or if your phone heats up.

---

## 🎞️ Interpolation (Smooth Motion)

Anime is produced at 24 fps. Screens run at 60 Hz, 90 Hz, or 120 Hz. The mismatch causes **judder** during camera pans.

Interpolation generates intermediate frames to blend motion for a fluid look.

**Trade-offs:**

| Cost | Detail |
|:---|:---|
| Input Lag | Adds delay due to intermediate frame calculation |
| Battery | Significant GPU + CPU impact |
| Sync Mode | Forces `display-resample` automatically |

**Recommended devices:** Snapdragon 8 Gen 2 / Dimensity 9200 and above.

---

## 🎨 Video Filters & Copy Mode

### Intelligent Copy Mode Switching

Android's hardware decoder can't apply post-processing filters in its default mode. To apply any filter (Sharpen, Saturation, etc.) AniZen must enable `mediacodec-copy`.

| State | Decoder Mode | Power Use |
|:---|:---|:---|
| All filters OFF | Hardware (efficient) | Low |
| Any filter ON | `mediacodec-copy` | Higher |

> 💡 **Battery Tip:** For long trips, ensure all Video Filter sliders are at **0** to keep the ultra-efficient hardware path active.

### Filter Presets

| Preset | Settings | Vibe |
|:---|:---|:---|
| **Vivid Anime** | Contrast +5, Saturation +20, Sharpen +15 | Punchy colors, crisp lines |
| **Cinema** | Brightness -5, Contrast +15, Saturation -10 | Dark, moody cinematic feel |
| **Vintage** | Saturation -30, Gamma -10, Hue -5 | Faded, old-school aesthetic |

---

## 🎮 Gestures Reference

| Gesture | Action |
|:---|:---|
| **Horizontal swipe** | Seek forward / backward |
| **Vertical swipe (left)** | Adjust brightness |
| **Vertical swipe (right)** | Adjust volume |
| **Long-press** | Activate 2× speed (release to return) |
| **Pinch** | Zoom in / out (up to 3×) |
| **Double-tap left/right** | Skip ±10 seconds |

---

## 📝 MPV Config & Scripts

Access the in-app editor at **Settings → Player → Advanced Config**.

- Edit `mpv.conf` for global playback settings
- Edit `input.conf` for custom key bindings
- Drop custom `.lua` scripts into the scripts folder for automation

> Useful resources: [mpv documentation](https://mpv.io/manual/master/), [mpv-android wiki](https://github.com/mpv-android/mpv-android/wiki)

---

## 🐛 Troubleshooting Playback Issues

| Symptom | Fix |
|:---|:---|
| Video stutters / audio desync | Switch to a lower Anime4K quality level |
| Phone overheats | Disable `ewa_lanczossharp` scaling and/or Anime4K |
| Subtitles not appearing | Use **Load External Subtitle** to pick the file manually |
| Black screen on start | Try forcing software decoding: `Settings → Player → Decoder → Software` |
| 4K content lags | Disable all shaders; hardware decoding alone is often sufficient |
