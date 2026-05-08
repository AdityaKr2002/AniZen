package tachiyomi.domain.season.interactor

import tachiyomi.domain.anime.model.Anime

class ShouldUpdateDbSeason {
    fun await(dbSeason: Anime, sourceSeason: Anime): Boolean {
        return dbSeason.ogTitle != sourceSeason.ogTitle ||
            dbSeason.seasonNumber != sourceSeason.seasonNumber ||
            dbSeason.seasonOrder != sourceSeason.seasonOrder ||
            dbSeason.backgroundUrl != sourceSeason.backgroundUrl
    }
}
