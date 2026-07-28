package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Overrides the User-Agent for requests to git forges (Codeberg, GitLab) that block
 * custom/app User-Agents with HTTP 403. This covers both JSON metadata and icon image fetches.
 */
object GitForgeInterceptor : Interceptor {

    private val GIT_FORGE_HOSTS = setOf(
        "codeberg.org",
        "gitlab.com",
    )

    private const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        return if (GIT_FORGE_HOSTS.any { host == it || host.endsWith(".$it") }) {
            chain.proceed(
                request.newBuilder()
                    .header("User-Agent", BROWSER_UA)
                    .header("Accept", "*/*")
                    .build(),
            )
        } else {
            chain.proceed(request)
        }
    }
}
