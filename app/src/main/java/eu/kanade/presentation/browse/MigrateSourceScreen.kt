package eu.kanade.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.anime.components.BottomMenuButton
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AnimatedFloatingSearchBox
import eu.kanade.presentation.components.SOURCE_SEARCH_BOX_HEIGHT
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.network.model.NodeStatus
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.FlagEmoji
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MigrateSourceScreen(
    state: MigrateSourceScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onToggleSortingDirection: () -> Unit,
    onToggleSortingMode: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onToggleSelection: (Source) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onMatchEnabled: () -> Unit,
    onMatchPinned: () -> Unit,
    onMigrate: () -> Unit,
) {
    val context = LocalContext.current
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.searchQuery == null && state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.information_empty_library,
            modifier = Modifier.padding(contentPadding),
        )
        else ->
            MigrateSourceList(
                list = state.items,
                contentPadding = contentPadding,
                onClickItem = onClickItem,
                onLongClickItem = { source ->
                    val sourceId = source.id.toString()
                    context.copyToClipboard(sourceId, sourceId)
                },
                sortingMode = state.sortingMode,
                onToggleSortingMode = onToggleSortingMode,
                sortingDirection = state.sortingDirection,
                onToggleSortingDirection = onToggleSortingDirection,
                state = state,
                onChangeSearchQuery = onChangeSearchQuery,
                onToggleSelection = onToggleSelection,
                onSelectAll = onSelectAll,
                onSelectNone = onSelectNone,
                onMatchEnabled = onMatchEnabled,
                onMatchPinned = onMatchPinned,
                onMigrate = onMigrate,
            )
    }
}

@Composable
private fun MigrateSourceList(
    list: ImmutableList<Pair<Source, Long>>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
    state: MigrateSourceScreenModel.State,
    onChangeSearchQuery: (String?) -> Unit,
    onToggleSelection: (Source) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onMatchEnabled: () -> Unit,
    onMatchPinned: () -> Unit,
    onMigrate: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    BackHandler(enabled = !state.searchQuery.isNullOrBlank()) {
        onChangeSearchQuery("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        val density = LocalDensity.current
        var searchBoxHeight by remember { mutableStateOf(SOURCE_SEARCH_BOX_HEIGHT) }

        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = searchBoxHeight,
                bottom = contentPadding.calculateBottomPadding() + if (state.selectionMode) 80.dp else 0.dp,
            ),
        ) {
            items(
                items = list,
                key = { (source, _) -> "migrate-${source.id}" },
            ) { (source, count) ->
                val isSelected = state.selectedSources.contains(source.id)
                MigrateSourceItem(
                    modifier = Modifier.animateItemFastScroll()
                        .padding(end = MaterialTheme.padding.small),
                    source = source,
                    count = count,
                    isSelected = isSelected,
                    isSelectionMode = state.selectionMode,
                    onClickItem = {
                        if (state.selectionMode) {
                            onToggleSelection(source)
                        } else {
                            onClickItem(source)
                        }
                    },
                    onLongClickItem = { onToggleSelection(source) },
                )
            }
        }

        AnimatedFloatingSearchBox(
            listState = lazyListState,
            searchQuery = state.searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            placeholderText = stringResource(MR.strings.action_search_for_source),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    horizontal = MaterialTheme.padding.medium,
                )
                .align(Alignment.TopCenter),
            onGloballyPositioned = { layoutCoordinates ->
                searchBoxHeight = with(density) { layoutCoordinates.size.height.toDp() }
            },
        )

        MigrateBottomActionMenu(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = state.selectionMode,
            onSelectAll = onSelectAll,
            onSelectNone = onSelectNone,
            onMatchEnabled = onMatchEnabled,
            onMatchPinned = onMatchPinned,
            onMigrate = onMigrate,
        )
    }
}
@Composable
private fun MigrateBottomActionMenu(
    visible: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onMatchEnabled: () -> Unit,
    onMatchPinned: () -> Unit,
    onMigrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 300)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom),
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                BottomMenuButton(
                    title = stringResource(MR.strings.migrate),
                    icon = Icons.Outlined.Checklist,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onMigrate,
                )
                BottomMenuButton(
                    title = stringResource(MR.strings.action_select_all),
                    icon = Icons.Outlined.SelectAll,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onSelectAll,
                )
                BottomMenuButton(
                    title = stringResource(SYMR.strings.select_none),
                    icon = Icons.Outlined.Checklist,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onSelectNone,
                )
                BottomMenuButton(
                    title = stringResource(SYMR.strings.match_enabled_sources),
                    icon = Icons.Outlined.NewReleases,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onMatchEnabled,
                )
                BottomMenuButton(
                    title = stringResource(SYMR.strings.match_pinned_sources),
                    icon = Icons.Outlined.PushPin,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onMatchPinned,
                )
            }
        }
    }
}

@Composable
private fun MigrateSourceItem(
    source: Source,
    count: Long,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClickItem: () -> Unit,
    onLongClickItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnselected = isSelectionMode && !isSelected
    val textDecoration = if (isUnselected) {
        TextDecoration.LineThrough
    } else {
        null
    }

    BaseSourceItem(
        modifier = modifier.alpha(if (isUnselected) 0.5f else 1f),
        source = source,
        showLanguageInContent = source.lang != "",
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { SourceIcon(source = source) },
        action = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                BadgeGroup(
                    modifier = Modifier.secondaryItemAlpha(),
                ) {
                    Badge(text = "$count")
                }
                if (isSelected) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onClickItem() },
                    )
                }
            }
        },
        content = { _, sourceLangString, _, _ ->
            Column(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .weight(1f),
            ) {
                Text(
                    text = source.name.ifBlank { source.id.toString() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = textDecoration,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sourceLangString != null) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = FlagEmoji.getEmojiLangFlag(source.lang) + " " + sourceLangString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = textDecoration,
                        )
                    }
                    if (source.isStub) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = stringResource(MR.strings.not_installed),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textDecoration = textDecoration,
                        )
                    }
                }
            }
        },
    )
}
