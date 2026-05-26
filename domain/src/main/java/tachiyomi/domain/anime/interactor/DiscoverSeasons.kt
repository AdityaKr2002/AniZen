package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import tachiyomi.domain.anime.model.toDomainAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.anime.service.SeasonRecognition
import tachiyomi.domain.source.service.SourceManager

class DiscoverSeasons(
    private val sourceManager: SourceManager,
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(anime: Anime): List<Anime> {
        val source = sourceManager.get(anime.source) as? AnimeCatalogueSource ?: return emptyList()
        
        val originalFullTitle = anime.title
        val rootTitle = SeasonRecognition.getRootTitle(originalFullTitle)
        
        if (rootTitle.length < 3) return emptyList()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            // 1. Strict Word Coverage Lock
            val originalWordSet = SeasonRecognition.getWordSet(rootTitle)
            if (originalWordSet.isEmpty()) return emptyList()

            val candidates = searchResult.animes.filter { sAnime ->
                val candidateFullTitle = sAnime.title
                
                // NO-TOLERANCE FILTERS
                if (SeasonRecognition.isUnrelated(originalFullTitle, candidateFullTitle)) return@filter false
                
                // 1. Signature Word Coverage Lock
                val originalSignature = SeasonRecognition.getSignatureWords(rootTitle)
                if (originalSignature.isNotEmpty()) {
                    val allWordsContained = originalSignature.all { sigWord ->
                        candidateFullTitle.contains(sigWord, ignoreCase = true)
                    }
                    if (!allWordsContained) return@filter false
                }

                // 2. Multi-Layer Similarity Check
                val dice = SeasonRecognition.diceCoefficient(rootTitle, candidateFullTitle)
                val tokenSort = SeasonRecognition.tokenSortSimilarity(rootTitle, candidateFullTitle)
                val isAcronym = SeasonRecognition.isAcronymMatch(rootTitle, candidateFullTitle)
                
                // Final Decision: Must pass signature check AND one of the similarity checks
                if (dice < 0.4 && tokenSort < 0.4 && !isAcronym && !candidateFullTitle.contains(rootTitle, ignoreCase = true)) {
                    return@filter false
                }

                // 3. Main Season Lock: Allow main seasons, movies, and OVAs.
                // Exclude generic specials (-5.0) to keep the seasons bar focused.
                val seasonNum = SeasonRecognition.parseSeasonNumber(anime.title, candidateFullTitle)
                if (seasonNum < -4.0) return@filter false
                
                true
            }.take(10)

            val verified = candidates
                .filter { it.url.trimEnd('/') != anime.url.trimEnd('/') } // Filter out the current anime
                .map { it.toDomainAnime(anime.source) }
                .distinctBy { it.url.trimEnd('/') } // Deduplicate by URL instead of placeholder ID

            verified.sortedBy { sAnime ->
                SeasonRecognition.parseSeasonNumber(anime.title, sAnime.title)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
