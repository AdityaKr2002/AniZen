# 🧠 Anime4K Guide

**Anime4K** is a set of open-source, real-time GLSL upscaling and denoising shaders for anime. AniZen integrates them directly into the MPV player pipeline.

---

## 🎯 Choosing the Right Mode

Anime4K is **not** a one-size-fits-all filter. Each mode is designed for a specific content type.

### Mode A — Faithful Upscaling
> Best for: **1080p / 720p modern anime (2010+)**

- **Goal:** Restore original lines as accurately as possible
- **Effect:** Sharpens edges and reconstructs missing details without altering the art style
- Ideal when the source is already decent quality but looks soft on high-DPI screens

### Mode B — Perceptual Deblur
> Best for: **720p / older anime (90s–00s)**

- **Goal:** Treat the image as blurred and reverse it
- **Effect:** Aggressively sharpens lines — can create thinner lines but makes the image pop significantly
- Choose this for content that feels "out of focus" or "mushy"

### Mode C — Denoise & Restore
> Best for: **480p / compressed streams / DVD rips**

- **Goal:** Clean up the image before upscaling
- **Effect:** Applies a de-blocking pass to smooth MPEG/JPEG artifacts, then upscales
- Prevents the "upscaling noise" effect on low-quality sources

### The "Plus" Variants (A+, B+, C+)

These run the primary reconstruction shader **twice**.

| Aspect | Detail |
|:---|:---|
| **Pros** | Even sharper image, higher perceived resolution |
| **Cons** | Doubles GPU load |
| **Recommended for** | Snapdragon 8 Gen 1/2/3, Dimensity 9000+ only |

---

## ⚡ Quality Profiles

The **Quality** setting controls the CNN (neural network) complexity.

| Profile | CNN Size | GPU Load | Best For |
|:---|:---|:---|:---|
| **Fast (S)** | Small | Low | Mid-range (SD 7xx, Exynos 1xxx) |
| **Balanced (M)** | Medium | Medium | High-end older devices (SD 865/870/888) |
| **High (L)** | Large | High | Flagship devices (SD 8 Gen 1+) |

---

## 🔄 Adaptive Shader Scaling

AniZen monitors frame drop rates in real time. If significant drops or delayed frames are detected:

1. Quality automatically steps down (e.g., `High` → `Balanced`)
2. Playback stays smooth without manual intervention
3. You can override this in **Settings → Player → Anime4K → Adaptive Scaling**

---

## 🐢 Troubleshooting Lag

If video stutters or audio desyncs while Anime4K is active:

1. **Lower the Quality first** — switch `High (L)` → `Balanced (M)` → `Fast (S)`
2. **Avoid "Plus" modes** — `A+/B+/C+` doubles GPU load; use standard `A/B/C`
3. **Check resolution** — upscaling 1080p → 4K is significantly harder than 480p → 1080p; disable shaders for 4K content
4. **Disable `ewa_lanczossharp`** — combining Jinc scaling + Anime4K is very GPU-intensive

---

## ⚠️ Compatibility Notes

| Constraint | Detail |
|:---|:---|
| `gpu-next` renderer | **Incompatible** with Anime4K — AniZen auto-switches to `gpu` |
| Hardware decoder | Anime4K requires `mediacodec-copy` (Copy Mode) — AniZen handles this automatically |
| 4K content | Shader upscaling on 4K sources is rarely beneficial and very expensive; recommend disabling |

---

## 📖 Further Reading

- [Anime4K GitHub](https://github.com/bloc97/Anime4K) — original shader repository with technical papers
- [mpv GLSL hooks](https://mpv.io/manual/master/#options-glsl-shader) — how shaders are loaded in mpv
- [Player Guide](Player-Guide) — broader player optimization settings
