package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.domain.anime.model.hasCustomCover
import tachiyomi.domain.anime.model.toSAnime
import eu.kanade.presentation.anime.AnimeScreen
import eu.kanade.presentation.anime.DuplicateAnimeDialog
import eu.kanade.presentation.anime.EditCoverAction
import eu.kanade.presentation.anime.EpisodeOptionsDialogScreen
import eu.kanade.presentation.anime.EpisodeSettingsDialog
import eu.kanade.presentation.anime.components.AnimeCoverDialog
import eu.kanade.presentation.anime.components.ClearAnimeDialog
import eu.kanade.presentation.anime.components.DeleteEpisodesDialog
import eu.kanade.presentation.anime.components.SetIntervalDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.more.settings.screen.player.PlayerSettingsGesturesScreen.SkipIntroLengthDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isLocalOrStub
import eu.kanade.tachiyomi.source.isSourceForTorrents
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.ui.anime.merged.EditMergedSettingsDialog
import eu.kanade.tachiyomi.ui.anime.notes.AnimeNotesScreen
import exh.source.MERGED_SOURCE_ID
import eu.kanade.tachiyomi.ui.anime.track.TrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateDialog
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateDialogScreenModel
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import mihon.feature.migration.config.MigrationConfigScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.RelatedAnimeScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.source.service.SourcePreferences
import uy.kohesive.injekt.injectLazy

