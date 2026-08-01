# Troubleshooting guide

Diagnose playback errors, extension connectivity issues, and system issues in **AniZen**.

---

## Common issues & solutions

### 1. Extension source loading errors or 403 Forbidden

- **Cause**: Cloudflare protection or anti-bot verification page triggered on the target site.
- **Solution**: Open the extension source in **Browse**, tap the **WebView** icon in the toolbar, and complete the verification challenge.

### 2. Video stuttering or high battery usage

- **Cause**: Heavy GLSL shader processing (Anime4K) or Jinc scaling (`ewa_lanczossharp`).
- **Solution**: Navigate to **Settings → Player → Anime4K** and lower the quality setting to **Balanced (M)** or **Fast (S)**. Enable **Adaptive Shader Scaling**.

### 3. Subtitles not displaying properly

- **Cause**: Missing subtitle track fonts or unsupported container soft-sub formats.
- **Solution**: Tap the subtitle icon during playback and manually select the desired track, or use **Load external subtitle...** to import an external `.srt` or `.ass` file.

### 4. Downloads failing at 0 MB

- **Cause**: Expired stream token or unresolved HLS variant master playlist.
- **Solution**: Ensure you are running the latest version of AniZen. Native HLS variant token resolution handles master playlist segment resolution dynamically.

---

## System diagnostics & log export

AniZen includes built-in diagnostic tools to help investigate technical issues:

### Extension health monitor

1. Navigate to **More → Settings → Diagnostics**.
2. Tap **Check Extension Health**.
3. View real-time reports detailing extension latency, online node status, and response metrics.

### Exporting logcat diagnostics

If submitting a bug report on GitHub:

1. Go to **More → Settings → Diagnostics**.
2. Tap **Export Logs**.
3. The formatted diagnostic report (including device specs, active player filters, and recent stack traces) will be copied to your clipboard.
4. Paste the log contents into your [GitHub Issue report](https://github.com/salmanbappi/AniZen/issues).
