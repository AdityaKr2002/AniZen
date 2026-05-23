package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryFolder

sealed interface LibraryDisplayItem {
    data class Anime(val libraryItem: LibraryItem) : LibraryDisplayItem
    data class Folder(val folder: LibraryFolder, val items: List<LibraryItem>) : LibraryDisplayItem
    data class Header(val name: String) : LibraryDisplayItem
}