class AnimeScreen(
    private val animeId: Long,
    val fromSource: Boolean = false,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    @Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
    override fun Content() {
        val sourcePreferences: SourcePreferences by injectLazy()
        val uiPreferences: UiPreferences by injectLazy()

        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val screenModel =
            rememberScreenModel { AnimeScreenModel(context, lifecycleOwner.lifecycle, animeId, fromSource) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        val successState = state as? AnimeScreenModel.State.Success
        if (successState == null) {
            LoadingScreen()
            return
        }

        val isHttpSource = remember { successState.source is HttpSource }

        LaunchedEffect(successState.anime, screenModel.source) {
            if (isHttpSource) {
                try {
                    withIOContext {
                        assistUrl = getAnimeUrl(screenModel.anime, screenModel.source)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to get anime URL" }
                }
            }
        }

        val autoExpandDescription by uiPreferences.autoExpandAnimeDescription().collectAsState()
        val showSeasonsSection by uiPreferences.showSeasonsSection().collectAsState()

        AnimeScreen(
            state = successState,
            snackbarHostState = screenModel.snackbarHostState,
            nextUpdate = successState.anime.expectedNextUpdate,
            isTabletUi = isTabletUi(),
            episodeSwipeStartAction = screenModel.episodeSwipeStartAction,
            episodeSwipeEndAction = screenModel.episodeSwipeEndAction,
            showNextEpisodeAirTime = screenModel.showNextEpisodeAirTime,
            alwaysUseExternalPlayer = screenModel.alwaysUseExternalPlayer,
            showFileSize = screenModel.showFileSize,
            autoExpandDescription = autoExpandDescription,
            showSeasonsSection = showSeasonsSection,
            onBackClicked = { navigator.pop() },
            onEpisodeClicked = { episode, alt ->
                scope.launchIO {
                    if (successState.source.isSourceForTorrents()) {
                        TorrentServerService.start()
                        TorrentServerService.wait(10)
                        TorrentServerUtils.setTrackersList()
                    }
                    val extPlayer = screenModel.alwaysUseExternalPlayer != alt
                    openEpisode(context, episode, extPlayer)
                }
            },
            onDownloadEpisode = screenModel::runEpisodeDownloadActions.takeIf { !successState.source.isLocalOrStub() },
            onAddToLibraryClicked = {
                screenModel.toggleFavorite()
            },
            onWebViewClicked = {
                openAnimeInWebView(
                    navigator,
                    screenModel.anime,
                    screenModel.source,
                )
            }.takeIf { isHttpSource },
            onWebViewLongClicked = {
                copyAnimeUrl(
                    context,
                    screenModel.anime,
                    screenModel.source,
                )
            }.takeIf { isHttpSource },
            onTrackingClicked = {
                if (!successState.hasLoggedInTrackers) {
                    navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
                } else {
                    screenModel.showTrackDialog()
                }
            },
            onTagSearch = { scope.launch { performGenreSearch(navigator, it, screenModel.source!!) } },
            onFilterButtonClicked = screenModel::showSettingsDialog,
            onRefresh = screenModel::fetchAllFromSource,
            onContinueWatching = {
                scope.launchIO {
                    val extPlayer = screenModel.alwaysUseExternalPlayer
                    continueWatching(context, screenModel.getNextUnseenEpisode(), extPlayer)
                }
            },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = screenModel::showCoverDialog,
            onShareClicked = {
                shareAnime(
                    context,
                    screenModel.anime,
                    screenModel.source,
                )
            }.takeIf { isHttpSource },
            onDownloadActionClicked = screenModel::runDownloadAction.takeIf { !successState.source.isLocalOrStub() },
            onEditCategoryClicked = screenModel::showChangeCategoryDialog.takeIf { successState.anime.favorite },
            onEditNotesClicked = { navigator.push(AnimeNotesScreen(successState.anime)) }.takeIf { successState.anime.favorite } ?: {},
            onMigrateClicked = {
                navigator.push(MigrationConfigScreen(successState.anime.id))
            }.takeIf { successState.anime.favorite },
            changeAnimeSkipIntro = screenModel::showAnimeSkipIntroDialog.takeIf { successState.anime.favorite },
            onEditInfoClicked = screenModel::showEditAnimeInfoDialog,
            onClearAnimeClicked = screenModel::showClearAnimeDialog,
            onMergeClicked = screenModel::showEditMergedSettings.takeIf { successState.source.id == MERGED_SOURCE_ID },
            onMultiBookmarkClicked = screenModel::bookmarkEpisodes,
            // AM (FILLERMARK) -->
            onMultiFillermarkClicked = screenModel::fillermarkEpisodes,
            // <-- AM (FILLERMARK)
            onMultiMarkAsSeenClicked = screenModel::markEpisodesSeen,
            onMarkPreviousAsSeenClicked = screenModel::markPreviousEpisodeSeen,
            onMultiDeleteClicked = screenModel::showDeleteEpisodeDialog,
            onEpisodeSwipe = screenModel::episodeSwipe,
            onEpisodeSelected = screenModel::toggleSelection,
            onAllEpisodeSelected = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onLocalScoreClicked = screenModel::showLocalScoreDialog,
            onToggleDiscoveryExpansion = screenModel::toggleDiscoveryExpansion,
        )

        val onDismissRequest = {
            screenModel.dismissDialog()
        }
        when (val dialog = successState.dialog) {
            null -> {}
            is AnimeScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen) },
                    onConfirm = { include, _ ->
                        screenModel.moveAnimeToCategoriesAndAddToLibrary(dialog.anime, include)
                    },
                )
            }
            is AnimeScreenModel.Dialog.DeleteEpisodes -> {
                DeleteEpisodesDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleAllSelection(false)
                        screenModel.deleteEpisodes(dialog.episodes)
                    },
                )
            }

            is AnimeScreenModel.Dialog.DuplicateAnime -> {
                DuplicateAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { screenModel.toggleFavorite(checkDuplicate = false) },
                    onOpenAnime = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
                    onMigrate = {
                        screenModel.showMigrateDialog(dialog.duplicate)
                    },
                )
            }

            is AnimeScreenModel.Dialog.Migrate -> {
                MigrateDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { replace ->
                        scope.launchIO {
                            val migrateAnime = Injekt.get<mihon.domain.migration.interactor.MigrateAnimeUseCase>()
                            migrateAnime.invoke(dialog.oldAnime, dialog.newAnime, replace)
                            withUIContext { navigator.replace(AnimeScreen(dialog.newAnime.id)) }
                        }
                    },
                )
            }

            is AnimeScreenModel.Dialog.SetAnimeFetchInterval -> {
                SetIntervalDialog(
                    interval = dialog.anime.fetchInterval,
                    nextUpdate = dialog.anime.expectedNextUpdate,
                    onDismissRequest = onDismissRequest,
                    onValueChanged = { interval: Int -> screenModel.setFetchInterval(dialog.anime, interval) },
                )
            }

            is AnimeScreenModel.Dialog.ShowQualities -> {
                EpisodeOptionsDialogScreen.onDismissDialog = onDismissRequest
                val episodeTitle = if (dialog.anime.displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
                    stringResource(
                        MR.strings.display_mode_episode,
                        formatEpisodeNumber(dialog.episode.episodeNumber),
                    )
                } else {
                    dialog.episode.name
                }
                NavigatorAdaptiveSheet(
                    screen = EpisodeOptionsDialogScreen(
                        useExternalDownloader = screenModel.useExternalDownloader,
                        episodeTitle = episodeTitle,
                        episodeId = dialog.episode.id,
                        animeId = dialog.anime.id,
                        sourceId = dialog.source.id,
                    ),
                    onDismissRequest = onDismissRequest,
                )
            }

            is AnimeScreenModel.Dialog.FullCover -> {
                val sm = rememberScreenModel { MigrateDialogScreenModel(successState.anime.id, sourceManager, getAnime) }
                val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                    if (it != null) {
                        sm.editCover(context, it)
                    }
                }
                AnimeCoverDialog(
                    coverBase64 = successState.anime.thumbnailUrl,
                    onShareClick = { shareAnime(context, successState.anime, successState.source) },
                    onSaveClick = { sm.saveCover(context) },
                    onEditClick = {
                        when (it) {
                            EditCoverAction.EDIT -> getContent.launch("image/*")
                            EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                        }
                    },
                    onDismissRequest = onDismissRequest,
                )
            }
            is AnimeScreenModel.Dialog.EditAnimeInfo -> {
                EditAnimeDialog(
                    anime = dialog.anime,
                    onDismissRequest = screenModel::dismissDialog,
                    onPositiveClick = screenModel::updateAnimeInfo,
                )
            }
            is AnimeScreenModel.Dialog.EditMergedAnimeSettings -> {
                EditMergedSettingsDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    mergedAnimes = dialog.data.anime.values.toList(),
                    mergedReferences = dialog.data.references,
                    onDeleteClick = screenModel::deleteMergedEntry,
                    onPositiveClick = screenModel::updateMergedSettings,
                    onOpenEntryClick = { navigator.push(AnimeScreen(it.animeId!!)) },
                )
            }
            is AnimeScreenModel.Dialog.ClearAnime -> {
                ClearAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteDownloads, deleteFromDatabase ->
                        screenModel.clearAnime(deleteDownloads, deleteFromDatabase)
                    },
                )
            }
            is AnimeScreenModel.Dialog.LocalScorePicker -> {
                LocalScoreDialog(
                    anime = dialog.anime,
                    onDismissRequest = screenModel::dismissDialog,
                    onConfirm = { newScore, newStatus ->
                        screenModel.updateAnimeInfo(
                            title = null,
                            author = null,
                            artist = null,
                            thumbnailUrl = null,
                            description = null,
                            tags = null,
                            status = newStatus,
                            score = newScore,
                            note = null,
                        )
                    }
                )
            }
            is AnimeScreenModel.Dialog.SettingsSheet -> {
                EpisodeSettingsDialog(
                    onDismissRequest = onDismissRequest,
                    anime = successState.anime,
                    onDownloadFilterChanged = screenModel::setDownloadedFilter,
                    onUnseenFilterChanged = screenModel::setUnseenFilter,
                    onBookmarkedFilterChanged = screenModel::setBookmarkedFilter,
                    onFillermarkedFilterChanged = screenModel::setFillermarkedFilter,
                    onSortChanged = screenModel::setSorting,
                    onDisplayModeChanged = screenModel::setDisplayMode,
                    onSetAsDefault = screenModel::setEpisodeSettingsAsDefault,
                )
            }
            is AnimeScreenModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = TrackInfoDialogHomeScreen(
                        animeId = successState.anime.id,
                        animeTitle = successState.anime.title,
                        sourceId = successState.source.id,
                    ),
                    onDismissRequest = onDismissRequest,
                )
            }
            // SY <--
            is AnimeScreenModel.Dialog.ChangeAnimeSkipIntro -> {
                fun updateSkipIntroLength(newLength: Long) {
                    scope.launchIO {
                        screenModel.setAnimeViewerFlags.awaitSetSkipIntroLength(animeId, newLength)
                    }
                }
                SkipIntroLengthDialog(
                    currentSkipIntroLength = if (
                        successState.anime.skipIntroLength == 0L ||
                        successState.anime.skipIntroLength == 0
                    ) {
                        screenModel.gesturePreferences.defaultIntroLength().get()
                    } else {
                        successState.anime.skipIntroLength
                    }.toInt(),
                    defaultSkipIntroLength = screenModel.gesturePreferences.defaultIntroLength().get().toInt(),
                    onDismissRequest = onDismissRequest,
                    onValueChanged = {
                        updateSkipIntroLength(it.toLong())
                        onDismissRequest()
                    },
                )
            }
        }
    }

    private suspend fun continueWatching(
        context: Context,
        unseenEpisode: Episode?,
        useExternalPlayer: Boolean,
    ) {
        if (unseenEpisode != null) openEpisode(context, unseenEpisode, useExternalPlayer)
    }

    private suspend fun openEpisode(context: Context, episode: Episode, useExternalPlayer: Boolean) {
        withIOContext {
            MainActivity.startPlayerActivity(
                context,
                episode.animeId,
                episode.id,
                useExternalPlayer,
            )
        }
    }

    private fun getAnimeUrl(anime_: Anime?, source_: Source?): String? {
        val anime = anime_ ?: return null
        val source = source_ as? HttpSource ?: return null

        return try {
            source.getAnimeUrl(anime.toSAnime())
        } catch (e: Exception) {
            null
        }
    }

    private fun openAnimeInWebView(navigator: Navigator, anime_: Anime?, source_: Source?) {
        getAnimeUrl(anime_, source_)?.let { url ->
            navigator.push(
                WebViewScreen(
                    url = url,
                    initialTitle = anime_?.title,
                    sourceId = source_?.id,
                ),
            )
        }
    }

    private fun shareAnime(context: Context, anime_: Anime?, source_: Source?) {
        try {
            getAnimeUrl(anime_, source_)?.let { url ->
                val intent = url.toUri().toShareIntent(context, type = "text/plain")
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.stringResource(MR.strings.action_share),
                    ),
                )
            }
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
        if (global) {
            navigator.push(GlobalSearchScreen(query))
            return
        }

        if (navigator.size < 2) {
            return
        }

        when (val previousController = navigator.items[navigator.size - 2]) {
            is HomeScreen -> {
                navigator.pop()
                LibraryTab.search(query)
            }
            is BrowseSourceScreen -> {
                navigator.pop()
                previousController.search(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    private suspend fun performGenreSearch(
        navigator: Navigator,
        genreName: String,
        source: Source,
    ) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        if (previousController is BrowseSourceScreen && source is HttpSource) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Anime URL to Clipboard
     */
    private fun copyAnimeUrl(context: Context, anime_: Anime?, source_: Source?) {
        val anime = anime_ ?: return
        val source = source_ as? HttpSource ?: return
        val url = source.getAnimeUrl(anime.toSAnime())
        context.copyToClipboard(url, url)
    }
}
