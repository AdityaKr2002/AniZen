// File: LibraryCoverOnlyGrid.kt
package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibraryDisplayItem
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.library.model.LibraryAnime

/**
 * A grid layout that displays only the cover image for each anime item.
 * This composable forwards to [LibraryCompactGrid] with the title hidden.
 */
@Composable
fun LibraryCoverOnlyGrid(
    items: ImmutableList<LibraryDisplayItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: ImmutableList<LibraryAnime>,
    onClick: (LibraryAnime) -> Unit,
    onLongClick: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    usePanorama: Boolean? = null,
    onFolderClick: ((LibraryDisplayItem.Folder) -> Unit)? = null,
    onFolderLongClick: ((LibraryDisplayItem.Folder) -> Unit)? = null,
) {
    // Reuse the existing CompactGrid implementation but hide the title.
    LibraryCompactGrid(
        items = items,
        showTitle = false,
        columns = columns,
        contentPadding = contentPadding,
        selection = selection,
        onClick = onClick,
        onLongClick = onLongClick,
        onClickContinueWatching = onClickContinueWatching,
        searchQuery = searchQuery,
        onGlobalSearchClicked = onGlobalSearchClicked,
        usePanorama = usePanorama,
        onFolderClick = onFolderClick,
        onFolderLongClick = onFolderLongClick,
    )
}
