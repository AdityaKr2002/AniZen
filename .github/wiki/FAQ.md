# Frequently Asked Questions

Answers to common questions about AniZen's library, player, downloads, and storage.

---

## General

### Is AniZen free and open-source?
Yes, AniZen is open-source software licensed under the **Apache-2.0 License**.

### Does AniZen replace Aniyomi or Anikku?
No. AniZen uses the package identifier `app.anizen`. It installs as a separate application on your device and operates independently without affecting your existing Aniyomi or Anikku installations.

### How do I import my backup from Aniyomi?
1. Open **Aniyomi** and navigate to **Settings → Backup and restore → Create backup**.
2. Save the `.tachibk` file to your storage.
3. Open **AniZen** and navigate to **Settings → Backup and restore → Restore backup**.
4. Select your `.tachibk` file to restore your library and history.

---

## Media Player

### Why is the player lagging during playback?
Playback stuttering usually happens when hardware decoding is overloaded by active video filters or high Anime4K upscaling presets.

To resolve playback lag:
- Lower your Anime4K quality setting to **Balanced (M)** or **Fast (S)**.
- Disable **High-quality scaling** (`ewa_lanczossharp`).
- Reset video adjustment sliders back to `0` to re-enable pure hardware decoding.

### How do I add custom external subtitles?
While playing a video:
1. Tap the **Subtitles** icon on the player overlay.
2. Select **Load external subtitle...**
3. Pick your `.srt`, `.ass`, or `.vtt` file using the system file picker.

---

## Downloads & Storage

### Where are downloaded videos stored?
Downloaded files are saved in `AniZen/downloads/` within your primary device storage by default. You can customize the download destination under **Settings → Downloads → Download location**.

### Can I use an external download manager?
Yes. Navigate to **Settings → Downloads → External downloader** and enable external handoff. AniZen will automatically prompt external download managers like **1DM** or **ADM** when initiating downloads.
