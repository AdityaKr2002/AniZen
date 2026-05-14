package eu.kanade.presentation.history

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.model.HistoryWithRelations
import java.time.LocalDate
import java.util.Date
import kotlin.random.Random

class HistoryScreenModelStateProvider : PreviewParameterProvider<HistoryScreenModel.State> {
    override val values: Sequence<HistoryScreenModel.State> = sequenceOf(
        HistoryScreenModel.State(
            list = persistentListOf(
                HistoryUiModel.Header(LocalDate.now()),
                HistoryUiModel.Item(
                    HistoryWithRelations(
                        id = 1L,
                        animeId = 1L,
                        episodeId = 1L,
                        seenAt = Date(),
                        ogTitle = "Anime Title",
                        episodeNumber = 1.0,
                        seen = false,
                        watchDuration = 0L,
                        coverData = tachiyomi.domain.anime.model.AnimeCover(
                            animeId = 1L,
                            sourceId = 1L,
                            isAnimeFavorite = Random.nextBoolean(),
                            ogUrl = "https://example.com/cover.png",
                            lastModified = Random.nextLong(),
                        ),
                    ),
                ),
            ),
        ),
    )
}
