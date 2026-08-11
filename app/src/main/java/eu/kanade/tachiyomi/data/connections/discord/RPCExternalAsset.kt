package eu.kanade.tachiyomi.data.connections.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RPCExternalAsset(
    applicationId: String,
    private val token: String,
    private val client: OkHttpClient,
    private val json: Json,
) {

    @Serializable
    data class ExternalAsset(
        val url: String? = null,
        @SerialName("external_asset_path")
        val externalAssetPath: String? = null,
    )

    private val api = "https://discord.com/api/v9/applications/$applicationId/external-assets"

    companion object {
        private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

    suspend fun getDiscordUri(imageUrl: String): String? {
        if (imageUrl.isBlank() || imageUrl.startsWith("mp:")) return imageUrl.takeIf { it.isNotBlank() }
        cache[imageUrl]?.let { return it }

        val request = Request.Builder().url(api).header("Authorization", token)
            .post("{\"urls\":[\"$imageUrl\"]}".toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            val res = client.newCall(request).await()
            res.use { response ->
                if (!response.isSuccessful) return@runCatching null
                json.decodeFromString<List<ExternalAsset>>(response.body.string())
                    .firstOrNull()?.externalAssetPath?.let { "mp:$it" }
                    ?.also { cache[imageUrl] = it }
            }
        }.getOrNull()
    }

    private suspend inline fun Call.await(): Response {
        return suspendCoroutine {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    it.resume(response)
                }
            })
        }
    }
}
