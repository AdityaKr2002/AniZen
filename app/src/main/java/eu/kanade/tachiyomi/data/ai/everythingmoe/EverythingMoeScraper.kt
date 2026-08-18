package eu.kanade.tachiyomi.data.ai.everythingmoe

import android.content.Context
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class EverythingMoeScraper(
    private val context: Context,
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val json: Json = Injekt.get(),
) {
    private val cacheMutex = Mutex()
    private val siteMemoryCache = ConcurrentHashMap<String, EverythingMoeSite>()
    private var lastCacheFetch: Long = 0L

    private val cacheFile: File by lazy {
        File(context.cacheDir, "everythingmoe_cache.json")
    }

    companion object {
        private const val BASE_URL = "https://everythingmoe.com"
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    init {
        loadDiskCache()
    }

    private fun loadDiskCache() {
        try {
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                val cached = json.decodeFromString(EverythingMoeCache.serializer(), content)
                if (cached.sites.isNotEmpty()) {
                    cached.sites.forEach { site ->
                        siteMemoryCache[site.slug] = site
                    }
                    lastCacheFetch = cached.lastFetchedTimestamp
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Failed to load EverythingMoe disk cache: ${e.message}" }
        }
    }

    private fun saveDiskCache() {
        try {
            val cacheObj = EverythingMoeCache(
                lastFetchedTimestamp = lastCacheFetch,
                sites = siteMemoryCache.values.toList(),
            )
            cacheFile.writeText(json.encodeToString(EverythingMoeCache.serializer(), cacheObj))
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Failed to save EverythingMoe disk cache: ${e.message}" }
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val clean = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val uri = URI(clean)
            val host = uri.host ?: ""
            host.removePrefix("www.").lowercase().trim()
        } catch (e: Exception) {
            url.substringAfter("://").substringBefore("/").removePrefix("www.").lowercase().trim()
        }
    }

    private fun unpackAlts(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val out = mutableListOf<String>()
        val parts = value.split("#")
        for (rawPart in parts) {
            var part = rawPart.trim()
            if (part.isEmpty()) continue
            if (part.contains("<<")) {
                part = part.substringAfter("<<")
            }
            if (part.startsWith("http://") || part.startsWith("https://")) {
                out.add(part)
            }
        }
        return out
    }

    suspend fun getSiteBySlug(slug: String): EverythingMoeSite? = withIOContext {
        siteMemoryCache[slug]?.let { return@withIOContext it }

        try {
            val request = Request.Builder()
                .url("$BASE_URL/s/$slug")
                .header("User-Agent", USER_AGENT)
                .build()

            val timedClient = networkHelper.client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            timedClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withIOContext null
                val html = response.body.string()
                val site = parseSitePage(slug, html)
                if (site != null) {
                    siteMemoryCache[site.slug] = site
                    saveDiskCache()
                }
                site
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Failed to fetch EverythingMoe site page for $slug: ${e.message}" }
            null
        }
    }

    private fun parseSitePage(slug: String, html: String): EverythingMoeSite? {
        val marker = "var siteData = "
        val idx = html.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val jsonEndMarker = "\n"
        val rawJsonCandidate = html.substring(start).substringBefore(";</script>").substringBefore(";\n").trim()

        val jsonObject: JsonObject = try {
            json.parseToJsonElement(rawJsonCandidate).jsonObject
        } catch (e: Exception) {
            return null
        }

        val title = jsonObject["title"]?.jsonPrimitive?.content ?: slug
        val link = jsonObject["link"]?.jsonPrimitive?.content ?: ""
        val icon = jsonObject["icon"]?.jsonPrimitive?.content ?: ""
        val filterRaw = jsonObject["filter"]?.jsonPrimitive?.content ?: ""
        val tags = filterRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val altLink = jsonObject["ex-altlink"]?.jsonPrimitive?.content
        val altLink2 = jsonObject["ex-altlink2"]?.jsonPrimitive?.content
        val extraLink = jsonObject["extra-link"]?.jsonPrimitive?.content
        val mirrors = unpackAlts(altLink2) + unpackAlts(altLink)
        val extraLinks = unpackAlts(extraLink)

        val rank = jsonObject["rank"]?.jsonPrimitive?.content ?: ""
        val category = jsonObject["type"]?.jsonPrimitive?.content ?: ""

        val deadPrimitive = jsonObject["DEAD"]?.jsonPrimitive
        val isDead = deadPrimitive != null && deadPrimitive.content.isNotBlank() && deadPrimitive.content != "false"
        val deadReason = if (isDead) deadPrimitive?.content else null

        val reviewsList = mutableListOf<EverythingMoeReview>()
        val rawReviews = jsonObject["reviews"]?.let {
            if (it is JsonArray) it else null
        }
        if (rawReviews != null) {
            for (element in rawReviews) {
                if (element is JsonObject) {
                    val rName = element["name"]?.jsonPrimitive?.content
                    val rReview = element["review"]?.jsonPrimitive?.content
                    val rTime = element["time"]?.jsonPrimitive?.longOrNull
                    val rVote = element["vote"]?.jsonPrimitive?.intOrNull
                    reviewsList.add(
                        EverythingMoeReview(
                            name = rName,
                            review = rReview,
                            time = rTime,
                            vote = rVote,
                        )
                    )
                }
            }
        }

        val metaDescRegex = """<meta\s+name="description"\s+content="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
        val description = metaDescRegex.find(html)?.groupValues?.getOrNull(1)?.trim()

        return EverythingMoeSite(
            slug = slug,
            name = title,
            url = link,
            icon = icon,
            tags = tags,
            mirrors = mirrors,
            extraLinks = extraLinks,
            rank = rank,
            category = category,
            description = description,
            isDead = isDead,
            deadReason = deadReason,
            reviewCount = reviewsList.size,
            reviewVoteSum = reviewsList.sumOf { it.vote ?: 0 },
            reviews = reviewsList.take(5),
        )
    }

    suspend fun refreshDirectoryIfNeeded(): Unit = withIOContext {
        val now = System.currentTimeMillis()
        if (siteMemoryCache.isNotEmpty() && (now - lastCacheFetch < CACHE_TTL_MS)) {
            return@withIOContext
        }

        cacheMutex.withLock {
            if (siteMemoryCache.isNotEmpty() && (now - lastCacheFetch < CACHE_TTL_MS)) {
                return@withLock
            }

            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val timedClient = networkHelper.client.newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()

                timedClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withLock
                    val html = response.body.string()
                    
                    // Find all /s/<slug> links in directory
                    val slugRegex = """href="/s/([a-zA-Z0-9_-]+)"""".toRegex()
                    val slugs = slugRegex.findAll(html).map { it.groupValues[1] }.distinct().toList()

                    // Quick-parse embedded data if any, or seed known slugs
                    for (slug in slugs) {
                        if (!siteMemoryCache.containsKey(slug)) {
                            // Seed placeholder with slug until details are fetched
                            siteMemoryCache[slug] = EverythingMoeSite(
                                slug = slug,
                                name = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
                            )
                        }
                    }
                    lastCacheFetch = now
                    saveDiskCache()
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Failed to refresh EverythingMoe directory: ${e.message}" }
            }
        }
    }

    suspend fun matchSource(sourceName: String, baseUrl: String?): EverythingMoeSite? = withIOContext {
        refreshDirectoryIfNeeded()

        val sourceDomain = baseUrl?.let { extractDomain(it) }?.ifBlank { null }
        val normalizedSourceName = sourceName.lowercase().replace("[^a-z0-9]".toRegex(), "")

        // 1. Direct domain match against cached sites
        if (sourceDomain != null) {
            for (site in siteMemoryCache.values) {
                if (site.url.isNotBlank() && extractDomain(site.url) == sourceDomain) {
                    return@withIOContext getSiteBySlug(site.slug) ?: site
                }
                if (site.mirrors.any { extractDomain(it) == sourceDomain }) {
                    return@withIOContext getSiteBySlug(site.slug) ?: site
                }
            }
        }

        // 2. Name / Slug match
        for (site in siteMemoryCache.values) {
            val normalizedSiteName = site.name.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val normalizedSlug = site.slug.lowercase().replace("[^a-z0-9]".toRegex(), "")
            if (normalizedSiteName == normalizedSourceName ||
                normalizedSlug == normalizedSourceName ||
                (normalizedSourceName.length > 3 && (normalizedSiteName.contains(normalizedSourceName) || normalizedSourceName.contains(normalizedSiteName)))
            ) {
                return@withIOContext getSiteBySlug(site.slug) ?: site
            }
        }

        // 3. Fallback: try direct slug fetch if normalized name looks like a slug
        if (normalizedSourceName.isNotBlank()) {
            val directSlug = sourceName.lowercase().replace(" ", "-").replace("[^a-z0-9-]".toRegex(), "")
            val directSite = getSiteBySlug(directSlug)
            if (directSite != null) {
                return@withIOContext directSite
            }
        }

        null
    }

    suspend fun getInstalledExtensionsIntelligence(installedExtensions: List<Extension.Installed>): String = withIOContext {
        if (installedExtensions.isEmpty()) return@withIOContext "No installed extensions to evaluate."

        val sb = StringBuilder()
        var matchedCount = 0

        for (ext in installedExtensions) {
            for (source in ext.sources) {
                val baseUrl = (source as? AnimeHttpSource)?.baseUrl
                val site = matchSource(source.name, baseUrl)
                if (site != null) {
                    matchedCount++
                    val statusStr = if (site.isDead) "🔴 DEAD (${site.deadReason ?: "Discontinued"})" else "🟢 ALIVE"
                    val rankStr = if (site.rank.isNotBlank()) "Rank: ${site.rank}" else "Rank: Unranked"
                    val tagsStr = if (site.tags.isNotEmpty()) site.tags.joinToString(", ") else "None"
                    val mirrorsStr = if (site.mirrors.isNotEmpty()) {
                        site.mirrors.take(4).joinToString(", ") { extractDomain(it) }
                    } else "None listed"
                    val ratingStr = if (site.reviewCount > 0) "+${site.reviewVoteSum} (${site.reviewCount} reviews)" else "No reviews"

                    sb.append("- **${source.name}** (`${baseUrl ?: "N/A"}`):\n")
                    sb.append("  * **Status**: $statusStr | **$rankStr** | **Community**: $ratingStr\n")
                    sb.append("  * **Features/Tags**: $tagsStr\n")
                    sb.append("  * **Active Mirrors**: $mirrorsStr\n")
                    if (!site.reviews.isNullOrEmpty()) {
                        val topReview = site.reviews.maxByOrNull { it.vote ?: 0 }
                        if (topReview != null && !topReview.review.isNullOrBlank()) {
                            val cleanRev = topReview.review.take(120).replace("\n", " ")
                            sb.append("  * **Top User Feedback**: \"$cleanRev\"\n")
                        }
                    }
                }
            }
        }

        if (matchedCount == 0) {
            return@withIOContext "No EverythingMoe entries matched installed extensions directly."
        }

        sb.toString().trim()
    }
}
