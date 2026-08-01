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
- **Episode Thumbnails**: Place a `<Episode Name>-thumbnail.jpg` or `<Episode Name>-thumbnail.png` file (e.g. `Episode 01-thumbnail.jpg`) alongside the video file, or let AniZen automatically generate one from the video frame.
- **Side-Loaded Subtitles**: Place `.ass`, `.srt`, or `.vtt` subtitle files in the same folder with matching filenames (e.g., `Episode 01.ass` alongside `Episode 01.mp4`). AniZen will automatically bind and load the external subtitle track during playback.

---

## Local metadata files (`details.json` & `episodes.json`)

You can provide rich series and episode metadata directly inside your anime subfolder:

### `details.json` (Anime details)
Create `Anizen/local/<Anime Title>/details.json`:
```json
{
  "title": "Frieren: Beyond Journey's End",
  "author": "Kanehito Yamada",
  "artist": "Tsukasa Abe",
  "description": "An elf mage and her courageous fellow adventurers have defeated the Demon King...",
  "genre": ["Adventure", "Drama", "Fantasy"],
  "status": 1
}
```

### `episodes.json` (Episode titles & summaries)
Create `Anizen/local/<Anime Title>/episodes.json`:
```json
[
  {
    "episode_number": 1.0,
    "name": "The Journey's End",
    "summary": "After a 10-year quest, the Hero Party returns victorious...",
    "date_upload": "2023-09-29T00:00:00",
    "preview_url": "file:///path/to/custom-thumbnail.jpg"
  }
]
```

