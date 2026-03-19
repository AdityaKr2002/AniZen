package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.CustomAnimeRepository

class GetCustomAnimeInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun get(mangaId: Long) = customAnimeRepository.get(mangaId)

    fun subscribe(mangaId: Long) = customAnimeRepository.subscribe(mangaId)
}
