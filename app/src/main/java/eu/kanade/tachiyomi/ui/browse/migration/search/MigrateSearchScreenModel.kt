package eu.kanade.tachiyomi.ui.browse.migration.search

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.anime.interactor.GetAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val animeId: Long,
    initialExtensionFilter: String = "",
    getAnime: GetAnime = Injekt.get(),
) : SearchScreenModel() {

    init {
        extensionFilter = initialExtensionFilter
        screenModelScope.launch {
            val anime = getAnime.await(animeId)!!
            mutableState.update {
                it.copy(
                    fromSourceId = anime.source,
                    searchQuery = getSearchQuery(anime.title),
                )
            }

            search()
        }
    }

    private fun getSearchQuery(title: String): String {
        return title
            .replace(SEASON_REGEX, "")
            .replace(QUALITY_REGEX, "")
            .trim()
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != SourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
            .sortedWith(
                compareBy(
                    { it.id != state.value.fromSourceId },
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    companion object {
        private val SEASON_REGEX = Regex("""(?i)\s*(?:S\d+|Season\s*\d+|2nd\s*Season|Part\s*\d+)""")
        private val QUALITY_REGEX = Regex("""(?i)\s*(?:\[?\d+p\]?|\(BD\)|BD|Uncensored)""")
    }
}
