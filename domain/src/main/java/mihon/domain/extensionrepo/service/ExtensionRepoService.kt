package mihon.domain.extensionrepo.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

class ExtensionRepoService(
    networkHelper: NetworkHelper,
    private val json: Json,
) {
    val client = networkHelper.client

    suspend fun fetchRepoDetails(
        repo: String,
    ): ExtensionRepo? {
        return withIOContext {
            try {
                val response = client.newCall(GET("$repo/repo.json")).awaitSuccess()
                val responseText = response.body.string().trim().removePrefix("\uFEFF")
                response.close()
                json.decodeFromString<ExtensionRepoMetaDto>(responseText).toExtensionRepo(baseUrl = repo)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch repo.json details for $repo, trying fallback" }
                fetchFallbackRepoDetails(repo)
            }
        }
    }

    private suspend fun fetchFallbackRepoDetails(
        repo: String,
    ): ExtensionRepo? {
        return try {
            val response = client.newCall(GET("$repo/index.min.json")).awaitSuccess()
            val isSuccess = response.isSuccessful
            response.close()
            if (isSuccess) {
                val repoName = repo.substringAfter("://").substringBefore("/")
                val website = if (repo.contains("/raw")) repo.substringBefore("/raw") else repo
                ExtensionRepo(
                    baseUrl = repo,
                    name = repoName,
                    shortName = null,
                    website = website,
                    signingKeyFingerprint = "NOFINGERPRINT_${repo.hashCode()}",
                    isVisible = true,
                )
            } else null
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch fallback index for $repo" }
            null
        }
    }
}