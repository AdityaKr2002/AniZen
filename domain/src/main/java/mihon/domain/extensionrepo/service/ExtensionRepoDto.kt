package mihon.domain.extensionrepo.service

import kotlinx.serialization.Serializable
import mihon.domain.extensionrepo.model.ExtensionRepo

@Serializable
data class ExtensionRepoMetaDto(
    val meta: ExtensionRepoDto,
)

@Serializable
data class ExtensionRepoDto(
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
    val author: String? = null,
)

fun ExtensionRepoMetaDto.toExtensionRepo(
    baseUrl: String,
): ExtensionRepo {
    return ExtensionRepo(
        baseUrl = baseUrl,
        name = meta.name,
        shortName = meta.shortName,
        website = meta.website,
        signingKeyFingerprint = meta.signingKeyFingerprint.trim().lowercase().padStart(64, '0'),
        isVisible = true,
        author = meta.author,
    )
}