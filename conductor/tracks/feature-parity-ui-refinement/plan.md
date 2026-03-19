# Implementation Plan: Feature Parity & UI Refinement

## Tasks

### 1. Migration Source Selection Multi-Selection
- [x] **State Management**:
    - Update `MigrateSourceScreenModel.State` with `selectedSources: Set<Long>`.
    - Add `toggleSelection(sourceId: Long)` to `MigrateSourceScreenModel`.
    - Implement `selectAll()`, `selectNone()`, `matchEnabled()`, `matchPinned()`.
- [x] **UI Implementation**:
    - Add `BulkSelectionToolbar` (icons in header) to `MigrateSourceScreen`.
    - Include selection icons: Select All, Select None, Match Enabled, Match Pinned.
    - Show checkboxes for each source item in `MigrateSourceItem`.
    - Integrate bulk selection actions in the UI.
- [x] **Logic Expansion**:
    - Update `MigrateAnimeScreen` and `MigrateAnimeScreenModel` to support multiple source IDs.

### 2. Migration Bottom Sheet & Search Fix
- [x] **Synchronization**:
    - Verified search query propagation.
    - Improved UI consistency in migration flows.

### 3. Migration Wording Refinement
- [x] **String Updates**:
    - Added `migrating` string to `i18n-sy`.
    - Replaced "Migration" with "Migrating" in active progress indicators and selection header.
    - Updated confirmation buttons to use "Migrating".

### 4. Enhanced Notes Feature
- [x] **Layout Swap**:
    - Swapped Title and "Edit Notes" in `AnimeNotesScreen` AppBar.
- [x] **Rich Text Improvements**:
    - Verified Bold, Italics, Underline, Bullet points, and Numbered list support in `AnimeNotesTextArea`.
- [x] **Character Constraint**:
    - Implemented 250-character limit that excludes whitespace from the count.
