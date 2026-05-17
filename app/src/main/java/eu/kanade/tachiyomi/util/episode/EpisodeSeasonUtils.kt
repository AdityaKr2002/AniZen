package eu.kanade.tachiyomi.util.episode

import tachiyomi.domain.episode.model.Episode

object EpisodeSeasonUtils {
    // Added |_ to delimiters to support patterns like _s1e1_
    private val seasonRegex = Regex("""(?i)(?:^|\b|\s|\[|_)(?:s|season\s*)(\d+)(?:\s|e|x|\||-|\.|\b|\]|_|$)""")
    private val specialKeywordsRegex = Regex("""(?i)\b(ova|ona|movie|pv|trailer|bonus|recap|summary|prologue|extra|special|omake|teaser|clip|interview|preview)\b""")


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
     * Checks if the episode name contains special keywords.
     */
    fun hasSpecialKeywords(episode: Episode): Boolean {
        return specialKeywordsRegex.containsMatchIn(episode.name)
    }

    /**
     * Checks if the episode name contains digits.
     */
    fun hasDigits(s: String): Boolean {
        return s.any { it.isDigit() }
    }

    /**
     * Checks if the episode is from Season 0.
     * Also detects 0.x numbering (e.g., 0.1, 0.2) used by many extensions for specials.
     */
    fun isSeasonZero(episode: Episode): Boolean {
        val hasSeasonZeroName = getSeasonName(episode) == "Season 0"
        val hasSeasonZeroNumber = episode.episodeNumber > 0 && episode.episodeNumber < 1.0
        return hasSeasonZeroName || hasSeasonZeroNumber
    }

    /**
     * Checks if the episode is likely a non-standard content (Special or Extra).
     */
    fun isSpecial(episode: Episode): Boolean {
        // Contains special keywords, or is Season 0, or is unrecognized with negative number, or name has no digits
        return episode.episodeNumber < 0 || hasSpecialKeywords(episode) || !hasDigits(episode.name) || isSeasonZero(episode)
    }

    /**
     * Comparator to sort section names naturally.
     * Priority: Seasons > Specials > Extras.
     */
    val SeasonComparator = Comparator<String> { s1, s2 ->
        fun getPriority(s: String): Int {
            return when {
                s.startsWith("Season", ignoreCase = true) -> 0
                s.contains("Special", ignoreCase = true) -> 1
                s.contains("Extra", ignoreCase = true) -> 2
                else -> 3
            }
        }

        val p1 = getPriority(s1)
        val p2 = getPriority(s2)

        if (p1 != p2) return@Comparator p1.compareTo(p2)

        val n1 = s1.filter { it.isDigit() }.toIntOrNull() ?: 0
        val n2 = s2.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (n1 != n2) {
            n1.compareTo(n2)
        } else {
            s1.compareTo(s2)
        }
    }
}
