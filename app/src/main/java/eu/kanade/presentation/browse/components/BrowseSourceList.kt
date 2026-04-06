package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.AnimeListItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import kotlinx.collections.immutable.ImmutableSet
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceList(
    animeList: LazyPagingItems<Anime>,
    entries: Int,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime, Int) -> Unit,
    onAnimeLongClick: (Anime, Int) -> Unit,
    selection: List<Anime>,
    favoriteIds: ImmutableSet<Long>,
    onBatchIncrement: (Int) -> Unit = {},
    usePanorama: Boolean = false,
) {
    val selectionIds = remember(selection) { selection.map { it.id }.toSet() }
    val content: @Composable (Int) -> Unit = { containerHeight ->
        LazyColumn(
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "browse-list-load-prepend") {
                if (animeList.loadState.prepend is LoadState.Loading) {
                    BrowseSourceLoadingItem()
                }
            }

            items(
                count = animeList.itemCount,
                key = { index -> "source-list-${animeList.peek(index)?.id ?: "placeholder"}-$index" },
                contentType = { index -> if (animeList.peek(index) != null) "anime" else "placeholder" },
            ) { index ->
                val anime = animeList[index] ?: return@items
                onBatchIncrement(index)
                BrowseSourceListItem(
                    anime = anime,
                    isFavorite = anime.id in favoriteIds,
                    isSelected = anime.id in selectionIds,
                    onClick = { onAnimeClick(anime, index) },
                    onLongClick = { onAnimeLongClick(anime, index) },
                    entries = entries,                    containerHeight = containerHeight,
                    usePanorama = usePanorama,
                )
            }

            item(key = "browse-list-load-append") {
                if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                    BrowseSourceLoadingItem()
                }
            }
        }
    }

    if (entries > 0) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            content(constraints.maxHeight)
        }
    } else {
        content(0)
    }
}

@Composable
internal fun BrowseSourceListItem(
    anime: Anime,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    entries: Int,
    containerHeight: Int,
    usePanorama: Boolean = false,
) {
    AnimeListItem(
        title = anime.title,
        isSelected = isSelected,
        coverData = remember(anime.id, isFavorite) {
            AnimeCover(
                animeId = anime.id,
                sourceId = anime.source,
                isAnimeFavorite = isFavorite,
                ogUrl = anime.thumbnailUrl,
                lastModified = anime.coverLastModified,
            )
        },
        coverAlpha = if (isFavorite) CommonAnimeItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = isFavorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        entries = entries,
        containerHeight = containerHeight,
        usePanorama = usePanorama,
    )
}
