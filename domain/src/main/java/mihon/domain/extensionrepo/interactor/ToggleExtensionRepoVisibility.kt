package mihon.domain.extensionrepo.interactor

import mihon.domain.extensionrepo.repository.ExtensionRepoRepository

class ToggleExtensionRepoVisibility(
    private val repository: ExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String, isVisible: Boolean) {
        repository.updateRepoVisibility(baseUrl, isVisible)
    }
}
