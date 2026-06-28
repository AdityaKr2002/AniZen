package eu.kanade.tachiyomi.data.track.model

data class TrackAnimeMetadata(
    val remoteId: Long,
    val title: String,
    val thumbnailUrl: String,
    val description: String?,
    val author: String?,
    val artist: String?,
    val genres: List<String>? = null,
)
