# Player FAQ

Frequently asked questions regarding MPV playback, Anime4K upscaling, video synchronization, and subtitles.

---

## Why does video stutter when Anime4K is enabled?

Anime4K shaders place high demand on GPU execution units.

If playback stutters:

1. Reduce the preset quality in **Settings → Player → Anime4K** to **Balanced (M)** or **Fast (S)**.
2. Avoid **Plus (+)** variants (`A+`, `B+`, `C+`) on mid-range hardware.
3. Enable **Adaptive Shader Scaling** to automatically reduce shader preset complexity when frame drops occur.

---

## What is the difference between `audio` and `display-resample` video sync?

- **`audio` (AniZen Default)**: Syncs video frames to the audio clock, keeping CPU overhead low.
- **`display-resample`**: Resamples audio to match the display refresh rate. Required for **Motion Interpolation**, but increases CPU and GPU load.

AniZen uses `audio` sync by default and automatically switches to `display-resample` when Motion Interpolation is enabled.

---

## What is Copy Mode (`mediacodec-copy`)?

Standard Android hardware decoding (`mediacodec`) renders directly to a display surface. To apply post-processing filters (such as sharpening, saturation, or debanding), frames are copied to RAM for GPU processing (`mediacodec-copy`).

- **Filters Disabled**: Direct hardware decoding active for minimum power consumption.
- **Filters Enabled**: `mediacodec-copy` engaged automatically to run MPVFX filters and shaders.

---

## How do I load external subtitle files mid-playback?

To add external subtitles during video playback:

1. Tap the screen to display player controls.
2. Tap the **Subtitles** icon.
3. Select **Add external subtitles**.
4. Choose an `.srt`, `.ass`, or `.vtt` file from internal storage.

---

## How do I configure Player Settings in AniZen?

Navigate to **Settings → Player**:

- **Player Engine**: Switch between the built-in MPV engine and external players (e.g., VLC, MX Player).
- **Decoder Mode**: Select hardware (`mediacodec`), hardware copy (`mediacodec-copy`), or software decoding.
- **Skip Duration**: Set seek intervals for double-tap gestures (e.g., 5s, 10s, 30s, 85s).
- **AniSkip**: Enable automatic skipping or manual skip buttons for opening and ending sequences.
- **Auto-Play Next Episode**: Start the next episode automatically on stream completion.
- **Track Persistence**: Save audio and subtitle language preferences per title.
