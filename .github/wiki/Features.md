# ✨ Features

AniZen packs an extensive set of features across its player, downloader, UI, and analytics systems.

---

## 🎬 Media Player & Video Engine

### Neural Upscaling (Anime4K)
- **Anime4K Shaders** built-in with quality levels: `Fast (S)`, `Balanced (M)`, `High (L)`
- Multiple processing modes: `A`, `B`, `C`, `A+`, `B+`, `C+` for different content types
- **Adaptive Shader Scaling** — automatically downgrades quality on frame-drop detection
- **Dynamic MediaCodec Switching** — auto-fallback to software decoding if filters exceed hardware limits

> See the full [Anime4K Guide](Anime4K-Guide) for mode explanations and performance tuning.

### Playback & Motion
- **Motion Interpolation** — temporal frame generation with `oversample`, `mitchell`, `catmull-rom` to match display refresh (up to 120/144 Hz)
- **High-Quality Scaling** — `ewa_lanczossharp` Jinc-based scaler for sharp anime lines
- **AniSkip Integration** — skippable intros with a Netflix-style skip button
- **Filler Episode Skipping** — auto-skips filler episodes from tracker data
- **Default Stream Memory** — remembers preferred hoster & quality per anime
- **Custom Aspect Ratios** — define precise values with a dedicated sheet

### Gestures & Controls
- **Pinch-to-Zoom** — up to 3× scale with adjustment sheet
- **Long-Press 2× Speed** — jitter-free with animated release
- **Horizontal Slide Speed** — smooth horizontal swipe speed adjustment
- **Volume Boosting** — up to 200% with pitch correction
- **Sleep Timer** — schedule playback stop
- **Native PiP** — background playback with custom controls

### Subtitles & Config
- **On-Demand Subtitles** — load external `.srt`/`.ass`/`.vtt` files mid-session via URI
- **Advanced Config Editor** — in-app editor for `mpv.conf` and `input.conf`
- **Custom Script Runner** — load and run custom MPV Lua scripts
- **Custom Player Layout** — reorder and configure all action buttons

### Filters (MPVFX Suite)
Card-based filter interface with:
- Debanding
- Brightness / Saturation / Contrast / Gamma / Hue / Sharpen
- Integrated Anime4K controls
- Presets: **Vivid Anime**, **Cinema**, **Vintage**

---

## 📥 Resilient Downloader & Storage

| Feature | Detail |
|:---|:---|
| **1DM-Style Engine** | Multi-threaded chunked downloads via byte-range splitting |
| **Part-File Recovery** | Per-part size verification, 5× retry with exponential backoff |
| **External Downloader** | Handoff to 1DM/ADM with headers, filenames, and directories |
| **Native HLS** | Multi-threaded segment downloader with on-the-fly AES-128 decryption |
| **Native DASH** | FFmpeg-based muxing with duration-based progress estimation |
| **Stream Pre-fetching** | Background URL resolution for queued downloads |
| **Storage Protection** | 200 MB safety buffer check before every download |
| **Soft Subtitles** | Auto-downloads VTT/ASS/SRT tracks alongside video |
| **Atomic Assembly** | Sandbox `_tmp` folder renames to prevent partial-download clutter |
| **Preload Next Episode** | Pre-resolves stream links with network-aware throttling |

---

## 📰 Feed & Personalization

- **Category-Styled Feeds** — saved searches & popular content in draggable category rows
- **Saved Search Feeds** — pin keyword/filter combos as auto-updating feed rows
- **Unified Feed Tab** — view multiple sources simultaneously
- **Custom Cover Art** — set covers from local files or web URLs
- **Auto Theme Color** — entry view colors derived from cover art
- **Dynamic Player Theme** — player UI colors from active cover art
- **22 Color Palettes** — full custom theming
- **Panorama Cover** — wide-cover full display
- **Library Folders** — collapsible sub-folders inside library categories
- **UI Container Styles** — card layouts configurable per-tab
- **Haze Glassmorphism** — toggleable glass-blur for nav bars

---

## 📊 Statistics & Maintenance

- **Watch Statistics** — weekly heatmaps, genre affinity, status breakdowns, 30-day activity logs, top titles, preferred viewing times
- **Infrastructure Metrics** — throughput distribution, latency matrices, topology breakdowns
- **Extension Health Monitor** — live latency, node status, connection metrics
- **AI Diagnostics Assistant** — conversational troubleshooter using exception traces & library context
- **Unified Rating Distribution** — combines local + tracker scores into unified score distributions
- **Extension Repo Mapping** — resolves the GitHub owner/repo for every installed extension
- **Diagnostics Export** — one-tap export of health stats, resolve logs, and errors to clipboard
- **Adaptive Navigation** — suggests layout presets (Default, Minimal, Power) based on network & time of day
