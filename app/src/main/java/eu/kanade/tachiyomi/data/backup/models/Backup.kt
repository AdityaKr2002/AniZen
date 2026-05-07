package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class LegacyBackup(
    @ProtoNumber(1) val backupManga: List<BackupManga> = emptyList(),
    @ProtoNumber(2) var backupCategories: List<BackupCategory> = emptyList(),
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    // @ProtoNumber(100) var backupBrokenSources, legacy source model with non-compliant proto number,
    @ProtoNumber(101) var backupSources: List<BackupSource> = emptyList(),
    // @ProtoNumber(102) var backupBrokenAnimeSources, legacy source model with non-compliant proto number,
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
    @ProtoNumber(107) var backupAnimeExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(108) var backupMangaExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(109) var backupCustomButton: List<BackupCustomButtons> = emptyList(),
) {
    fun toBackup(): Backup {
        return Backup(
            backupManga = backupManga,
            backupCategories = backupCategories,
            backupAnime = backupAnime,
            backupAnimeCategories = backupAnimeCategories,
            backupSources = backupSources,
            backupAnimeSources = backupAnimeSources,
            backupPreferences = backupPreferences,
            backupSourcePreferences = backupSourcePreferences,
            backupExtensions = backupExtensions,
            backupAnimeExtensionRepo = backupAnimeExtensionRepo,
            backupMangaExtensionRepo = backupMangaExtensionRepo,
            backupCustomButton = backupCustomButton,

            isLegacy = false,
        )
    }
}

@Serializable
data class Backup(
    @ProtoNumber(1) val backupManga: List<BackupManga> = emptyList(),
    @ProtoNumber(2) var backupCategories: List<BackupCategory> = emptyList(),
    @ProtoNumber(3) var backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),

    @ProtoNumber(101) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
    @ProtoNumber(107) var backupAnimeExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(108) var backupMangaExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(109) var backupCustomButton: List<BackupCustomButtons> = emptyList(),

    // Anizen specific values (Legacy Anizen format range)
    @ProtoNumber(500) val isLegacy: Boolean = true,
    @ProtoNumber(501) val backupAnimeAnizen: List<BackupAnime> = emptyList(),
    @ProtoNumber(502) var backupAnimeCategoriesAnizen: List<BackupCategory> = emptyList(),
    @ProtoNumber(503) var backupAnimeSourcesAnizen: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(504) var backupExtensionsAnizen: List<BackupExtension> = emptyList(),
    @ProtoNumber(505) var backupAnimeExtensionRepoAnizen: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(506) var backupCustomButtonAnizen: List<BackupCustomButtons> = emptyList(),
)
