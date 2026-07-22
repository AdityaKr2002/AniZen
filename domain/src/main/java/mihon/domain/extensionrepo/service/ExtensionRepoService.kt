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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class ExtensionRepoService(
    networkHelper: NetworkHelper,
    private val json: Json,
) {
    val client = networkHelper.client

    suspend fun fetchRepoDetails(
        repo: String,
        author: String? = null,
    ): ExtensionRepo? {
        return withIOContext {
            try {
                val responseText = client.newCall(GET("$repo/repo.json"))
                    .awaitSuccess()
                    .body.string()

                val jsonElement = json.parseToJsonElement(responseText)
                val repoDto = if (jsonElement is JsonObject && "meta" in jsonElement) {
                    json.decodeFromJsonElement<ExtensionRepoDto>(jsonElement.jsonObject["meta"]!!)
                } else {
                    json.decodeFromJsonElement<ExtensionRepoDto>(jsonElement)
                }
                repoDto.toExtensionRepo(baseUrl = repo, author = author)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch repo details for $repo" }
                null
            }
        }
    }
}