package eu.kanade.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.components.BulkSelectionToolbar
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.domain.anime.model.Anime
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun MigrateSearchScreen(
    state: SearchScreenModel.State,
    fromSourceId: Long?,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getAnime: @Composable (Anime) -> State<Anime>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Anime) -> Unit,
    onLongClickItem: (Anime) -> Unit,
    selection: List<Anime> = emptyList(),
    onClearSelection: () -> Unit = {},
    onChangeCategory: () -> Unit = {},
) {
    BackHandler(enabled = selection.isNotEmpty(), onBack = onClearSelection)

    Scaffold(
        topBar = { scrollBehavior ->
            if (selection.isEmpty()) {
                GlobalSearchToolbar(
                    searchQuery = state.searchQuery,
                    progress = state.progress,
                    total = state.total,
                    navigateUp = navigateUp,
                    onChangeSearchQuery = onChangeSearchQuery,
                    onSearch = onSearch,
                    sourceFilter = state.actualSourceFilter,
                    onChangeSearchFilter = onChangeSearchFilter,
                    onlyShowHasResults = state.onlyShowHasResults,
                    onToggleResults = onToggleResults,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                BulkSelectionToolbar(
                    selectedCount = selection.size,
                    isRunning = false,
                    onClickClearSelection = onClearSelection,
                    onChangeCategoryClick = onChangeCategory,
                )
            }
        },
    ) { paddingValues ->
        GlobalSearchContent(
            fromSourceId = fromSourceId,
            items = state.filteredItems,
            contentPadding = paddingValues,
            getAnime = getAnime,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            selection = selection,
        )
    }
}
