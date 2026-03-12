package mihon.domain.extensionrepo.model

data class ExtensionRepo(
    val baseUrl: String,
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
    val isVisible: Boolean,
    val author: String? = null,
)
