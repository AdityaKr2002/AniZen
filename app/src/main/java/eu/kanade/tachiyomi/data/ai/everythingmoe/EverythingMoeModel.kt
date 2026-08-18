package eu.kanade.tachiyomi.data.ai.everythingmoe

import kotlinx.serialization.Serializable

@Serializable
data class EverythingMoeSite(
    val slug: String,
    val name: String,
    val url: String = "",
    val icon: String = "",
    val tags: List<String> = emptyList(),
    val mirrors: List<String> = emptyList(),
    val extraLinks: List<String> = emptyList(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val info: String? = null,
    val rank: String = "",
    val category: String = "",
    val description: String? = null,
    val isDead: Boolean = false,
    val deadReason: String? = null,
    val reviewCount: Int = 0,
    val reviewVoteSum: Int = 0,
    val reviews: List<EverythingMoeReview> = emptyList(),
)

@Serializable
data class EverythingMoeReview(
    val name: String? = null,
    val review: String? = null,
    val time: Long? = null,
    val vote: Int? = null,
)

@Serializable
data class EverythingMoeCache(
    val lastFetchedTimestamp: Long = 0L,
    val sites: List<EverythingMoeSite> = emptyList(),
)
