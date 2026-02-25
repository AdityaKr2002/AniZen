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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

@Composable
fun FeedScreen(
    screenModel: FeedScreenModel,
    onAnimeClick: (Anime) -> Unit,
    onAddSourceClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state by screenModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.categories.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(3) {
                SkeletonFeedIsland()
            }
        }
        return
    }

    val visibleCategories = remember(state.categories, state.items) {
        state.categories.filter { category ->
            !category.name.equals("Global", ignoreCase = true) || (state.items[category.id]?.isNotEmpty() == true)
        }
    }

    if (visibleCategories.isEmpty()) {
         // If Global is hidden and no other categories exist, show empty state (effectively Global's empty state)
         Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
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
        return
    }

    val pagerState = rememberPagerState { visibleCategories.size }

    LaunchedEffect(pagerState.currentPage) {
        // Optional: Persist selected category or other logic
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (visibleCategories.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, visibleCategories.lastIndex),
                modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
                edgePadding = 0.dp,
                divider = {}, // Remove default divider if desired, or keep it
            ) {
                visibleCategories.forEachIndexed { index: Int, category: FeedSavedSearchCategory ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(text = category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
        ) { page ->
            val category = visibleCategories.getOrNull(page)
            if (category != null) {
                val items = state.items[category.id]
                val listPadding = if (visibleCategories.size > 1) {
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 8.dp
                    )
                } else {
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 8.dp
                    )
                }

                if (items == null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listPadding,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(3) {
                            SkeletonFeedIsland()
                        }
                    }
                } else if (items.isEmpty()) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listPadding,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(
                            items = items,
                            key = { "feed-${it.feed.id}" },
                        ) { item ->
                            val title = if (item.savedSearch != null) {
                                "${item.source.name} (${item.savedSearch.name})"
                            } else {
                                "${item.source.name} (${tachiyomi.domain.source.model.FeedSavedSearch.Type.from(item.feed.type).name})"
                            }
                            FeedIsland(
                                title = title,
                                animeList = item.animeList,
                                onAnimeClick = onAnimeClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedIsland(
    title: String,
    animeList: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, // 30% Secondary
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
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = animeList,
                    key = { anime: tachiyomi.domain.anime.model.Anime -> "anime-${anime.id}" },
                ) { anime: tachiyomi.domain.anime.model.Anime ->
                    FeedCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime) }
                    )
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
    val entry = AnimeCover.getEntry(anime.id)
    val width = if (entry == AnimeCover.Panorama) 200.dp else 100.dp

    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entry(
            data = anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
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