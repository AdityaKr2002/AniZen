package eu.kanade.tachiyomi.util.episode

import tachiyomi.domain.episode.model.Episode

object EpisodeSeasonUtils {
    private val seasonRegex = Regex("""(?i)(?:^|\b|\s|\[)(?:s|season\s*)(\d+)(?:\s|e|x|\.|\b|\]|$)""")

    /**
     * Extracts season number from episode name.
     * Returns "Season X" if found.
     */
    fun getSeasonName(episode: Episode): String? {
        val name = episode.name
        val match = seasonRegex.find(name)
        return if (match != null) {
            val seasonNumber = match.groupValues[1].toIntOrNull()
            if (seasonNumber != null) "Season $seasonNumber" else null
        } else {
            null
        }
    }

    /**
     * Comparator to sort season names naturally (Season 1, Season 2, Season 10).
     */
    val SeasonComparator = Comparator<String> { s1, s2 ->
        val n1 = s1.filter { it.isDigit() }.toIntOrNull() ?: 0
        val n2 = s2.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (n1 != n2) {
            n1.compareTo(n2)
        } else {
            s1.compareTo(s2)
        }
    }
}
