package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.anime.model.AnimeCover as EntryCoverModel
import tachiyomi.domain.library.model.LibraryFolder
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.filled.PlayArrow
import eu.kanade.presentation.anime.components.LibraryBottomActionMenu
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import kotlinx.collections.immutable.toImmutableList

@Composable
fun FolderOverlay(
    folder: LibraryFolder,
    items: List<LibraryItem>,
    displayMode: tachiyomi.domain.library.model.LibraryDisplayMode,
    columns: Int,
    usePanorama: Boolean,
    onDismiss: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onClickAnime: (eu.kanade.tachiyomi.ui.library.LibraryItem) -> Unit,
    onLongClickAnime: (eu.kanade.tachiyomi.ui.library.LibraryItem) -> Unit,
    onFolderActionClicked: (List<eu.kanade.tachiyomi.ui.library.LibraryItem>) -> Unit,
    onDownloadClicked: (List<eu.kanade.tachiyomi.ui.library.LibraryItem>) -> Unit,
    onDeleteAnimeClicked: (List<eu.kanade.tachiyomi.ui.library.LibraryItem>) -> Unit,
    onMarkAsSeenClicked: (List<eu.kanade.tachiyomi.ui.library.LibraryItem>) -> Unit,
    onMarkAsUnseenClicked: (List<eu.kanade.tachiyomi.ui.library.LibraryItem>) -> Unit,
    onClickContinueWatching: ((tachiyomi.domain.library.model.LibraryAnime) -> Unit)? = null,
    onClickFilter: (() -> Unit)? = null,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(emptySet<Long>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        BackHandler(enabled = selectedItems.isNotEmpty()) {
            selectedItems = emptySet()
        }

        var animationStarted by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            animationStarted = true
        }

        val animAlpha by animateFloatAsState(
            targetValue = if (animationStarted) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(150),
            label = "alpha",
        )
        val animScale by animateFloatAsState(
            targetValue = if (animationStarted) 1f else 0.9f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "scale",
        )

        val displayItems = remember(items) { items.map { eu.kanade.tachiyomi.ui.library.LibraryDisplayItem.Anime(it) }.toImmutableList() }
        val selectedAnime = remember(selectedItems, items) { items.map { it.libraryAnime }.filter { it.id in selectedItems }.toImmutableList() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(color = Color.Black.copy(alpha = 0.85f * animAlpha))
                }
                .clickable(onClick = onDismiss),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 48.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = animScale
                        scaleY = animScale
                        alpha = animAlpha
                    }
                    .clickable(enabled = false) {}, // consume clicks to prevent dismiss
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row: title + action icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (selectedItems.isNotEmpty()) {
                            IconButton(onClick = { selectedItems = emptySet() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                )
                            }
                            Text(
                                text = "${selectedItems.size} selected",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { selectedItems = items.map { it.libraryAnime.id }.toSet() }) {
                                Icon(
                                    imageVector = Icons.Outlined.SelectAll,
                                    contentDescription = "Select all",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { 
                                val allIds = items.map { it.libraryAnime.id }.toSet()
                                selectedItems = allIds - selectedItems
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.FlipToBack,
                                    contentDescription = "Invert",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Rename folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete folder",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                            if (onClickFilter != null) {
                                IconButton(onClick = onClickFilter) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Filter, Sort, Display, Group",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${items.size} titles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No titles in this folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            val containerHeight = constraints.maxHeight

                            val onClick: (tachiyomi.domain.library.model.LibraryAnime) -> Unit = {
                                if (selectedItems.isNotEmpty()) {
                                    selectedItems = if (selectedItems.contains(it.id)) {
                                        selectedItems - it.id
                                    } else {
                                        selectedItems + it.id
                                    }
                                } else {
                                    val libItem = items.find { item -> item.libraryAnime.id == it.id }
                                    if (libItem != null) onClickAnime(libItem)
                                }
                            }
                            val onLongClick: (tachiyomi.domain.library.model.LibraryAnime) -> Unit = {
                                selectedItems = if (selectedItems.contains(it.id)) {
                                    selectedItems - it.id
                                } else {
                                    selectedItems + it.id
                                }
                            }
                            
                            val onClickContinueWatchingGrid: ((tachiyomi.domain.library.model.LibraryAnime) -> Unit)? = 
                                if (onClickContinueWatching != null) { it -> onClickContinueWatching(it) } else null
                            
                            when (displayMode) {
                                tachiyomi.domain.library.model.LibraryDisplayMode.List -> {
                                    LibraryList(
                                        items = displayItems,
                                        entries = columns,
                                        containerHeight = containerHeight,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        selection = selectedAnime,
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        onClickContinueWatching = onClickContinueWatchingGrid,
                                        searchQuery = null,
                                        onGlobalSearchClicked = {},
                                        usePanorama = usePanorama,
                                    )
                                }
                                tachiyomi.domain.library.model.LibraryDisplayMode.CompactGrid -> {
                                    LibraryCompactGrid(
                                        items = displayItems,
                                        showTitle = true,
                                        columns = columns,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        selection = selectedAnime,
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        onClickContinueWatching = onClickContinueWatchingGrid,
                                        searchQuery = null,
                                        onGlobalSearchClicked = {},
                                        usePanorama = usePanorama,
                                    )
                                }
                                tachiyomi.domain.library.model.LibraryDisplayMode.ComfortableGrid -> {
                                    LibraryComfortableGrid(
                                        items = displayItems,
                                        columns = columns,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        selection = selectedAnime,
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        onClickContinueWatching = onClickContinueWatchingGrid,
                                        searchQuery = null,
                                        onGlobalSearchClicked = {},
                                        usePanorama = usePanorama,
                                    )
                                }
                                tachiyomi.domain.library.model.LibraryDisplayMode.CoverOnlyGrid -> {
                                    LibraryCoverOnlyGrid(
                                        items = displayItems,
                                        columns = columns,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        selection = selectedAnime,
                                        onClick = onClick,
                                        onLongClick = onLongClick,
                                        onClickContinueWatching = onClickContinueWatchingGrid,
                                        searchQuery = null,
                                        onGlobalSearchClicked = {},
                                        usePanorama = usePanorama,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Menu inside the dialog
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.95f)) {
                LibraryBottomActionMenu(
                    visible = selectedItems.isNotEmpty(),
                    onChangeCategoryClicked = {},
                    onFolderClicked = {
                        val selectedLibraryItems = items.filter { selectedItems.contains(it.libraryAnime.id) }
                        onFolderActionClicked(selectedLibraryItems)
                        selectedItems = emptySet()
                    },
                    onMarkAsSeenClicked = {
                        val selectedLibraryItems = items.filter { selectedItems.contains(it.libraryAnime.id) }
                        onMarkAsSeenClicked(selectedLibraryItems)
                        selectedItems = emptySet()
                    },
                    onMarkAsUnseenClicked = {
                        val selectedLibraryItems = items.filter { selectedItems.contains(it.libraryAnime.id) }
                        onMarkAsUnseenClicked(selectedLibraryItems)
                        selectedItems = emptySet()
                    },
                    onFavoriteClicked = null,
                    onDownloadClicked = { _ ->
                        val selectedLibraryItems = items.filter { selectedItems.contains(it.libraryAnime.id) }
                        onDownloadClicked(selectedLibraryItems)
                        selectedItems = emptySet()
                    },
                    onDeleteClicked = {
                        val selectedLibraryItems = items.filter { selectedItems.contains(it.libraryAnime.id) }
                        onDeleteAnimeClicked(selectedLibraryItems)
                        selectedItems = emptySet()
                    },
                    onMigrateClicked = {},
                    onMergeClicked = {},
                    onSelectionUpdateClicked = {},
                    onClickCollectRecommendations = null,
                    onClickResetInfo = null,
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newName.isNotBlank()) {
                            onRenameFolder(newName.trim())
                            showRenameDialog = false
                        }
                    }),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onRenameFolder(newName.trim())
                        showRenameDialog = false
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Folder") },
            text = {
                Text("Delete \"${folder.name}\"? Anime inside will not be deleted and will return to the main library.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder()
                    showDeleteDialog = false
                    onDismiss()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

