package eu.kanade.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.PushPin
import tachiyomi.domain.source.model.Pin
import tachiyomi.source.local.isLocal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.ContainerStyle
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AnimatedFloatingSearchBox
import eu.kanade.presentation.components.SOURCE_SEARCH_BOX_HEIGHT
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.isScrollingUp
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.util.fastForEach
import eu.kanade.presentation.components.AnimatedFloatingSearchBox
import eu.kanade.presentation.components.SOURCE_SEARCH_BOX_HEIGHT

@Composable
fun SourcesScreen(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onToggleNsfwOnly: () -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val containerStyles by uiPreferences.containerStyles().collectAsState()
    val useContainer = remember(containerStyles) { ContainerStyle.BROWSE in containerStyles }
    val focusManager = LocalFocusManager.current

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    var searchBoxHeight by remember { mutableStateOf(SOURCE_SEARCH_BOX_HEIGHT) }

    // Handle system back button: 1 click to clear text and focus if text exists.
    BackHandler(enabled = !state.searchQuery.isNullOrEmpty()) {
        onChangeSearchQuery("")
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                top = searchBoxHeight,
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                bottom = contentPadding.calculateBottomPadding() + 8.dp
            ),
        ) {
            if (state.isLoading) {
                item(key = "loading") {
                    LoadingScreen(modifier = Modifier.fillParentMaxSize())
                }
            } else if (state.isEmpty) {
                item(key = "empty") {
                    EmptyScreen(
                        stringRes = if (state.searchQuery.isNullOrEmpty()) MR.strings.source_empty_screen else MR.strings.no_results_found,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                itemsIndexed(
                    items = state.items,
                    key = { index, model ->
                        when (model) {
                            is SourceUiModel.Header -> "header-${model.language}-${model.displayName}-$index"
                            is SourceUiModel.Item -> "source-${model.headerKey}-${model.source.id}-$index"
                        }
                    },
                    contentType = { _, model ->
                        when (model) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                ) { _, model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            SourceHeader(
                                displayName = model.displayName,
                                modifier = Modifier.animateItem()
                            )
                        }
                        is SourceUiModel.Item -> {
                            val shape = if (useContainer) MaterialTheme.shapes.large else RoundedCornerShape(0.dp)
                            val containerColor = if (useContainer) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            val elevation = if (useContainer) 2.dp else 0.dp
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                shape = shape,
                                color = containerColor,
                                tonalElevation = elevation
                            ) {
                                SourceItem(
                                    item = model,
                                    onClickItem = onClickItem,
                                    onLongClickItem = onLongClickItem,
                                    onClickPin = onClickPin,
                                    hideLatest = state.hideLatest,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated floating search bar on top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            AnimatedFloatingSearchBox(
                modifier = Modifier.weight(1f),
                listState = lazyListState,
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                placeholderText = stringResource(MR.strings.action_search_hint),
                onGloballyPositioned = { layoutCoordinates ->
                    searchBoxHeight = with(density) { layoutCoordinates.size.height.toDp() }
                },
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = lazyListState.isScrollingUp(),
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically(),
            ) {
                FilterChip(
                    selected = state.nsfwOnly,
                    onClick = onToggleNsfwOnly,
                    label = {
                        Text(
                            text = "18+",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.error,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = state.nsfwOnly,
                        selectedBorderColor = MaterialTheme.colorScheme.error,
                        selectedBorderWidth = 2.dp,
                    ),
                    modifier = Modifier.height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupSeparator(enabled: Boolean) {
    if (enabled) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun SourceHeader(
    displayName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = displayName,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        style = MaterialTheme.typography.header,
    )
}

@Composable
private fun SourceItem(
    item: SourceUiModel.Item,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    hideLatest: Boolean,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        item = item,
        onClickItem = { onClickItem(item.source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(item.source) },
        action = {
            if (item.source.supportsLatest && !hideLatest) {
                TextButton(onClick = { onClickItem(item.source, Listing.Latest) }) {
                    Text(
                        text = stringResource(MR.strings.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            SourcePinButton(
                isPinned = Pin.Pinned in item.source.pin,
                onClick = { onClickPin(item.source) },
            )
        },
    )
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(
            alpha = SECONDARY_ALPHA,
        )
    }
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            contentDescription = stringResource(description),
        )
    }
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onClickAddToFeed: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                if (!source.isLocal()) {
                    if (onClickAddToFeed != null) {
                        Text(
                            text = "Add to Feed",
                            modifier = Modifier
                                .clickable(onClick = onClickAddToFeed)
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                    Text(
                        text = stringResource(MR.strings.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}
