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
    private val slugLookup = ConcurrentHashMap<String, String>() // normalized slug -> exact case slug
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
                    cached.sites.filter { it.tags.isNotEmpty() || it.url.isNotBlank() }.forEach { site ->
                        val lowerSlug = site.slug.lowercase()
                        siteMemoryCache[lowerSlug] = site
                        slugLookup[lowerSlug] = site.slug
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
                sites = siteMemoryCache.values.filter { it.tags.isNotEmpty() || it.url.isNotBlank() }.toList(),
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

    private fun extractJsonBlob(text: String, startIdx: Int): String? {
        val idx = text.indexOf('{', startIdx)
        if (idx == -1) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in idx until text.length) {
            val c = text[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                if (inString) escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        return text.substring(idx, i + 1)
                    }
                }
            }
        }
        return null
    }

    suspend fun getSiteBySlug(rawSlug: String): EverythingMoeSite? = withIOContext {
        val lowerSlug = rawSlug.lowercase()
        val exactSlug = slugLookup[lowerSlug] ?: rawSlug

        val cached = siteMemoryCache[lowerSlug]
        if (cached != null && (cached.tags.isNotEmpty() || cached.url.isNotBlank())) {
            return@withIOContext cached
        }

        try {
            val request = Request.Builder()
                .url("$BASE_URL/s/$exactSlug")
                .header("User-Agent", USER_AGENT)
                .build()

            val timedClient = networkHelper.client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            timedClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withIOContext null
                val html = response.body.string()
                val site = parseSitePage(exactSlug, html)
                if (site != null) {
                    siteMemoryCache[lowerSlug] = site
                    slugLookup[lowerSlug] = site.slug
                    saveDiskCache()
                    return@withIOContext site
                }
                null
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Failed to fetch EverythingMoe site page for $rawSlug: ${e.message}" }
            null
        }
    }

    private fun parseSitePage(slug: String, html: String): EverythingMoeSite? {
        val marker = "var siteData = "
        val idx = html.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val rawJsonCandidate = extractJsonBlob(html, start) ?: return null

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
            reviews = reviewsList,
        )
    }

    suspend fun refreshDirectoryIfNeeded(): Unit = withIOContext {
        val now = System.currentTimeMillis()
        if (slugLookup.isNotEmpty() && (now - lastCacheFetch < CACHE_TTL_MS)) {
            return@withIOContext
        }

        cacheMutex.withLock {
            if (slugLookup.isNotEmpty() && (now - lastCacheFetch < CACHE_TTL_MS)) {
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

                    for (exactSlug in slugs) {
                        val lowerSlug = exactSlug.lowercase()
                        slugLookup[lowerSlug] = exactSlug
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
                    return@withIOContext site
                }
                if (site.mirrors.any { extractDomain(it) == sourceDomain }) {
                    return@withIOContext site
                }
            }
        }

        // 2. Name / Slug match against cached sites
        for (site in siteMemoryCache.values) {
            val normalizedSiteName = site.name.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val normalizedSlug = site.slug.lowercase().replace("[^a-z0-9]".toRegex(), "")
            if (normalizedSiteName == normalizedSourceName ||
                normalizedSlug == normalizedSourceName ||
                (normalizedSourceName.length >= 4 && (normalizedSiteName == normalizedSourceName || normalizedSlug == normalizedSourceName))
            ) {
                return@withIOContext site
            }
        }

        // 3. Fallback: exact slug lookup from directory
        val lowerCandidate = sourceName.lowercase().replace(" ", "-").replace("[^a-z0-9-]".toRegex(), "")
        if (slugLookup.containsKey(lowerCandidate)) {
            val site = getSiteBySlug(lowerCandidate)
            if (site != null && (site.tags.isNotEmpty() || site.url.isNotBlank())) {
                return@withIOContext site
            }
        }

        null
    }

    suspend fun getIntelligenceContext(
        query: String,
        installedExtensions: List<Extension.Installed>,
    ): String = withIOContext {
        refreshDirectoryIfNeeded()
        val sb = StringBuilder()

        // 1. Installed extensions summary
        val installedMatched = mutableListOf<Triple<String?, String, EverythingMoeSite>>()
        for (ext in installedExtensions) {
            for (source in ext.sources) {
                val baseUrl = (source as? AnimeHttpSource)?.baseUrl
                val site = matchSource(source.name, baseUrl)
                if (site != null && (site.tags.isNotEmpty() || site.url.isNotBlank())) {
                    installedMatched.add(Triple(ext.name, baseUrl ?: site.url, site))
                }
            }
        }

        if (installedMatched.isNotEmpty()) {
            sb.append("### INSTALLED EXTENSIONS COMMUNITY INTELLIGENCE (From EverythingMoe):\n")
            for ((extName, baseUrl, site) in installedMatched) {
                formatSiteEntry(sb, extName, site.name, baseUrl, site)
            }
        }

        // 2. Queried/Mentioned Sources from Directory (Not installed)
        val queryWords = query.lowercase().split("[^a-zA-Z0-9_-]".toRegex()).filter { it.length >= 3 }
        val mentionedSites = mutableListOf<EverythingMoeSite>()
        for (word in queryWords) {
            if (word in listOf("the", "and", "for", "with", "from", "anime", "extension", "extensions", "source", "sources", "stream", "working", "check", "what", "which", "down", "dead")) continue
            val site = matchSource(word, null)
            if (site != null && (site.tags.isNotEmpty() || site.url.isNotBlank()) && mentionedSites.none { it.slug.equals(site.slug, ignoreCase = true) }) {
                val alreadyInstalled = installedMatched.any { it.third.slug.equals(site.slug, ignoreCase = true) }
                if (!alreadyInstalled) {
                    mentionedSites.add(site)
                }
            }
        }

        if (mentionedSites.isNotEmpty()) {
            sb.append("\n### COMMUNITY DIRECTORY INTELLIGENCE (Queried / Mentioned Sites - Not Installed):\n")
            for (site in mentionedSites) {
                formatSiteEntry(sb, null, site.name, site.url, site)
            }
        }

        sb.toString().trim()
    }

    private fun formatSiteEntry(
        sb: StringBuilder,
        extName: String?,
        sourceName: String,
        baseUrl: String?,
        site: EverythingMoeSite,
    ) {
        val statusStr = if (site.isDead) "🔴 DEAD (${site.deadReason ?: "Discontinued"})" else "🟢 ALIVE"
        val rankStr = if (site.rank.isNotBlank()) "Rank: ${site.rank}" else "Unranked"
        val tagsStr = if (site.tags.isNotEmpty()) site.tags.joinToString(", ") else "None listed"
        val mirrorsStr = if (site.mirrors.isNotEmpty()) {
            site.mirrors.take(4).joinToString(", ") { extractDomain(it) }
        } else "None listed"
        val ratingStr = if (site.reviewCount > 0) "+${site.reviewVoteSum} (${site.reviewCount} reviews)" else "No reviews"

        val titlePrefix = if (extName != null && extName != sourceName) "$extName ($sourceName)" else sourceName
        sb.append("- **$titlePrefix** (`${baseUrl ?: site.url}`):\n")
        sb.append("  * **Status**: $statusStr | **$rankStr** | **Community Rating**: $ratingStr\n")
        sb.append("  * **Supported Features/Tags**: $tagsStr\n")
        sb.append("  * **Active Mirrors**: $mirrorsStr\n")

        val topReviews = site.reviews.filter { !it.review.isNullOrBlank() }.sortedByDescending { it.vote ?: 0 }.take(2)
        if (topReviews.isNotEmpty()) {
            sb.append("  * **Top User Feedback**:\n")
            topReviews.forEach { r ->
                val clean = (r.review ?: "").replace("\n", " ").take(150)
                sb.append("    - \"$clean\" (Votes: ${r.vote ?: 0})\n")
            }
        }
    }
}
