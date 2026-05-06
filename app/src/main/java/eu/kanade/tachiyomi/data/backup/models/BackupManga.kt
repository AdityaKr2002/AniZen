package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupManga(
    @ProtoNumber(1) var source: Long = 0,
    @ProtoNumber(2) var url: String = "",
    @ProtoNumber(3) var title: String = "",
)
