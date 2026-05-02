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

                // 2. Similarity Check (Dice Coefficient)
                // We accept > 0.4 because sequels often add many words ("...Season 2 Part 3")
                // which lowers the score against the short root title.
                val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateFullTitle)
                if (similarity < 0.4 && !candidateFullTitle.contains(rootTitle, ignoreCase = true)) return@filter false
                
                true
            }.take(10)

            candidates.map { it.toDomainAnime(anime.source) }
                .sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
