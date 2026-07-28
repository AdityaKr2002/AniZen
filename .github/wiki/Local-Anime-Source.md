# Local anime source guide

Play video files stored directly on your device storage with automatic metadata indexing and external subtitle support.

---

## Directory structure

AniZen reads local media files from the dedicated local storage directory.

1. Create a folder named `Anizen/local/` on your primary internal device storage.
2. Store each series inside its own subfolder:

```
Anizen/
└── local/
    ├── Frieren/
    │   ├── Episode 01.mp4
    │   ├── Episode 02.mkv
    │   └── cover.jpg
    └── Jujutsu Kaisen/
        ├── S01E01.mp4
        └── S01E02.mp4
```

---

## File naming conventions

To ensure accurate episode ordering:

- Use standard episode number patterns in filenames: `Episode 01`, `E01`, `S01E01`, or `[01]`.
- Supported video formats: `.mp4`, `.mkv`, `.webm`, `.avi`, `.ts`.

---

## External subtitles & cover artwork

- **Cover Image**: Add a `cover.jpg` or `cover.png` inside the series folder to display custom cover art in your Library.
- **Side-Loaded Subtitles**: Place `.ass`, `.srt`, or `.vtt` subtitle files in the same folder with matching filenames (e.g., `Episode 01.ass` alongside `Episode 01.mp4`). AniZen will automatically bind and load the external subtitle track during playback.
