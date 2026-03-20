package eu.kanade.presentation.updates
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.presentation.anime.components.DotSeparatorText
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.anime.components.EpisodeDownloadIndicator
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit
import tachiyomi.domain.source.service.SourceManager as DomainSourceManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref

internal fun LazyListScope.updatesUiItems(
    uiModels: List<UpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem, altPlayer: Boolean) -> Unit,
    onDownloadEpisode: (List<UpdatesItem>, EpisodeDownloadAction) -> Unit,
    useContainer: Boolean,
    usePanorama: Boolean = false,
) {
    uiModels.forEach { model ->
        when (model) {
            is UpdatesUiModel.Header -> {
                if (useContainer) {
                    item(key = "animeUpdatesHeader-${model.date}") {
                        ListGroupHeader(
                            modifier = Modifier,
                            text = relativeDateText(model.date),
                        )
                    }
                } else {
                    item(key = "animeUpdatesHeader-${model.date}") {
                        ListGroupHeader(
                            modifier = Modifier,
                            text = relativeDateText(model.date),
                        )
                    }
                }
            }
            is UpdatesUiModel.Item -> {
                val updatesItem = model.item
                item(key = "animeUpdate-${updatesItem.update.id}") {
                    if (useContainer) {
                        val shape = when (model.position) {
                            UpdatesUiModel.ItemPosition.SINGLE -> MaterialTheme.shapes.large
                            UpdatesUiModel.ItemPosition.TOP -> MaterialTheme.shapes.large.copy(
                                bottomEnd = ZeroCornerSize,
                                bottomStart = ZeroCornerSize,
                            )
                            UpdatesUiModel.ItemPosition.MIDDLE -> RectangleShape
                            UpdatesUiModel.ItemPosition.BOTTOM -> MaterialTheme.shapes.large.copy(
                                topEnd = ZeroCornerSize,
                                topStart = ZeroCornerSize,
                            )
                        }
                        val topPadding = if (model.position == UpdatesUiModel.ItemPosition.SINGLE || model.position == UpdatesUiModel.ItemPosition.TOP) 4.dp else 0.dp
                        val bottomPadding = if (model.position == UpdatesUiModel.ItemPosition.SINGLE || model.position == UpdatesUiModel.ItemPosition.BOTTOM) 4.dp else 0.dp

                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = topPadding, bottom = bottomPadding)
                                .fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp,
                        ) {
                            UpdatesUiItem(
                                update = updatesItem.update,
                                selected = updatesItem.selected,
                                onLongClick = { onUpdateSelected(updatesItem, !updatesItem.selected, true, true) },
                                onClick = { onClickUpdate(updatesItem, false) },
                                onClickCover = { onClickCover(updatesItem) },
                                onDownloadEpisode = { onDownloadEpisode(listOf(updatesItem), it) },
                                downloadStateProvider = updatesItem.downloadStateProvider,
                                downloadProgressProvider = updatesItem.downloadProgressProvider,
                                updatesItem = updatesItem,
                                usePanorama = usePanorama,
                            )
                        }
                    } else {
                        UpdatesUiItem(
                            modifier = Modifier,
                            update = updatesItem.update,
                            selected = updatesItem.selected,
                            onLongClick = { onUpdateSelected(updatesItem, !updatesItem.selected, true, true) },
                            onClick = { onClickUpdate(updatesItem, false) },
                            onClickCover = { onClickCover(updatesItem) },
                            onDownloadEpisode = { onDownloadEpisode(listOf(updatesItem), it) },
                            downloadStateProvider = updatesItem.downloadStateProvider,
                            downloadProgressProvider = updatesItem.downloadProgressProvider,
                            updatesItem = updatesItem,
                            usePanorama = usePanorama,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesUiItem(
    update: UpdatesWithRelations,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: () -> Unit,
    onDownloadEpisode: (EpisodeDownloadAction) -> Unit,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    updatesItem: UpdatesItem,
    // <-- AM (FILE_SIZE)
    modifier: Modifier = Modifier,
    usePanorama: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val textAlpha = if (update.seen) DISABLED_ALPHA else 1f

    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                },
            )
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (entry, ratio) = AnimeCover.getEntry(update.animeId, usePanoramaOverride = usePanorama)
        entry(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = update.coverData,
            onClick = onClickCover,
            ratio = ratio,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                text = update.animeTitle,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                var modified by remember { androidx.compose.runtime.mutableStateOf(false) }
                if (update.bookmark) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier
                            .size(MaterialTheme.typography.bodySmall.fontSize.value.dp)
                            .graphicsLayer { alpha = textAlpha },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    DotSeparatorText()
                }
                Text(
                    text = update.episodeName,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .graphicsLayer { alpha = textAlpha }
                        .weight(1f, fill = false),
                )

                // AM (FILE_SIZE) -->
                val fileSize = updatesItem.fileSize
                if (fileSize != null) {
                    DotSeparatorText()
                    Text(
                        text = java.text.Formatter().format("%.2f MB", fileSize.toDouble() / (1024 * 1024)).toString(),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.graphicsLayer { alpha = textAlpha },
                    )
                }
                // <-- AM (FILE_SIZE)
            }
        }

        EpisodeDownloadIndicator(
            enabled = true,
            modifier = Modifier.padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = onDownloadEpisode,
        )
    }
}

// AM (FILE_SIZE) -->
private val storagePreferences: StoragePreferences by injectLazy()
private val downloadProvider: DownloadProvider by injectLazy()
private val sourceManager: SourceManager by injectLazy()
// <-- AM (FILE_SIZE)
