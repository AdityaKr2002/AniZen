# Player settings

Detailed guide to internal player configurations, performance tweaks, video filters, and controls.

---

## Internal player

### Zero-lag performance suite

AniZen optimizes playback performance out of the box with the following defaults:

- **Audio Sync**: Default `video-sync` is set to `audio` to minimize CPU overhead. `display-resample` is automatically enabled only when **Motion Interpolation** is active.
- **Smart Font Caching**: Caches subtitle fonts across player sessions to ensure instantaneous player launches.
- **Low-Overhead Stats**: The technical stats overlay uses an active polling loop only when open, eliminating background CPU drain when hidden.

### High-quality scaling (`ewa_lanczossharp`)

Replaces standard bilinear scaling with a Jinc-based scaler to remove pixelated or jagged edges from anime lines.

> [!WARNING]
> `ewa_lanczossharp` requires up to 3x more GPU processing power than standard scaling. If your device experiences heating or battery drain, consider disabling this setting.

---

## Video filters & presets

Access video adjustment sliders in the player by opening the **Filters (MPVFX)** panel.

### Filter options
- **Debanding**: Smooths color gradients to eliminate color banding in dark scenes.
- **Adjustments**: Sliders for Brightness, Contrast, Saturation, Gamma, Hue, and Sharpening.

### Preset profiles

| Preset | Adjustments | Recommended Use |
|:---|:---|:---|
| **Vivid Anime** | Contrast +5, Saturation +20, Sharpen +15 | Enhances line art and color vibrance |
| **Cinema** | Brightness -5, Contrast +15, Saturation -10 | Provides a darker, moody atmosphere |
| **Vintage** | Saturation -30, Gamma -10, Hue -5 | Gives a retro, faded aesthetic |

> [!NOTE]
> Applying video filters automatically enables **Copy Mode** (`mediacodec-copy`) to allow post-processing. Setting all filter sliders back to `0` restores pure hardware decoding for maximum battery efficiency.

---

## Gestures

The internal player supports touch gestures for intuitive control:

- **Horizontal Swipe**: Seek forward or backward through the video timeline.
- **Vertical Swipe (Left)**: Adjust display brightness.
- **Vertical Swipe (Right)**: Adjust audio output volume.
- **Long-Press**: Momentarily accelerate playback speed to 2x (releases back to normal speed upon letting go).
- **Pinch-to-Zoom**: Scale video content up to 3x magnification.
- **Double-Tap**: Skip backward or forward by the configured skip duration.
