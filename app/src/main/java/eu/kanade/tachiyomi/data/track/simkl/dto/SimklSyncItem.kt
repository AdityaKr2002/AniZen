package eu.kanade.tachiyomi.data.track.simkl.dto

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.simkl.toTrackStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimklSyncResult(
    val anime: List<SimklSyncItem>? = null,
    val tv: List<SimklSyncItem>? = null,
    val shows: List<SimklSyncItem>? = null,
    val movies: List<SimklSyncItem>? = null,
) {
    fun getFromType(type: String): List<SimklSyncItem>? {
        return when (type) {
            "anime" -> anime
            "tv" -> tv
            "movies" -> movies
            "shows" -> shows ?: tv
            else -> throw Exception("Unknown type: $type")
        }
    }
}

@Serializable
data class SimklSyncItem(
    val show: SimklSyncResultItem? = null,
    val movie: SimklSyncResultItem? = null,
    val status: String? = null,
    @SerialName("total_episodes_count")
    val totalEpisodesCount: Long? = null,
    @SerialName("watched_episodes_count")
    val watchedEpisodesCount: Double? = null,
    @SerialName("user_rating")
    val userRating: Int? = null,
) {
    fun toAnimeTrack(typeName: String, type: String, statusString: String): Track {
        val resultData = getFromType(typeName)

        return Track.create(TrackerManager.SIMKL).apply {
            title = resultData.title
            remote_id = resultData.ids.simkl
            if (typeName != "movie") {
                total_episodes = totalEpisodesCount!!
                last_episode_seen = watchedEpisodesCount!!
            } else {
                total_episodes = 1
                last_episode_seen = if (statusString == "completed") 1.0 else 0.0
            }
            score = userRating?.toDouble() ?: 0.0
            status = toTrackStatus(statusString)
            tracking_url = "/$type/${resultData.ids.simkl}"
        }
    }

    fun getFromType(typeName: String): SimklSyncResultItem {
        return when (typeName) {
            "show" -> show!!
            "movie" -> movie!!
            else -> throw Exception("Unknown type: $typeName")
        }
    }
}

@Serializable
data class SimklSyncResultItem(
    val title: String,
    val poster: String? = null,
    val ids: SimklSyncResultIds,
)

@Serializable
data class SimklSyncResultIds(
    val simkl: Long,
)
