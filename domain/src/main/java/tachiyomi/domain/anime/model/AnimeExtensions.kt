package tachiyomi.domain.anime.model

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.model.UpdateStrategy

fun SAnime.toDomainAnime(sourceId: Long): Anime {
    return Anime.create().copy(
        url = url,
        ogTitle = title,
        source = sourceId,
        ogArtist = artist,
        ogAuthor = author,
        ogThumbnailUrl = thumbnail_url,
        ogDescription = description,
        ogGenre = genre?.split(", ")?.map { it.trim() },
        ogStatus = status.toLong(),
        initialized = initialized,
        updateStrategy = UpdateStrategy.ALWAYS_UPDATE, // Default
    )
}

fun Anime.isLocal(): Boolean = source == 0L
