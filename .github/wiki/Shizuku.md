# Shizuku guide

Use Shizuku for silent, background extension updates without system package installer prompts.

---

## What is Shizuku?

**Shizuku** is an open-source tool that allows applications to use system-level APIs with elevated permissions (via ADB or Wireless Debugging).

By connecting AniZen to Shizuku:

- Extension updates install silently in the background.
- You avoid repeated "Install from Unknown Sources" system prompts for every extension package.

---

## Enabling Shizuku in AniZen

1. Download and install **Shizuku** from Google Play or F-Droid.
2. Start the Shizuku service on your device using Wireless Debugging or an ADB command.
3. Open **AniZen** and navigate to **More → Settings → Advanced**.
4. Select **Installer** and tap **Shizuku**.
5. When prompted by Shizuku, grant AniZen permission to access the service.

> [!NOTE]
> Shizuku integration requires Android 8.0 or higher. Wireless debugging setup requires Android 11 or higher.
