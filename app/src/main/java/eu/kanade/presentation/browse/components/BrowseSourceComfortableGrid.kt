package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.AnimeComfortableGridItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceComfortableGrid(
    animeList: LazyPagingItems<StateFlow<Anime>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime, Int) -> Unit,
    onAnimeLongClick: (Anime, Int) -> Unit,
    selection: List<Anime>,
    favoriteIds: ImmutableSet<Long>,
    onBatchIncrement: (Int) -> Unit = {},
    usePanorama: Boolean? = null,
) {
    val selectionIds = remember(selection) { selection.map { it.id }.toSet() }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
    ) {
        if (animeList.loadState.prepend is LoadState.Loading) {
            item(key = "browse-grid-comfortable-load-prepend", span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = animeList.itemCount,
            key = { index -> "source-comfortable-grid-${animeList.peek(index)?.value?.id ?: "placeholder"}-$index" },
            contentType = { index -> if (animeList.peek(index) != null) "anime" else "placeholder" },
        ) { index ->
            val anime by animeList[index]?.collectAsState() ?: return@items
            onBatchIncrement(index)
            
            val currentOnAnimeClick = remember(onAnimeClick, anime, index) { 
                { onAnimeClick(anime, index) } 
            }
            val currentOnAnimeLongClick = remember(onAnimeLongClick, anime, index) { 
                { onAnimeLongClick(anime, index) } 
            }
            
            BrowseSourceComfortableGridItem(
                anime = anime,
                isFavorite = anime.id in favoriteIds,
                isSelected = anime.id in selectionIds,
                onClick = currentOnAnimeClick,
                onLongClick = currentOnAnimeLongClick,
                usePanorama = usePanorama,
            )
        }

        if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
            item(key = "browse-grid-comfortable-load-append", span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
internal fun BrowseSourceComfortableGridItem(
    anime: Anime,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    usePanorama: Boolean? = null,
) {
    AnimeComfortableGridItem(
        title = anime.title,
        coverData = remember(anime.id, isFavorite) {
            anime.asAnimeCover().copy(isAnimeFavorite = isFavorite)
        },
        coverAlpha = if (isFavorite) CommonAnimeItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = isFavorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        isSelected = isSelected,
        usePanorama = usePanorama,
    )
}
