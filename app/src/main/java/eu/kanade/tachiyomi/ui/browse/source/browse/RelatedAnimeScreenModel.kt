package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.anime.SuggestionSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedAnimeScreenModel(
    private val animeId: Long,
    private val getAnime: GetAnime = Injekt.get(),
) : StateScreenModel<RelatedAnimeScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            val anime = getAnime.await(animeId) ?: return@launchIO
            mutableState.update { it.copy(title = anime.title) }

            val cached = AnimeScreenModel.suggestionsCache.get(animeId)
            if (cached != null) {
                var newItems = state.value.items
                cached.sections.forEach { section ->
                    val title = when (section.type) {
                        SuggestionSection.Type.Franchise -> "Franchise"
                        SuggestionSection.Type.Similarity -> "Similar"
                        SuggestionSection.Type.Author -> section.title
                        SuggestionSection.Type.Source -> section.title
                        SuggestionSection.Type.Tag -> "Tags"
                    }
                    newItems = newItems.put(title, section.items)
                }
                mutableState.update { it.copy(items = newItems) }
            }
        }
    }

    @Immutable
    data class State(
        val title: String = "",
        val items: PersistentMap<String, ImmutableList<Anime>> = persistentMapOf(),
    )
}
