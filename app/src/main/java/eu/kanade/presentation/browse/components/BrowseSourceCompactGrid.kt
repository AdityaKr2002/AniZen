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
import eu.kanade.presentation.library.components.AnimeCompactGridItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceCompactGrid(
    animeList: LazyPagingItems<Anime>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime, Int) -> Unit,
    onAnimeLongClick: (Anime, Int) -> Unit,
    selection: List<Anime>,
    favoriteIds: ImmutableSet<Long>,
    onBatchIncrement: (Int) -> Unit = {},
    showTitle: Boolean = true,
    usePanorama: Boolean? = null,
) {
    val selectionIds = remember(selection) { selection.map { it.id }.toSet() }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
    ) {
        item(key = "browse-grid-compact-load-prepend", span = { GridItemSpan(maxLineSpan) }) {
            if (animeList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = animeList.itemCount,
            key = { index -> "source-compact-grid-${animeList.peek(index)?.value?.id ?: "placeholder"}-$index" },
            contentType = { index -> if (animeList.peek(index) != null) "anime" else "placeholder" },
        ) { index ->
            val anime = animeList[index] ?: return@items
            onBatchIncrement(index)

            val currentOnAnimeClick = remember(onAnimeClick, anime, index) { 
                { onAnimeClick(anime, index) } 
            }
            val currentOnAnimeLongClick = remember(onAnimeLongClick, anime, index) { 
                { onAnimeLongClick(anime, index) } 
            }

            BrowseSourceCompactGridItem(
                anime = anime,
                isFavorite = anime.id in favoriteIds,
                isSelected = anime.id in selectionIds,
                onClick = currentOnAnimeClick,
                onLongClick = currentOnAnimeLongClick,
                showTitle = showTitle,
                usePanorama = usePanorama,
            )
        }

        item(key = "browse-grid-compact-load-append", span = { GridItemSpan(maxLineSpan) }) {
            if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
internal fun BrowseSourceCompactGridItem(
    anime: Anime,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    showTitle: Boolean = true,
    usePanorama: Boolean? = null,
) {
    AnimeCompactGridItem(
        title = anime.title.takeIf { showTitle },
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
