package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreenModel
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun SourcesFilterScreen(
    navigateUp: () -> Unit,
    state: SourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_sources),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.source_filter_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        SourcesFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLanguage = onClickLanguage,
            onClickSource = onClickSource,
        )
    }
}

@Composable
private fun SourcesFilterContent(
    contentPadding: PaddingValues,
    state: SourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        state.items.forEach { (header, items) ->
            val enabled = header.language in state.enabledLanguages
            item(
                key = header.language,
                contentType = "source-filter-header",
            ) {
                SourcesFilterHeader(
                    modifier = Modifier,
                    header = header,
                    enabled = enabled,
                    onClickItem = onClickLanguage,
                )
            }
            if (enabled) {
                items(
                    items = items,
                    key = { "source-filter-${it.source.id}-${it.source.lang}" },
                    contentType = { "source-filter-item" },
                ) { item ->
                    SourcesFilterItem(
                        modifier = Modifier,
                        item = item,
                        isEnabled = "${item.source.id}" !in state.disabledSources,
                        onClickItem = onClickSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesFilterHeader(
    header: SourceUiModel.Header,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = header.displayName,
        checked = enabled,
        onCheckedChanged = { onClickItem(header.language) },
    )
}

@Composable
private fun SourcesFilterItem(
    item: SourceUiModel.Item,
    isEnabled: Boolean,
    onClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        item = item,
        onClickItem = { onClickItem(item.source) },
        action = {
            Checkbox(checked = isEnabled, onCheckedChange = null)
        },
    )
}
