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
}
