package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.episode.model.Episode

@Serializable
data class BackupEpisode(
    // in 1.x some of these values have different names
    // url is called key in 1.x
    @EncodeDefault
    @ProtoNumber(1) var url: String,
    @EncodeDefault
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var seen: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,

    // AM (FILLERMARK) -->
    @Transient
    var fillermark: Boolean = false,
    // <-- AM (FILLERMARK)

    // lastPageRead is called progress in 1.x
    @ProtoNumber(6) var lastSecondSeen: Long = 0,
    @ProtoNumber(16) var totalSeconds: Long = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    // episodeNumber is called number is 1.x
    @ProtoNumber(9) var episodeNumber: Float = 0F,
    @ProtoNumber(10) var sourceOrder: Long = 0,

    @Transient
    var summary: String? = null,
    @Transient
    var previewUrl: String? = null,

    @ProtoNumber(11) var lastModifiedAt: Long = 0,
    @ProtoNumber(12) var version: Long = 0,

    // AY -->
    @ProtoNumber(501) var fillermarkAY: Boolean = false,
    @ProtoNumber(502) var summaryAY: String? = null,
    @ProtoNumber(503) var previewUrlAY: String? = null,
    // <-- AY
) {
    fun toEpisodeImpl(): Episode {
        return Episode.create().copy(
            url = this@BackupEpisode.url,
            name = this@BackupEpisode.name,
            episodeNumber = this@BackupEpisode.episodeNumber.toDouble(),
            scanlator = this@BackupEpisode.scanlator,
            summary = this@BackupEpisode.summaryAY,
            previewUrl = this@BackupEpisode.previewUrlAY,
            seen = this@BackupEpisode.seen,
            bookmark = this@BackupEpisode.bookmark,
            fillermark = this@BackupEpisode.fillermarkAY,
            lastSecondSeen = this@BackupEpisode.lastSecondSeen,
            totalSeconds = this@BackupEpisode.totalSeconds,
            dateFetch = this@BackupEpisode.dateFetch,
            dateUpload = this@BackupEpisode.dateUpload,
            sourceOrder = this@BackupEpisode.sourceOrder,
            lastModifiedAt = this@BackupEpisode.lastModifiedAt,
            version = this@BackupEpisode.version,
        )
    }
}

val backupEpisodeMapper = {
        _: Long,
        _: Long,
        url: String,
        name: String,
        scanlator: String?,
        seen: Boolean,
        bookmark: Boolean,
        fillermark: Boolean,
        lastSecondSeen: Long,
        totalSeconds: Long,
        episodeNumber: Double,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        summary: String?,
        previewUrl: String?,
        lastModifiedAt: Long,
        version: Long,
    ->
    BackupEpisode(
        url = url,
        name = name,
        episodeNumber = episodeNumber.toFloat(),
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        fillermarkAY = fillermark,
        summaryAY = summary,
        previewUrlAY = previewUrl,
    )
}
