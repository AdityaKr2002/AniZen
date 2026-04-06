# Engineering Lessons Learnt

## 2026-04-04: Extension Settings & Quality Sorting
- **Root Cause Category**: Logic Error / Assumption Failure
- **Issue**: Extension settings (720p/Dub) were being ignored even after mirroring Aniyomi logic.
- **RCA**: 
    1. **Shadowing**: The app was wrapping sources in `EnhancedHttpSource` which didn't delegate `preferences` or `sort` calls. (Fixed)
    2. **API Mismatch**: `sortVideos` in the base class wasn't calling the deprecated `sort` method used by most extensions. (Fixed)
    3. **Race Condition (Current P0)**: `HosterLoader` resolves videos in parallel. If an extension reorders a list (e.g., puts 720p first) but doesn't set the `preferred` flag, `HosterLoader` currently picks whichever video finishes loading first. If 1080p loads faster than 720p, it wins despite being second in the list.
- **Rule**: `HosterLoader` must strictly respect the extension's list order. If no video is marked `preferred`, it must wait for the first video in the list to resolve before falling back to others.
