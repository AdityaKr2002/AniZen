package eu.kanade.tachiyomi.ui.history

import android.content.Context
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.history.HistoryScreen
import eu.kanade.presentation.history.components.HistoryDeleteAllDialog
import eu.kanade.presentation.history.components.HistoryDeleteDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.injectLazy
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref

data object HistoryTab : Tab {

    private val resumeLastEpisodeSeenEvent = Channel<Unit>()

    override val options: TabOptions
        @Composable
        get() {
            val uiPreferences = remember { Injekt.get<UiPreferences>() }
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            val visibleTabs by uiPreferences.bottomNavTabs().collectAsStatePref()
            val index = remember(visibleTabs) { 
                val i = visibleTabs.indexOf(NavItem.HISTORY.id)
                if (i != -1) i.toUShort() else 5u
            }
            return TabOptions(
                index = index,
                title = stringResource(MR.strings.history),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastEpisodeSeenEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val fromMore = isTabFromMore(NavItem.HISTORY.id)
        // Hoisted for history tab's search bar
        val snackbarHostState = SnackbarHostState()

        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { HistoryScreenModel() }
        val state by screenModel.state.collectAsState()
        val searchQuery by screenModel.query.collectAsState()

        val scope = rememberCoroutineScope()
        val navigateUp: (() -> Unit)? = if (fromMore) {
            {
                if (navigator.lastItem == HomeScreen) {
                    scope.launch { HomeScreen.openTab(HomeScreen.HomeTab.AnimeLib()) }
                } else {
                    navigator.pop()
                }
            }
        } else {
            null
        }

        suspend fun openEpisode(context: Context, episode: Episode?) {
            val playerPreferences: PlayerPreferences by injectLazy()
            val extPlayer = playerPreferences.alwaysUseExternalPlayer().get()
            if (episode != null) {
                MainActivity.startPlayerActivity(
                    context,
                    episode.animeId,
                    episode.id,
                    extPlayer,
                )
            }
        }

        HistoryScreen(
            state = state,
            searchQuery = searchQuery,
            onClickCover = { navigator.push(AnimeScreen(it)) },
            onClickResume = { scope.launch { openEpisode(context, it) } },
            onEditSearch = screenModel::search,
            onDelete = { screenModel.deleteHistory(it) },
            onDeleteAll = { screenModel.deleteAllHistory() },
            onNextEpisode = { screenModel.getNextEpisode() },
            navigateUp = navigateUp,
            snackbarHostState = snackbarHostState,
        )

        val historyDeleteDialog by screenModel.historyDeleteDialog.collectAsState()
        historyDeleteDialog?.let {
            HistoryDeleteDialog(
                onDismissRequest = { screenModel.historyDeleteDialog.value = null },
                onDelete = {
                    screenModel.deleteHistory(it, true)
                    screenModel.historyDeleteDialog.value = null
                },
            )
        }

        val historyDeleteAllDialog by screenModel.historyDeleteAllDialog.collectAsState()
        if (historyDeleteAllDialog) {
            HistoryDeleteAllDialog(
                onDismissRequest = { screenModel.historyDeleteAllDialog.value = false },
                onDelete = {
                    screenModel.deleteAllHistory()
                    screenModel.historyDeleteAllDialog.value = false
                },
            )
        }

        LaunchedEffect(Unit) {
            resumeLastEpisodeSeenEvent.receiveAsFlow().collectLatest {
                val episode = screenModel.getNextEpisode()
                if (episode != null) {
                    openEpisode(context, episode)
                } else {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_episode))
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                when (event) {
                    HistoryScreenModel.Event.InternalError -> {
                        snackbarHostState.showSnackbar(context.stringResource(MR.strings.internal_error))
                    }
                    HistoryScreenModel.Event.HistoryCleared -> {
                        snackbarHostState.showSnackbar(context.stringResource(MR.strings.clear_history_completed))
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            // AM (DISCORD) -->
            DiscordRPCService.setAnimeScreen(context, DiscordScreen.HISTORY)
            DiscordRPCService.setMangaScreen(context, DiscordScreen.HISTORY)
            // <-- AM (DISCORD)
        }
    }
}
