package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.presentation.components.AppBar
import kotlinx.coroutines.launch
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.components.SkeletonFeedIsland
import tachiyomi.presentation.core.components.SkeletonAnimeCard
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun FeedScreen(
    screenModel: FeedScreenModel,
    onAnimeClick: (Anime, Long?) -> Unit,
    onAddSourceClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state by screenModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.categories.isEmpty()) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }

    val visibleCategories = state.categories
    val pagerState = rememberPagerState { visibleCategories.size }

    // Use derivedStateOf for smooth tab tracking at high refresh rates
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    // Update pagerState when categories change to avoid OOB
    LaunchedEffect(visibleCategories.size) {
        if (pagerState.currentPage >= visibleCategories.size && visibleCategories.isNotEmpty()) {
            pagerState.scrollToPage(visibleCategories.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        if (visibleCategories.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = currentPage.coerceIn(0, visibleCategories.lastIndex),
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentPage.coerceIn(0, tabPositions.lastIndex)]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                visibleCategories.forEachIndexed { index, category ->
                    Tab(
                        selected = currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            key = { page -> visibleCategories.getOrNull(page)?.id ?: page },
        ) { page ->
            val category = visibleCategories.getOrNull(page) ?: return@HorizontalPager
            val items = state.items[category.id]

            val listPadding = remember(contentPadding) {
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 8.dp
                )
            }

            if (items == null) {
                // Initial loading of the structure
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(3, key = { "skeleton-$it" }) {
                        SkeletonFeedIsland()
                    }
                }
            } else if (items.isEmpty()) {
                // Category is loaded but has no feeds
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = contentPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyScreen(
                        stringRes = SYMR.strings.feed_tab_empty,
                    )
                    Button(
                        onClick = onAddSourceClick,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Add Sources")
                    }
                }
            } else {
                // Show established containers
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(
                        items = items,
                        key = { "feed-${it.feed.id}" },
                        contentType = { "feed_island" }
                    ) { item ->
                        FeedIsland(
                            item = item,
                            onAnimeClick = { onAnimeClick(it, item.feed.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedIsland(
    item: FeedScreenModel.FeedItem,
    onAnimeClick: (Anime) -> Unit,
) {
    // Memoize the title to avoid re-generating strings on every scroll frame
    val title = remember(item.feed.id, item.feed.type, item.savedSearch?.id) {
        if (item.savedSearch != null) {
            "${item.source.name} (${item.savedSearch.name})"
        } else {
            "${item.source.name} (${tachiyomi.domain.source.model.FeedSavedSearch.Type.from(item.feed.type).name})"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (item.animeList.isEmpty()) {
                // PULSING PLACEHOLDERS INSIDE THE SECTION WHILE FETCHING
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        SkeletonAnimeCard(width = 100.dp)
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = item.animeList,
                        key = { anime -> "anime-${item.feed.id}-${anime.id}" },
                        contentType = { "anime_card" }
                    ) { anime ->
                        FeedCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    anime: Anime,
    onClick: () -> Unit,
) {
    val (entry, ratio) = AnimeCover.getEntry(anime.id)
    val width = if (entry == AnimeCover.Panorama) 200.dp else 100.dp

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entry(
            data = anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            ratio = ratio,
        )
        Text(
            text = anime.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(32.dp)
        )
    }
}
