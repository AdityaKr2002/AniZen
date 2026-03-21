package eu.kanade.presentation.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.components.MoreItem
import eu.kanade.presentation.more.components.MoreSection
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    isFDroid: Boolean,
    hiddenTabs: List<NavItem>,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickLibraryUpdateErrors: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickPlayerSettings: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_more),
                navigateUp = null,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        ) {
            item {
                MoreSection(title = "General") {
                    hiddenTabs.forEach { navItem ->
                        MoreItem(
                            title = stringResource(navItem.titleRes),
                            // We use Default icon for now or map them
                            icon = when (navItem) {
                                NavItem.LIBRARY -> null // Use default icons from system if possible
                                NavItem.HISTORY -> Icons.Outlined.History
                                NavItem.BROWSE -> null
                                NavItem.UPDATES -> null
                                NavItem.FEED -> null
                                NavItem.MORE -> null
                                NavItem.ADAPTIVE -> null
                            },
                            onClick = {
                                scope.launch {
                                    val homeTab = when (navItem) {
                                        NavItem.LIBRARY -> HomeScreen.HomeTab.AnimeLib()
                                        NavItem.FEED -> HomeScreen.HomeTab.Feed
                                        NavItem.UPDATES -> HomeScreen.HomeTab.Updates
                                        NavItem.HISTORY -> HomeScreen.HomeTab.History
                                        NavItem.BROWSE -> HomeScreen.HomeTab.Browse()
                                        NavItem.MORE -> HomeScreen.HomeTab.More(false)
                                        NavItem.ADAPTIVE -> HomeScreen.HomeTab.More(false)
                                    }
                                    HomeScreen.openTab(homeTab)
                                }
                            }
                        )
                    }
                    MoreItem(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = Icons.Outlined.Storage,
                        onClick = onClickDataAndStorage
                    )
                    MoreItem(
                        title = stringResource(MR.strings.label_settings),
                        icon = Icons.Outlined.Settings,
                        onClick = onClickSettings
                    )
                    MoreItem(
                        title = stringResource(MR.strings.label_player_settings),
                        icon = Icons.Outlined.VideoSettings,
                        onClick = onClickPlayerSettings
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                MoreSection(title = "Stats & Data") {
                    MoreItem(
                        title = "Statistics",
                        icon = Icons.Outlined.QueryStats,
                        onClick = onClickStats
                    )
                }
            }
        }
    }
}
