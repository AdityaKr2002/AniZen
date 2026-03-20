package eu.kanade.presentation.history

import androidx.compose.runtime.Immutable
import tachiyomi.domain.history.model.HistoryWithRelations
import java.time.LocalDate

@Immutable
sealed class HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel()
    data class Item(val item: HistoryWithRelations) : HistoryUiModel()
}
