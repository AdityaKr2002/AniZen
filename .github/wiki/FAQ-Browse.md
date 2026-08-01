# Browse & Extensions FAQ

Frequently asked questions about extensions, repository setup, Cloudflare verification, and local media sources.

---

## How do I add extension repositories?

AniZen uses modular extension repositories for content indexing.

To add a repository:

1. Open **AniZen** and navigate to **Browse → Extension Repositories**.
2. Tap **Add repository**.
3. Paste the repository URL provided by the repository maintainer.
4. Tap **Add**. Installed extensions will now appear in the **Extensions** tab under **Browse**.

---

## What should I do if Cloudflare protection blocks a source?

Some media hosts protect their endpoints with Cloudflare verification pages.

To pass Cloudflare verification:

1. Open the affected source in **Browse**.
2. Tap the **WebView** icon in the upper right toolbar.
3. Complete the captcha or verification challenge inside the WebView browser.
4. Once the site loads correctly, return to AniZen. The Cloudflare session token will be cached automatically.

---

## Can I play local video files stored on my device?

Yes. AniZen includes a local anime source module.

To configure local video sources:

1. Create an `Anizen/local/` directory in your device storage.
2. Structure your video files inside folders named after the series title:
   ```
   Anizen/local/
   └── Series Name/
       ├── Episode 01.mp4
       └── Episode 02.mkv
   ```
3. Open **Browse → Anime Sources** and tap **Local source** to browse and play your local files.

---

## Why does global search take longer on certain sources?

Global search queries all active extensions concurrently. If an extension points to a slow host or experiences network latency, its results may take longer to return.

> [!TIP]
> You can disable slow or unused extensions in **Browse → Extensions** to accelerate global search response times.
