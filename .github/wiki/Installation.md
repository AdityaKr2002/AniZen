# 🚀 Installation

Get AniZen running on your Android device in minutes.

---

## 📋 Requirements

| Requirement | Detail |
|:---|:---|
| **Android Version** | Android 8.0 (API 26) or higher |
| **Architecture** | `arm64-v8a` (most modern Android phones) |
| **Storage** | ~120 MB for the app, plus space for downloads |
| **Internet** | Required for streaming; optional for local playback |

---

## 📥 Download

1. Navigate to the [**Releases**](https://github.com/salmanbappi/AniZen/releases) tab.
2. Expand the latest release and download `AniZen-arm64-v8a.apk`.
3. You can also grab automated preview builds from [**GitHub Actions**](https://github.com/salmanbappi/AniZen/actions/workflows/preview.yml) — these are unstable but contain the latest changes.

---

## 🔧 Installing the APK

1. Open your device **Settings → Security** (or **Privacy**).
2. Enable **"Install from Unknown Sources"** (or for Android 8+, grant this permission to your file manager/browser when prompted).
3. Open the downloaded `.apk` file and tap **Install**.
4. AniZen installs under the package ID `app.anizen` — it **runs side-by-side** with official Anikku with no conflicts.

---

## 🔄 Updating

- AniZen has a built-in update checker. A banner will appear when a new version is available.
- You can also manually check: **Settings → About → Check for Updates**.
- For beta/preview updates, follow the [Actions tab](https://github.com/salmanbappi/AniZen/actions).

---

## 🗑️ Uninstalling

Standard Android uninstall from **Settings → Apps → AniZen**. Your library data stored in the `app.anizen` data directory will be removed. Download files in external storage are **not** removed automatically.

---

## 🏗️ Building from Source

For developers who want to build locally:

```bash
git clone https://github.com/salmanbappi/AniZen.git
cd AniZen
./gradlew assembleDebug
```

See [Architecture](Architecture) for the full module breakdown and [Contributing](Contributing) for setup requirements.

---

> ⚠️ **Sideloading Warning:** Only install APKs from the official [salmanbappi/AniZen](https://github.com/salmanbappi/AniZen/releases) releases page. Third-party mirrors may be modified.
