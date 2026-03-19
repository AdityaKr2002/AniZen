package eu.kanade.presentation.browse

import androidx.compose.runtime.Immutable
import eu.kanade.domain.source.model.Source
import eu.kanade.tachiyomi.network.model.NodeStatus

@Immutable
sealed interface SourceUiModel {
    @Immutable
    data class Item(
        val source: Source,
        val isNsfw: Boolean,
        val status: NodeStatus,
        val isBdix: Boolean,
        val isApi: Boolean,
        val isStub: Boolean,
        val secondaryText: String,
        val displayName: String,
    ) : SourceUiModel

    @Immutable
    data class Header(val language: String, val displayName: String) : SourceUiModel
}
