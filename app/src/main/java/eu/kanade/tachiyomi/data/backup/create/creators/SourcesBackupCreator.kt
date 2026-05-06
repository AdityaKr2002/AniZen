package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesBackupCreator(
    private val sourceManager: SourceManager = Injekt.get(),
) {

    operator fun invoke(animes: List<BackupAnime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map(BackupAnime::source)
            .distinct()
            .map(sourceManager::getOrStub)
            .map { it.toBackupAnimeSource() }
            .toList()
    }
}

private fun Source.toBackupAnimeSource() =
    BackupAnimeSource(
        name = this.name,
        sourceId = this.id,
    )
