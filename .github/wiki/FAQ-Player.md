# Player FAQ

Frequently asked questions regarding MPV playback, Anime4K upscaling, audio/video sync, and subtitles.

---

## Why does video stutter when Anime4K is active?

Anime4K neural upscaling requires significant GPU shader performance.

If stuttering occurs:

1. Lower the preset quality in **Settings → Player → Anime4K** from **High (L)** to **Balanced (M)** or **Fast (S)**.
2. Avoid using **Plus (+)** variants (such as `A+`, `B+`, `C+`) on mid-range devices.
3. Turn on **Adaptive Shader Scaling**. When AniZen detects dropped frames during playback, it automatically steps down shader complexity to maintain smooth rendering.

---

## What is the difference between `audio` and `display-resample` video sync?

- **`audio` (AniZen Default)**: Syncs video frames to the audio clock. Extremely lightweight, avoiding CPU micro-stutter during skips or high-bitrate playback.
- **`display-resample`**: Resamples audio to match display refresh rate. Required for **Motion Interpolation**, but increases CPU and GPU load.

AniZen uses `audio` sync by default and dynamically switches to `display-resample` only when Interpolation is turned ON.

---

## What is Copy Mode (`mediacodec-copy`)?

Android's default hardware decoder processes video inside a closed hardware pipeline. To apply post-processing filters (such as Sharpen, Saturation, or Debanding), video frames must be copied back to system memory.

- **Filters OFF**: Pure hardware decoding active (maximum battery efficiency).
- **Filters ON**: `mediacodec-copy` mode engaged automatically to render custom GLSL and MPVFX filters.

---

## How do I load custom subtitle files mid-playback?

To inject local subtitle files during playback:

1. Tap the player screen to display controls.
2. Select the **Subtitles** icon.
3. Tap **Load external subtitle...**
4. Select your `.srt`, `.ass`, or `.vtt` file from your device storage using the system file picker.
