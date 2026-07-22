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
    author: String? = null,
): ExtensionRepo {
    return meta.toExtensionRepo(baseUrl, author)
}

fun ExtensionRepoDto.toExtensionRepo(
    baseUrl: String,
    author: String? = null,
): ExtensionRepo {
    return ExtensionRepo(
        baseUrl = baseUrl,
        name = name,
        shortName = shortName,
        website = website,
        signingKeyFingerprint = signingKeyFingerprint.trim().lowercase().padStart(64, '0'),
        isVisible = true,
        author = author ?: this.author,
    )
}