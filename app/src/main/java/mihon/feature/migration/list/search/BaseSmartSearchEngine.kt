package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.util.lang.StringSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.util.Locale

typealias SearchAction<T> = suspend (String) -> List<T>

abstract class BaseSmartSearchEngine<T>(
    private val extraSearchParams: String? = null,
    private val eligibleThreshold: Double = MIN_ELIGIBLE_THRESHOLD,
) {

    protected abstract fun getTitle(result: T): String

    protected suspend fun regularSearch(searchAction: SearchAction<T>, title: String): T? {
        return baseSearch(searchAction, listOf(title)) {
            StringSimilarity.levenshteinSimilarity(title, getTitle(it))
        }
    }

    protected suspend fun deepSearch(searchAction: SearchAction<T>, title: String): T? {
        val cleanedTitle = cleanDeepSearchTitle(title)

        val queries = getDeepSearchQueries(cleanedTitle)

        return baseSearch(searchAction, queries) {
            val cleanedAnimeTitle = cleanDeepSearchTitle(getTitle(it))
            StringSimilarity.levenshteinSimilarity(cleanedTitle, cleanedAnimeTitle)
        }
    }

    private suspend fun baseSearch(
        searchAction: SearchAction<T>,
        queries: List<String>,
        calculateDistance: (T) -> Double,
    ): T? {
        val eligibleAnime = supervisorScope {
            queries.map { query ->
                async(Dispatchers.Default) {
                    val builtQuery = if (!extraSearchParams.isNullOrBlank()) {
                        "$query ${extraSearchParams.trim()}"
                    } else {
                        query
                    }

                    val candidates = searchAction(
                        builtQuery.sanitize(),
                    )
                    candidates
                        .map {
                            val distance = if (queries.size > 1 || candidates.size > 1) {
                                calculateDistance(it)
                            } else {
                                1.0
                            }
                            SearchEntry(it, distance)
                        }
                        .filter { it.distance >= eligibleThreshold }
                }
            }
                .flatMap { it.await() }
        }

        return eligibleAnime.maxByOrNull { it.distance }?.entry
    }

    private fun cleanDeepSearchTitle(title: String): String {
        val preTitle = title.lowercase(Locale.getDefault())

        // Remove text in brackets
        var cleanedTitle = removeTextInBrackets(preTitle, true)
        if (cleanedTitle.length <= 5) { // Title is suspiciously short, try parsing it backwards
            cleanedTitle = removeTextInBrackets(preTitle, false)
        }

        // Strip non-special characters
        val cleanedTitleEng = cleanedTitle.replace(titleRegex, " ")

        // Do not strip foreign language letters if cleanedTitle is too short
        cleanedTitle = if (cleanedTitleEng.length <= 5) {
            cleanedTitle.replace(titleCyrillicRegex, " ")
        } else {
            cleanedTitleEng
        }

        // Strip splitters and consecutive spaces
        cleanedTitle = cleanedTitle.trim().replace(" - ", " ").replace(consecutiveSpacesRegex, " ").trim()

        return cleanedTitle
    }

    private fun removeTextInBrackets(text: String, readForward: Boolean): String {
        val openingChars = if (readForward) "([<{" else ")]}>"
        val closingChars = if (readForward) ")]}>" else "([<{"
        var depth = 0

        val result = buildString {
            for (char in (if (readForward) text else text.reversed())) {
                when {
                    char in openingChars -> depth++
                    char in closingChars && depth > 0 -> {
                        depth--
                    }
                    else -> if (depth == 0) {
                        append(char)
                    }
                }
            }
        }
        return if (readForward) result else result.reversed()
    }

    private fun getDeepSearchQueries(cleanedTitle: String): List<String> {
        val splitCleanedTitle = cleanedTitle.split(" ")
        val splitSortedByLargest = splitCleanedTitle.sortedByDescending { it.length }

        if (splitCleanedTitle.isEmpty()) {
            return emptyList()
        }

        val searchQueries = listOf(
            listOf(cleanedTitle),
            splitSortedByLargest.take(2),
            splitSortedByLargest.take(1),
            splitCleanedTitle.take(2),
            splitCleanedTitle.take(1),
        )

        return searchQueries
            .map { it.joinToString(" ").trim() }
            .distinct()
    }

    private fun String.sanitize(): String {
        return this.replace(sanitizeRegex, "")
    }

    companion object {
        const val MIN_ELIGIBLE_THRESHOLD = 0.4

        private val titleRegex = Regex("[^a-zA-Z0-9- ]")
        private val titleCyrillicRegex = Regex("[^\\p{L}0-9- ]")
        private val consecutiveSpacesRegex = Regex(" +")
        private val sanitizeRegex = Regex("[^\\p{L}0-9 ]")
    }
}

data class SearchEntry<T>(val entry: T, val distance: Double)
