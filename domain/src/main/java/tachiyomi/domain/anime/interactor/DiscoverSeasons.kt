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
                
                true
            }.take(10)

            val mapped = candidates.map { sAnime ->
                val domainAnime = sAnime.toDomainAnime(anime.source)
                val seasonNum = SeasonRecognition.parseSeasonNumber(anime.title, domainAnime.title)
                domainAnime to seasonNum
            }

            // Deduplication: If multiple entries claim the same season number, 
            // pick the one that is most likely the "Main" entry.
            val uniqueSeasons = mapped.groupBy { it.second }
                .map { (num, matches) ->
                    matches.minByOrNull { (anime, _) ->
                        // Priority Score: Lower is better
                        var score = anime.title.length // Shorter titles are usually cleaner
                        
                        // Boost if title contains the actual number/roman numeral
                        val hasExplicitNumber = anime.title.contains(Regex("""\b(?i)(?:Season|S|Part|Cour|Vol)\s*\d+\b""")) ||
                                               anime.title.contains(Regex("""\b(?i)(?:I|II|III|IV|V|VI|VII|VIII|IX|X)\b"""))
                        
                        if (hasExplicitNumber) score -= 100 
                        
                        score
                    }!!.first
                }

            uniqueSeasons.sortedBy { sAnime ->
                SeasonRecognition.parseSeasonNumber(anime.title, sAnime.title)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
