package mihon.domain.extensionrepo.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.domain.extensionrepo.model.ExtensionRepo
import okhttp3.CacheControl
import okhttp3.Headers
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

private val REPO_HEADERS = Headers.Builder()
    .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0")
    .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .add("Accept-Language", "en-US,en;q=0.5")
    .build()

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
                val request = GET("$repo/repo.json", headers = REPO_HEADERS, cache = CacheControl.FORCE_NETWORK)
                val response = client.newCall(request).awaitSuccess()
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
            val request = GET("$repo/index.min.json", headers = REPO_HEADERS, cache = CacheControl.FORCE_NETWORK)
            val response = client.newCall(request).awaitSuccess()
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