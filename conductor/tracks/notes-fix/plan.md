# Plan: Fix Note Saving and Enhance UI

## Objective
Fix the issue where editing a note and immediately exiting fails to save the note. Additionally, enhance the `AnimeNotesScreen` UI by displaying the anime's description below the note editor, separated by a line, to provide context while writing.

## Key Files & Context
- `app/src/main/java/eu/kanade/tachiyomi/ui/anime/notes/AnimeNotesScreen.kt`: The screen model where the save logic is executed.
- `app/src/main/java/eu/kanade/presentation/anime/components/AnimeNotesTextArea.kt`: The UI component for editing notes.

## Implementation Steps

### Phase 1: Fix Saving Bug
- [x] **Task 1: Update Coroutine Scope**
    - Modify `updateNotes` in `AnimeNotesScreen.Model` to use `tachiyomi.core.common.util.lang.launchIO` instead of `screenModelScope.launchNonCancellable`. 
    - This ensures that when the user presses "Back" and `onDispose` triggers the save, the coroutine is not cancelled by the screen model's disposal.

### Phase 2: Enhance UI with Description
- [x] **Task 1: Update `AnimeNotesTextArea.kt`**
    - In `AnimeNotesTextArea.kt`, add a `HorizontalDivider` below the `Row` that contains the formatting toolbar.
    - Below the divider, add a scrollable container (e.g., using `verticalScroll`) with a `Text` or `AnimeSummary` displaying `state.anime.description`.
    - Adjust the layout weights (e.g., using `Modifier.weight(1f)` for both the editor and description) to ensure both are comfortably visible on the screen.

## Verification & Testing
- Write a note and immediately press the back button; verify that the note is successfully saved and persists when reopening the screen.
- Verify that the anime description is visible below the note editor and is separated by a clear horizontal line.
- Verify that both the editor and the description area are scrollable if their content exceeds the available space.