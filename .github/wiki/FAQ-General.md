# General FAQ

Answers to general questions about AniZen, installation, platforms, and app origins.

---

## Why isn't AniZen on the Google Play Store?

**AniZen** will not be available on the **Google Play Store**.

Third-party extension models conflict with [Google Play Developer Content Policies](https://play.google.com/about/developer-content-policy/). Providing modular source loading could lead to store takedowns, which we avoid by distributing directly via official GitHub releases.

---

## Is AniZen available for iOS or iPadOS?

No. There is no iOS or iPadOS version, and there are no plans to create one. iOS and Android require completely separate codebases, programming languages, and UI frameworks.

> [!CAUTION]
> Any application claiming to be **"AniZen for iOS"** is fake and should be treated as a security risk.

---

## What is AniZen Preview?

**AniZen Preview** is an automatically built version containing the latest commits from the `preview` branch.

- **Pros**: Access new features, player improvements, and bug fixes before official releases.
- **Cons**: Higher chance of encountering temporary bugs or crashes.

> [!TIP]
> If you run Preview builds, enable **Automatic Backups** under **More → Settings → Backup and restore** to safeguard your library database.

---

## Does AniZen conflict with Aniyomi or Anikku?

No. AniZen uses the package identifier `app.anizen`. It installs and runs side-by-side with Aniyomi (`eu.kanade.tachiyomi`) and Anikku (`app.komikku.anikku`) without sharing data or interfering with their operation.

---

## What is a fork?

A fork is an independent copy of an open-source codebase developed with unique features or architectural changes. AniZen is a specialized fork combining core foundations from Aniyomi and Anikku with deep MPV player enhancements, GLSL upscaling shaders, and dynamic theming.
