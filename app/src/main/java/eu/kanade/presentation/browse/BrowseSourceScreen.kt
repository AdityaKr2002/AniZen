package eu.kanade.presentation.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.PanoramaMode
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGrid
import eu.kanade.presentation.browse.components.BrowseSourceCompactGrid
import eu.kanade.presentation.browse.components.BrowseSourceList
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.Source
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun BrowseSourceContent(
    source: Source?,
    animeList: LazyPagingItems<Anime>,
    columns: GridCells,
    entries: Int = 0,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
    selection: ImmutableList<Anime>,
    favoriteIds: ImmutableSet<Long>,
    onBatchIncrement: (Int) -> Unit = {},
) {
    val context = LocalContext.current

    val errorState = (animeList.loadState.refresh as? LoadState.Error)
        ?: (animeList.loadState.append as? LoadState.Error)
        ?: (animeList.loadState.prepend as? LoadState.Error)

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        with(context) { state.error.formattedMessage }
    }

    LaunchedEffect(errorState) {
        if (animeList.itemCount > 0 && errorState != null) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.stringResource(MR.strings.action_retry),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> animeList.retry()
            }
        }
    }

    val screenState = when {
        animeList.itemCount <= 0 && errorState != null -> "Error"
        animeList.itemCount == 0 && animeList.loadState.refresh is LoadState.Loading -> "Loading"
        else -> "Content"
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            fadeIn().togetherWith(fadeOut())
        },
        label = "browse_source_content",
    ) { state ->
        when (state) {
            "Error" -> {
                EmptyScreen(
                    message = errorState?.let { getErrorMessage(it) }.orEmpty(),
                    actions = persistentListOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_retry,
                            icon = Icons.Outlined.Refresh,
                            onClick = animeList::refresh,
                        ),
                        EmptyScreenAction(
                            stringRes = MR.strings.action_open_in_web_view,
                            icon = Icons.Outlined.Public,
                            onClick = onWebViewClick,
                        ),
                        EmptyScreenAction(
                            stringRes = MR.strings.label_help,
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            onClick = onHelpClick,
                        ),
                    ),
                )
            }
            "Loading" -> {
                LoadingScreen(
                    modifier = Modifier.padding(contentPadding),
                )
            }
            "Content" -> {
                val uiPreferences = remember { Injekt.get<UiPreferences>() }
                val globalPanoramaState = uiPreferences.panoramaCover().collectAsStatePref()
                val browseModeState = uiPreferences.browsePanoramaMode().collectAsStatePref()
                
                val globalPanorama = globalPanoramaState.value
                val browseMode = browseModeState.value
                val effectivePanorama = remember(globalPanorama, browseMode) { browseMode.resolve(globalPanorama) }

                when (displayMode) {
                    LibraryDisplayMode.ComfortableGrid -> {
                        BrowseSourceComfortableGrid(
                            animeList = animeList,
                            columns = columns,
                            contentPadding = contentPadding,
                            onAnimeClick = onAnimeClick,
                            onAnimeLongClick = onAnimeLongClick,
                            selection = selection,
                            favoriteIds = favoriteIds,
                            onBatchIncrement = onBatchIncrement,
                            usePanorama = effectivePanorama,
                        )
                    }
                    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                        BrowseSourceCompactGrid(
                            animeList = animeList,
                            columns = columns,
                            showTitle = displayMode is LibraryDisplayMode.CompactGrid,
                            contentPadding = contentPadding,
                            onAnimeClick = onAnimeClick,
                            onAnimeLongClick = onAnimeLongClick,
                            selection = selection,
                            favoriteIds = favoriteIds,
                            onBatchIncrement = onBatchIncrement,
                            usePanorama = effectivePanorama,
                        )
                    }
                    LibraryDisplayMode.List -> {
                        BrowseSourceList(
                            animeList = animeList,
                            contentPadding = contentPadding,
                            onAnimeClick = onAnimeClick,
                            onAnimeLongClick = onAnimeLongClick,
                            selection = selection,
                            favoriteIds = favoriteIds,
                            usePanorama = effectivePanorama,
                            entries = entries,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseSourceScreen(
    source: Source?,
    animeList: LazyPagingItems<Anime>,
    columns: GridCells,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
    onBatchIncrement: (Int) -> Unit,
    selection: ImmutableList<Anime>,
    favoriteIds: ImmutableSet<Long>,
    entries: Int = 0,
) {
    if (source == null) {
        EmptyScreen(
            message = stringResource(MR.strings.source_not_installed, "Unknown"),
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    if (source is StubSource) {
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = source.name,
                    navigateUp = {},
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            EmptyScreen(
                message = stringResource(MR.strings.source_not_installed, source.toString()),
                modifier = Modifier.padding(paddingValues),
            )
        }
        return
    }

    BrowseSourceContent(
        source = source,
        animeList = animeList,
        columns = columns,
        displayMode = displayMode,
        snackbarHostState = snackbarHostState,
        contentPadding = contentPadding,
        onWebViewClick = onWebViewClick,
        onHelpClick = onHelpClick,
        onLocalSourceHelpClick = onLocalSourceHelpClick,
        onAnimeClick = onAnimeClick,
        onAnimeLongClick = onAnimeLongClick,
        selection = selection,
        favoriteIds = favoriteIds,
        onBatchIncrement = onBatchIncrement,
        entries = entries,
    )
}
