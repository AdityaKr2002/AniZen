# Backups guide

Backups allow you to safeguard your library, watch history, category organization, and app settings.

---

## Creating a backup

To manually generate a backup file:

1. Open **AniZen** and navigate to **More → Settings → Data and storage**.
2. Tap **Create backup**.
3. Choose a secure directory in your device storage to save the `.tachibk` file.

> [!NOTE]
> Backups are saved as compressed files with the `.tachibk` extension and are fully portable across devices.

---

## What is included in a backup?

A backup includes the following data:

- **Library Titles**: All saved anime and movie entries.
- **Categories**: Custom library categories and folder structures.
- **Watch History & Progress**: Episode watch states, bookmark flags, and timestamps.
- **Tracker Connections**: Active tracking accounts and series binding IDs.
- **App & Source Settings**: Player configuration, theme choices, and source settings.
- **Extension List**: Identifiers for installed extensions.

### What is NOT included?

- Video downloads or local media files.
- Custom cover images (these are re-downloaded upon restoration).
- Search history or transient cache files.

---

## Restoring a backup

To restore a previously created backup:

1. Copy the `.tachibk` file to your new device.
2. Ensure you have installed your preferred extension repositories under **Browse → Extension Repositories**.
3. Log into your tracking services under **More → Settings → Tracking**.
4. Go to **More → Settings → Data and storage**.
5. Tap **Restore backup** and select your `.tachibk` file.

---

## Automatic backups

We strongly recommend enabling automatic backups to prevent library loss.

### Setting up automatic backups

1. Navigate to **More → Settings → Data and storage**.
2. Select **Automatic backups** and choose a backup frequency (e.g., *Daily* or *Weekly*).
3. Set your target backup storage folder.

> [!TIP]
> You can sync your automatic backup folder to cloud services like Google Drive or Nextcloud using third-party sync utilities such as **Autosync for Google Drive** or **FolderSync**.
