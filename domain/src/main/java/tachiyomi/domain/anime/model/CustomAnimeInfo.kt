package tachiyomi.domain.anime.model

import java.io.Serializable

data class CustomAnimeInfo(
    val id: Long,
    val title: String?,
    val author: String? = null,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
    val score: Double? = null,
    val note: String? = null,
) : Serializable
