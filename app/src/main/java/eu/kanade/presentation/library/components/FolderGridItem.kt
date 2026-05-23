package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.tachiyomi.ui.library.LibraryDisplayItem
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryDisplayMode
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun FolderGridItem(
    folder: LibraryDisplayItem.Folder,
    displayMode: LibraryDisplayMode,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showTitle: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val items = folder.items
    val count = items.size

    val isCompact = displayMode is LibraryDisplayMode.CompactGrid || displayMode is LibraryDisplayMode.CoverOnlyGrid

    Column(
        modifier = Modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (count > 0) {
                val previewItems = items.take(4)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            if (previewItems.size > 0) FolderPreviewCover(previewItems[0])
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            if (previewItems.size > 1) FolderPreviewCover(previewItems[1])
                        }
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            if (previewItems.size > 2) FolderPreviewCover(previewItems[2])
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            if (previewItems.size > 3) {
                                if (count > 4) {
                                    FolderPreviewMore(count - 3)
                                } else {
                                    FolderPreviewCover(previewItems[3])
                                }
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }

            if (isCompact && showTitle) {
                CoverTextOverlay(
                    title = folder.folder.name,
                    onClickContinueWatching = null,
                )
            }
        }
        if (!isCompact && showTitle) {
            Text(
                text = folder.folder.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FolderPreviewCover(item: LibraryItem) {
    val anime = item.libraryAnime.anime
    eu.kanade.presentation.anime.components.AnimeCover.Book(
        data = tachiyomi.domain.anime.model.AnimeCover(
            animeId = anime.id,
            sourceId = anime.source,
            isAnimeFavorite = anime.favorite,
            ogUrl = anime.thumbnailUrl,
            lastModified = anime.coverLastModified,
        ),
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
        shouldExtractColor = false,
    )
}

@Composable
private fun FolderPreviewMore(moreCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$moreCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}
