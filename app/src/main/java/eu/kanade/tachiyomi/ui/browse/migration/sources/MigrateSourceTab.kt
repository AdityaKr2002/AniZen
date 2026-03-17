package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.MigrateSourceScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrateAnimeScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun Screen.migrateSourceTab(): TabContent {
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MigrateSourceScreenModel() }
    val state by screenModel.state.collectAsState()

    val migrationHelpGuide = stringResource(MR.strings.migration_help_guide)

    return remember(migrationHelpGuide, state.selectionMode, state.sortingMode, state.sortingDirection) {
        TabContent(
            titleRes = MR.strings.label_migration,
            numberTitle = if (state.selectionMode) state.selectedSources.size else 0,
            actions = persistentListOf<AppBar.AppBarAction>().builder()
                .apply {
                    if (state.selectionMode) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = screenModel::selectAll,
                            ),
                        )
                        add(
                            AppBar.Action(
                                title = stringResource(SYMR.strings.select_none),
                                icon = Icons.Outlined.Checklist,
                                onClick = screenModel::selectNone,
                            ),
                        )
                    } else {
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    if (state.sortingMode == SetMigrateSorting.Mode.ALPHABETICAL) {
                                        MR.strings.action_sort_alpha
                                    } else {
                                        MR.strings.action_sort_count
                                    },
                                ),
                                icon = if (state.sortingMode == SetMigrateSorting.Mode.ALPHABETICAL) {
                                    Icons.Outlined.SortByAlpha
                                } else {
                                    Icons.Outlined.Numbers
                                },
                                onClick = screenModel::toggleSortingMode,
                            ),
                        )
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    if (state.sortingDirection == SetMigrateSorting.Direction.ASCENDING) {
                                        MR.strings.action_asc
                                    } else {
                                        MR.strings.action_desc
                                    },
                                ),
                                icon = if (state.sortingDirection == SetMigrateSorting.Direction.ASCENDING) {
                                    Icons.Outlined.ArrowUpward
                                } else {
                                    Icons.Outlined.ArrowDownward
                                },
                                onClick = screenModel::toggleSortingDirection,
                            ),
                        )
                        add(
                            AppBar.Action(
                                title = migrationHelpGuide,
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                onClick = {
                                    uriHandler.openUri("https://aniyomi.org/help/guides/source-migration/")
                                },
                            ),
                        )
                    }
                }
                .build(),
            cancelAction = screenModel::selectNone,
            content = { contentPadding, _ ->
                MigrateSourceScreen(
                    state = state,
                    contentPadding = contentPadding,
                    onClickItem = { source ->
                        navigator.push(MigrateAnimeScreen(source.id))
                    },
                    onToggleSortingDirection = screenModel::toggleSortingDirection,
                    onToggleSortingMode = screenModel::toggleSortingMode,
                    onChangeSearchQuery = screenModel::search,
                    onToggleSelection = screenModel::toggleSelection,
                    onSelectAll = screenModel::selectAll,
                    onSelectNone = screenModel::selectNone,
                    onMatchEnabled = screenModel::matchEnabled,
                    onMatchPinned = screenModel::matchPinned,
                    onMigrate = {
                        val ids = state.selectedSources.toList()
                        if (ids.isNotEmpty()) {
                            navigator.push(MigrateAnimeScreen(ids))
                        }
                    },
                )
            },
        )
    }
}
