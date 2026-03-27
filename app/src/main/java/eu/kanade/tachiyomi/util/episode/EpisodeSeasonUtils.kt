package eu.kanade.tachiyomi.util.episode

import tachiyomi.domain.episode.model.Episode

object EpisodeSeasonUtils {
    private val seasonRegex = Regex("""(?i)(?:^|\b|\s|\[)(?:s|season\s*)(\d+)(?:\s|e|x|\||-|\.|\b|\]|$)""")
    private val volumeRegex = Regex("""(?i)(?:^|\b|\s|\[)(?:vol|volume\s*)(\d+)(?:\s|e|x|\||-|\.|\b|\]|$)""")
    private val specialKeywordsRegex = Regex("""(?i)\b(special|ova|ona|movie|pv|trailer|extra|bonus|recap|summary|prologue)\b""")
    private val volumeKeywordsRegex = Regex("""(?i)\b(vol|volume)\b""")

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
     * Extracts volume number from episode name.
     * Returns "Volume X" if found.
     */
    fun getVolumeName(episode: Episode): String? {
        val name = episode.name
        val match = volumeRegex.find(name)
        return if (match != null) {
            val volNumber = match.groupValues[1].toIntOrNull()
            if (volNumber != null) "Volume $volNumber" else null
        } else {
            null
        }
    }

    /**
     * Checks if the episode name contains volume keywords.
     */
    fun hasVolumeKeywords(episode: Episode): Boolean {
        return volumeKeywordsRegex.containsMatchIn(episode.name) || getVolumeName(episode) != null
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
     * Checks if the episode is likely a non-standard content (Special, Volume, or Extra).
     */
    fun isSpecial(episode: Episode): Boolean {
        // Contains special/volume keywords, or is Season 0, or is unrecognized with negative number, or name has no digits
        return episode.episodeNumber < 0 || hasSpecialKeywords(episode) || hasVolumeKeywords(episode) || !hasDigits(episode.name) || isSeasonZero(episode)
    }

    /**
     * Comparator to sort section names naturally.
     * Priority: Seasons > Specials > Volumes > Extras.
     */
    val SeasonComparator = Comparator<String> { s1, s2 ->
        fun getPriority(s: String): Int {
            return when {
                s.startsWith("Season", ignoreCase = true) -> 0
                s.contains("Special", ignoreCase = true) -> 1
                s.startsWith("Volume", ignoreCase = true) || s.equals("Volumes", ignoreCase = true) -> 2
                s.contains("Extra", ignoreCase = true) -> 3
                else -> 4
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
