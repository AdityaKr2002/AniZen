package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toDomainAnime

class SmartSourceSearchEngine(extraSearchParams: String?) : BaseSmartSearchEngine<SAnime>(extraSearchParams) {

    override fun getTitle(result: SAnime) = result.title

    suspend fun regularSearch(source: CatalogueSource, title: String): Anime? {
        return regularSearch(makeSearchAction(source), title).let {
            it?.toDomainAnime(source.id)
        }
    }

    suspend fun deepSearch(source: CatalogueSource, title: String): Anime? {
        return deepSearch(makeSearchAction(source), title).let {
            it?.toDomainAnime(source.id)
        }
    }

    private fun makeSearchAction(source: CatalogueSource): SearchAction<SAnime> = { query ->
        source.getSearchAnime(1, query, FilterList()).animes
    }
}
