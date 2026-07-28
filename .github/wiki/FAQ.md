# ❓ FAQ

Frequently asked questions about AniZen.

---

## 🔧 General

### Is AniZen free?
Yes — completely free and open-source under the [Apache-2.0 License](https://github.com/salmanbappi/AniZen/blob/master/LICENSE).

### Will AniZen conflict with Anikku or Aniyomi?
No. AniZen uses the package ID `app.anizen`, which is unique. It installs and runs completely independently alongside other anime apps.

### Does AniZen come with built-in anime sources?
No. Like Aniyomi, AniZen uses **extensions** — community-built plugins that connect to anime sites. Extensions are installed separately.

### Where are my downloads stored?
By default, downloads go to `/storage/emulated/0/AniZen/`. You can change this in **Settings → Downloads → Download Location**.

### Can I migrate my library from Aniyomi / Anikku?
Yes — use the backup/restore feature. Export a `.tachibk` backup from the source app and import it in AniZen via **Settings → Backup → Restore Backup**.

---

## 🎬 Player

### Why does the video stutter when I enable Anime4K?
Your device's GPU may not be powerful enough for the selected quality level. Try:
1. Lower the quality: `High → Balanced → Fast`
2. Avoid `A+/B+/C+` modes (they double GPU load)
3. Disable `ewa_lanczossharp` scaling simultaneously

See the full [Anime4K Guide](Anime4K-Guide) for details.

### The screen goes black when I start playback — what do I do?
Force software decoding: **Settings → Player → Decoder → Force Software**.

### Why is audio out of sync?
If you're using Interpolation, it requires `display-resample` sync which can sometimes desync on lower-end devices. Try disabling Interpolation.

### Can I use external subtitle files?
Yes. During playback, tap the subtitle button and select **Load External Subtitle**. Supports `.srt`, `.ass`, and `.vtt` formats.

### What does "Copy Mode" mean in Video Filters?
Android's hardware decoder normally doesn't allow post-processing. "Copy Mode" (`mediacodec-copy`) forces the decoder to copy frames back to memory so filters can be applied. It's automatically enabled when any filter is active and disabled when all filters are at 0.

---

## 📥 Downloads

### My download shows 0 MB — is it broken?
This was a known issue with HLS streams. It's fixed in the latest version — update AniZen and retry.

### Can I use an external download manager like 1DM?
Yes. Go to **Settings → Downloads → External Downloader** and select your preferred app. AniZen will hand off the URL with correct headers and filename.

### A download failed partway through — will it resume?
Yes. AniZen's downloader uses per-part file verification and will automatically resume from where it left off with up to 5 retry attempts per part.

---

## 🌐 Extensions & Sources

### Where do I get extensions?
Extensions are hosted by the community. Add an extension repository URL in **Settings → Browse → Extension Repositories**.

### An extension isn't working — is it broken?
Check the extension health monitor: **Settings → Statistics → Extension Health**. It shows live latency and node status per extension. If a source is down, it's usually a temporary server-side issue.

### How do I report a broken extension?
Report it to the extension repository maintainer, not the AniZen issue tracker. AniZen only ships the app — extensions are third-party.

---

## 🔄 Updates

### How do I get the latest version?
AniZen has a built-in update checker. You'll see a notification banner when an update is available. You can also check manually at **Settings → About → Check for Updates**, or grab the latest APK from [Releases](https://github.com/salmanbappi/AniZen/releases).

### What are Preview builds?
Preview builds are automated builds from the `preview` branch. They contain the latest commits but may be unstable. Find them in [GitHub Actions](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml).

---

## 💬 More Help

Still stuck? Join the [Discord Server](https://discord.gg/J2wmZqEJnS) or open a [GitHub Issue](https://github.com/salmanbappi/AniZen/issues).
