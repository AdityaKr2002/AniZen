package eu.kanade.tachiyomi.util.episode

import tachiyomi.domain.episode.model.Episode

object EpisodeSeasonUtils {
    private val seasonRegex = Regex("""(?i)(?:^|\b|\s|\[)(?:s|season\s*)(\d+)(?:\s|e|x|\||-|\.|\b|\]|$)""")
    private val specialKeywordsRegex = Regex("""(?i)\b(special|ova|ona|movie|pv|trailer|extra|bonus|recap|summary|prologue)\b""")

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
     * Checks if the episode is likely a special content.
     */
    fun isSpecial(episode: Episode): Boolean {
        // Not a recognized number or contains special keywords
        return episode.episodeNumber < 0 || specialKeywordsRegex.containsMatchIn(episode.name)
    }

    /**
     * Comparator to sort season names naturally (Season 1, Season 2, Season 10, Specials).
     */
    val SeasonComparator = Comparator<String> { s1, s2 ->
        val isSpecial1 = s1.contains("Special", ignoreCase = true)
        val isSpecial2 = s2.contains("Special", ignoreCase = true)
        
        if (isSpecial1 && !isSpecial2) return@Comparator 1
        if (!isSpecial1 && isSpecial2) return@Comparator -1
        
        val n1 = s1.filter { it.isDigit() }.toIntOrNull() ?: 0
        val n2 = s2.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (n1 != n2) {
            n1.compareTo(n2)
        } else {
            s1.compareTo(s2)
        }
    }
}
