# Downloads FAQ

Frequently asked questions regarding download managers, HLS stream muxing, 1DM handoff, and storage verification.

---

## How does AniZen's chunked download engine work?

AniZen includes a 1DM-style multi-threaded download engine:

- **Byte-Range Chunking**: Splits large video files into parallel streams for accelerated download speeds.
- **Part-File Recovery**: Verifies downloaded chunk byte sizes against checksums and retries corrupted chunks up to 5 times automatically.
- **Atomic Renaming**: Downloads are isolated in temporary `_tmp` folders and atomically renamed upon completion to prevent partially downloaded files from cluttering your media library.

---

## Can I delegate downloads to an external manager like 1DM or ADM?

Yes. AniZen supports seamless handoff to external download apps.

To enable external downloads:

1. Navigate to **More → Settings → Downloads**.
2. Select **External downloader**.
3. Choose your preferred app (e.g., **1DM**, **ADM**, or **Neat Download Manager**).

When initiating a download, AniZen automatically forwards the stream URL along with required HTTP headers, user-agents, and output filenames to your selected download manager.

---

## Why did my download report 0 MB or fail immediately?

Immediate 0 MB download failures typically occur when an HLS master playlist variant cannot be resolved due to expired tokens.

- **Solution**: Update AniZen to the latest release or Preview build. AniZen's native engine resolves HLS variant streams and AES-128 key tokens dynamically before starting segment fetches.

---

## What is Pre-Flight Storage Protection?

Before launching a download task, AniZen verifies available internal storage.

If available space is below **200 MB** (or less than **1.5×** the target size for FFmpeg muxing operations), the download is paused automatically with a notification warning to prevent device instability.
