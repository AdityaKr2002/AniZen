package eu.kanade.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.ContainerStyle
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.util.fastForEach

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
    var isSearchFocused by remember { mutableStateOf(false) }

    // Handle system back button properly: first hide keyboard/unfocus, then clear text
    BackHandler(enabled = isSearchFocused || !state.searchQuery.isNullOrEmpty()) {
        if (isSearchFocused) {
            focusManager.clearFocus()
        } else {
            onChangeSearchQuery("")
        }
    }

    Column(
        modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Redesigned Reactive Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            OutlinedTextField(
                value = state.searchQuery ?: "",
                onValueChange = onChangeSearchQuery,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                placeholder = { Text(stringResource(MR.strings.action_search_hint)) },
                leadingIcon = {
                    if (isSearchFocused || !state.searchQuery.isNullOrEmpty()) {
                        IconButton(onClick = {
                            if (!state.searchQuery.isNullOrEmpty()) {
                                onChangeSearchQuery("")
                            }
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    } else {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (!state.searchQuery.isNullOrEmpty()) {
                        IconButton(onClick = { onChangeSearchQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            // BOLD and BIG NSFW Toggle - Enclosed within a rounded border
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

        when {
            state.isLoading -> LoadingScreen()
            state.isEmpty -> EmptyScreen(
                stringRes = if (state.searchQuery.isNullOrEmpty()) MR.strings.source_empty_screen else MR.strings.no_results_found,
            )
            else -> {
                ScrollbarLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = contentPadding.calculateBottomPadding() + 8.dp
                    ),
                ) {
                    val items = state.items
                    var i = 0
                    while (i < items.size) {
                        val model = items[i]
                        if (model is SourceUiModel.Header) {
                            item(key = "header-${model.language}") {
                                SourceHeader(
                                    language = model.language,
                                    modifier = Modifier.animateItem()
                                )
                            }
                            i++
                            val groupItems = mutableListOf<SourceUiModel.Item>()
                            while (i < items.size && items[i] is SourceUiModel.Item) {
                                groupItems.add(items[i] as SourceUiModel.Item)
                                i++
                            }
                            item(key = "island-${model.language}") {
                                if (useContainer) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 2.dp
                                    ) {
                                        Column {
                                            groupItems.forEach { item ->
                                                SourceItem(
                                                    source = item.source,
                                                    onClickItem = onClickItem,
                                                    onLongClickItem = onLongClickItem,
                                                    onClickPin = onClickPin,
                                                    modifier = Modifier.animateItem()
                                                )
                                            GroupSeparator(groupItems.last() != item)
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        groupItems.forEach { item ->
                                            SourceItem(
                                                source = item.source,
                                                onClickItem = onClickItem,
                                                onLongClickItem = onLongClickItem,
                                                onClickPin = onClickPin,
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (model is SourceUiModel.Item) {
                            // Handle cases where items might appear before a header (e.g. pinned)
                            item(key = "source-${model.source.id}") {
                                if (useContainer) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 2.dp
                                    ) {
                                        SourceItem(
                                            source = model.source,
                                            onClickItem = onClickItem,
                                            onLongClickItem = onLongClickItem,
                                            onClickPin = onClickPin,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                } else {
                                    SourceItem(
                                        source = model.source,
                                        onClickItem = onClickItem,
                                        onLongClickItem = onLongClickItem,
                                        onClickPin = onClickPin,
                                        modifier = Modifier.animateItem().padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            i++
                        }
                    }
                }
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
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        text = LocaleHelper.getSourceDisplayName(language, context),
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
    source: Source,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                TextButton(onClick = { onClickItem(source, Listing.Latest) }) {
                    Text(
                        text = stringResource(MR.strings.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            SourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
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

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
