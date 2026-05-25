package eu.kanade.tachiyomi.animesource.model

import java.io.Serializable

data class Credit(
    val name: String,
    val role: String? = null,
    val character: String? = null,
    val image_url: String? = null,
) : Serializable
