# Library FAQ

Frequently asked questions regarding library management, updates, categories, and cover art.

---

## Why is Global Update skipping entries?

By default, **AniZen** skips background updates for entries that meet any of the following criteria:

- Has unwatched episodes or unread chapters.
- Has not been started yet.
- Marked with a **Completed** status.
- Is not expected to have new release updates yet.

This intelligent skipping strategy reduces unnecessary server requests and prevents extension sources from implementing anti-bot rate limits.

> [!TIP]
> You can control which categories are included in global updates by navigating to **More → Settings → Library → Global update → Categories**.

---

## Why aren't background library updates running?

Certain Android device skins (such as MIUI, ColorOS, or OneUI) apply aggressive background process limits that close background tasks.

To ensure reliable background updates:

1. Navigate to **More → Settings → Advanced**.
2. Tap **Disable battery optimization**.
3. Allow AniZen to run unrestricted in your system settings.

> [!NOTE]
> If issues persist, visit [Don't Kill My App](https://dontkillmyapp.com/) for device-specific background permission guides.

---

## How do I display download badges on library entries?

To show badge counts for downloaded episodes directly on entry cards:

1. Open the **Library** tab.
2. Tap the **Filter** icon in the upper toolbar.
3. Select the **Display** tab.
4. Enable **Download badges** under the badge options.

---

## How can I sync my library between multiple devices?

Direct multi-device syncing is not supported. To transfer your library database, watch history, and category setup to a new device:

1. On the source device, go to **More → Settings → Backup and restore → Create backup**.
2. Transfer the `.tachibk` file to your target device.
3. On the target device, navigate to **More → Settings → Backup and restore → Restore backup** and select the file.

---

## Why are some cover thumbnails blank or corrupted?

Blank or broken cover thumbnails usually indicate an incomplete image download due to network interruption.

To fix missing covers:

1. Navigate to **More → Settings → Advanced**.
2. Tap **Refresh library covers**.
3. Re-open your library to reload fresh cover artwork.

---

## How do I pause watching history?

To temporarily prevent watched episodes from being saved to your history:

1. Open **More**.
2. Toggle **Incognito mode** to **ON**.
3. A notification icon will confirm that watch history recording is paused.
