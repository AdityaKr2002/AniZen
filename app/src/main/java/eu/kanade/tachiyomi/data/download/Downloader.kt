package eu.kanade.tachiyomi.data.download

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import androidx.annotation.RequiresApi
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.player.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.loader.HosterLoader
import tachiyomi.core.common.util.system.logcat
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import okhttp3.Headers
import okhttp3.Request
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * Pro-Level Downloader matching 1DM+ Architecture.
 * Features: FilesDir Sandbox, Startup Truncation, Micro-Chunk Queueing, Jittered Exponential Backoff.
 */
class Downloader(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkHelper: eu.kanade.tachiyomi.network.NetworkHelper = Injekt.get(),
) {

    private val preferences: DownloadPreferences by injectLazy()
    private val store = DownloadStore(context)
    private val _queueState = MutableStateFlow<List<Download>>(emptyList())
    val queueState = _queueState.asStateFlow()

    private val memorySemaphore = Semaphore(12)
    private val notifier by lazy { DownloadNotifier(context) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloaderJob: Job? = null
    
    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow = _isRunningFlow.asStateFlow()

    val isRunning: Boolean
        get() = _isRunningFlow.value

    init {
        launchIO {
            val downloads = store.restore()
            addAllToQueue(downloads)
            sweepOrphanedFiles(downloads) // Fire the janitor on startup
        }
    }

    private fun calculateDynamicConcurrency(host: String): Int {
        if (host.contains("animepahe")) return 1 // Adaptive: Animepahe fails with multi-threading
        
        val userThreads = preferences.downloadThreads().get().coerceAtLeast(1)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        return if (activityManager?.isLowRamDevice == true) userThreads.coerceIn(1, 4) else userThreads.coerceIn(1, 64)
    }

    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) return false
        
        _isRunningFlow.value = true
        downloaderJob = scope.launch {
            // Pro-Active: Pre-fetch video URLs in parallel to eliminate transition lag
            launch {
                queueState.value.filter { it.video == null && it.status == Download.State.QUEUE }
                    .forEach { download ->
                        if (!isRunning) return@launch
                        try {
                            val hosters = EpisodeLoader.getHosters(download.episode, download.anime, download.source as AnimeSource)
                            download.video = HosterLoader.getBestVideo(download.source as AnimeSource, hosters)
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN) { "Pre-fetch failed for ${download.episode.name}" }
                        }
                    }
            }

            // Dynamic Queue Processing
            while (isRunning) {
                val download = queueState.value.firstOrNull { 
                    it.status == Download.State.QUEUE || it.status == Download.State.DOWNLOADING 
                } ?: break
                
                try {
                    downloadEpisode(download)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e)
                    download.status = Download.State.ERROR
                    notifier.onError(e.message)
                }
                delay(100) // Cooling period to prevent CPU spikes on rapid failures
            }

            _isRunningFlow.value = false
            val hasPending = queueState.value.any { it.status != Download.State.DOWNLOADED }
            if (!hasPending) {
                notifier.onComplete()
                DownloadJob.stop(context)
            }
        }
        return true
    }

    fun stop(reason: String? = null) {
        _isRunningFlow.value = false
        downloaderJob?.cancel()
        downloaderJob = null
        val hasPending = queueState.value.any { it.status != Download.State.DOWNLOADED }
        if (reason != null) notifier.onWarning(reason)
        else if (hasPending) notifier.onPaused()
        else {
            notifier.onComplete()
            notifier.dismissAll()
        }
        DownloadJob.stop(context)
    }

    fun pause() {
        _isRunningFlow.value = false
        downloaderJob?.cancel()
        downloaderJob = null
        _queueState.update {
            it.forEach { download ->
                if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                    download.status = Download.State.PAUSED
                    notifier.dismissProgress(download)
                }
            }
            it
        }
        notifier.onPaused()
    }

    fun dismissAll() {
        notifier.dismissAll()
    }

    fun clearQueue() {
        _isRunningFlow.value = false
        downloaderJob?.cancel()
        downloaderJob = null
        _queueState.update {
            it.forEach { download ->
                download.status = Download.State.NOT_DOWNLOADED
                download.clearProgress()
                notifier.dismissProgress(download)
            }
            store.clear()
            emptyList()
        }
        notifier.dismissProgress()
        notifier.dismissAll()
    }

    fun updateQueue(downloads: List<Download>) {
        _queueState.value = downloads
        store.addAll(downloads)
    }

    fun queueEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean, alt: Boolean = false, video: Video? = null) {
        val source = sourceManager.get(anime.source) as? HttpSource ?: return
        val downloads = episodes.map { Download(source, anime, it, alt, video) }
        addAllToQueue(downloads)
        if (autoStart || !DownloadJob.isRunning(context)) DownloadJob.start(context)
    }

    fun addAllToQueue(downloads: List<Download>) {
        _queueState.update { current ->
            val new = current.toMutableList()
            downloads.forEach { download ->
                if (new.none { it.episode.id == download.episode.id }) {
                    download.status = Download.State.QUEUE
                    new.add(download)
                }
            }
            store.addAll(new)
            new
        }
    }

    fun removeFromQueue(anime: Anime) {
        _queueState.update { current ->
            val new = current.filterNot { it.anime.id == anime.id }
            store.removeAll(current.filter { it.anime.id == anime.id })
            new
        }
    }

    fun removeFromQueue(episodes: List<Episode>) {
        val episodeIds = episodes.map { it.id }
        _queueState.update { current ->
            val new = current.filterNot { it.episode.id in episodeIds }
            store.removeAll(current.filter { it.episode.id in episodeIds })
            new
        }
    }

    private suspend fun <T> retry(
        times: Int = 5,
        initialDelay: Long = 1000,
        maxDelay: Long = 15000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                return block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                // FATAL ERROR CHECK: Do not retry dead/forbidden links
                if (e is HttpException) {
                    val code = e.code
                    if (code == 401 || code == 403 || code == 404 || code == 410) {
                        logcat(LogPriority.ERROR) { "Fatal HTTP $code. Aborting retry." }
                        throw e 
                    }
                }
                
                // Exponential Backoff with Jitter
                val jitter = Random.nextLong(0, 500)
                val backoff = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                delay(backoff + jitter)
                currentDelay = backoff
                logcat(LogPriority.WARN) { "Retry attempt ${attempt + 1} failed, backing off..." }
            }
        }
        return block()
    }

    private fun sweepOrphanedFiles(activeDownloads: List<Download>) {
        launchIO {
            try {
                val sandboxRoot = context.getExternalFilesDir("downloads") ?: return@launchIO
                if (!sandboxRoot.exists()) return@launchIO
                
                // Map the valid, active download directory names
                val expectedDirs = activeDownloads.map { 
                    provider.getEpisodeDirName(it.episode.name, it.episode.scanlator) 
                }.toSet()

                // Sweep the sandbox directory
                sandboxRoot.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name !in expectedDirs) {
                        logcat(LogPriority.INFO) { "Janitor Protocol: Deleting orphaned sandbox directory: ${file.name}" }
                        file.deleteRecursively()
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to sweep orphaned files" }
            }
        }
    }

    private suspend fun downloadEpisode(download: Download) {
        val animeDir = provider.getAnimeDir(download.anime.title, download.source)
        val episodeDirname = provider.getEpisodeDirName(download.episode.name, download.episode.scanlator)
        
        // Sandbox Storage: Protected from OS Cache cleanup
        val sandboxDir = File(context.getExternalFilesDir("downloads"), episodeDirname)
        if (!sandboxDir.exists() && !sandboxDir.mkdirs()) {
            throw IOException("Failed to create sandbox directory: ${sandboxDir.absolutePath}")
        }

        val videoFilename = DiskUtil.buildValidFilename(download.episode.name)

        notifier.onProgressChange(download)
        try {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            
            val finalExt = if (download.video?.videoUrl?.contains(".mp4") == true) "mp4" else "mkv"
            val mergedFile = File(sandboxDir, "$videoFilename.tmp")

            // RECOVERY: Handle interrupted FINALIZING state
            if (download.status == Download.State.FINALIZING && mergedFile.exists()) {
                finalizeDownload(download, mergedFile, animeDir, episodeDirname)
                return
            }

            download.status = Download.State.DOWNLOADING
            val video = retry {
                download.video ?: run {
                    val hosters = EpisodeLoader.getHosters(download.episode, download.anime, download.source as AnimeSource)
                    HosterLoader.getBestVideo(download.source as AnimeSource, hosters)
                } ?: throw Exception(context.stringResource(MR.strings.video_list_empty_error))
            }.also { download.video = it }
            
            // Check again for cancellation after slow network call
            kotlinx.coroutines.currentCoroutineContext().ensureActive()

            if (download.changeDownloader) {
                val success = externalDownload(download, animeDir, episodeDirname)
                if (success) return else throw Exception("Could not open external downloader")
            }

            val videoFile = if (video.videoUrl.startsWith("magnet") || video.videoUrl.endsWith(".torrent")) {
                download.engineType = "Torrent"; torrentDownload(download, sandboxDir, videoFilename)
            } else if (video.videoUrl.contains(".m3u8")) {
                download.engineType = "HLS"; nativeHlsDownload(download, sandboxDir, videoFilename)
            } else if (video.videoUrl.contains(".mpd") || video.audioTracks.isNotEmpty()) {
                download.engineType = "DASH"; nativeDashMuxDownload(download, sandboxDir, videoFilename)
            } else {
                download.engineType = "Normal"; internalDownload(download, sandboxDir, videoFilename)
            }

            // Download soft subtitles
            downloadSubtitles(video, sandboxDir, videoFilename)

            finalizeDownload(download, videoFile, animeDir, episodeDirname)
            
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Download failed" }
            if (e !is CancellationException) {
                download.status = Download.State.ERROR
                notifier.onError(e.message)
            }
        }
    }

    private fun checkFreeSpace(dir: File, requiredSize: Long) {
        val stats = StatFs(dir.absolutePath)
        val available = stats.availableBlocksLong * stats.blockSizeLong
        if (available < requiredSize + MIN_DISK_SPACE) {
            throw IOException(context.stringResource(MR.strings.download_insufficient_space))
        }
    }

    private suspend fun downloadSubtitles(video: Video, sandboxDir: File, videoFilename: String) {
        if (video.subtitleTracks.isEmpty()) return
        
        val client = networkHelper.client
        coroutineScope {
            video.subtitleTracks.forEach { track ->
                launch {
                    val subExt = when {
                        track.url.endsWith(".vtt") -> "vtt"
                        track.url.endsWith(".ass") -> "ass"
                        else -> "srt"
                    }
                    val filename = "${videoFilename}.${track.lang}.$subExt"
                    val subFile = File(sandboxDir, filename)
                    if (subFile.exists() && subFile.length() > 0) return@launch

                    if (track.url.isBlank()) return@launch

                    if (track.url.startsWith("file://")) {
                        try {
                            val sourceFile = File(track.url.removePrefix("file://"))
                            if (sourceFile.exists()) {
                                sourceFile.inputStream().use { input ->
                                    java.io.FileOutputStream(subFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e) { "Failed to copy local subtitle: ${track.url}" }
                        }
                        return@launch
                    }

                    retry(times = 3) {
                        val req = Request.Builder().url(track.url).build()
                        client.newCall(req).execute().use { res ->
                            if (!res.isSuccessful) throw IOException("Failed to download subtitle: ${res.code}")
                            res.body?.byteStream()?.use { input ->
                                java.io.FileOutputStream(subFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun finalizeDownload(download: Download, sandboxFile: File, publicDir: UniFile, filename: String) {
        download.status = Download.State.FINALIZING
        download.progress = 0
        notifier.onProgressChange(download)

        // Create episode directory
        var destDir = publicDir.findFile(filename)
        if (destDir != null && destDir.isFile) {
            destDir.delete()
            destDir = publicDir.createDirectory(filename)!!
        } else if (destDir == null) {
            destDir = publicDir.createDirectory(filename)!!
        }

        val videoFilename = DiskUtil.buildValidFilename(download.episode.name)
        val finalExt = if (download.video?.videoUrl?.contains(".mp4") == true) "mp4" else "mkv"
        val finalName = "$videoFilename.$finalExt"

        // CRITICAL: Prevent file bloating by deleting existing partial files from failed runs
        var destFile = destDir.findFile(finalName)
        if (destFile != null) destFile.delete()
        
        // Also delete any corrupt tmp file from previous buggy versions
        destDir.findFile("$videoFilename.tmp")?.delete()

        // Create the file with the final extension immediately so SAF assigns the correct video MIME type
        destFile = destDir.createFile(finalName)!!

        java.io.FileInputStream(sandboxFile).use { input ->
            destFile.openOutputStream().use { output ->
                val buffer = ByteArray(8 * 1024 * 1024) // 8MB buffer for speed
                var bytesCopied = 0L
                val totalBytes = sandboxFile.length()
                var read: Int
                var lastUpdate = System.currentTimeMillis()
                
                while (input.read(buffer).also { read = it } != -1) {
                    coroutineContext.ensureActive()
                    output.write(buffer, 0, read)
                    bytesCopied += read
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 1000 || bytesCopied == totalBytes) {
                        download.progress = ((bytesCopied.toDouble() / totalBytes) * 100).toInt()
                        notifier.onProgressChange(download)
                        store.update(download)
                        lastUpdate = now
                    }
                }
            }
        }

        // Pro-Active: Move soft subtitles to the destination directory
        val sandboxDir = sandboxFile.parentFile
        sandboxDir?.listFiles()?.forEach { file ->
            if (file.name != sandboxFile.name && !file.name.endsWith(".part") && !file.name.endsWith(".tmp")) {
                val subFile = destDir.createFile(file.name)
                if (subFile != null) {
                    java.io.FileInputStream(file).use { input ->
                        subFile.openOutputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        
        sandboxFile.parentFile?.deleteRecursively()

        download.status = Download.State.DOWNLOADED
        
        // KMK -->
        _queueState.update { it - download }
        store.remove(download)
        notifier.dismissProgress(download)
        // KMK <--

        notifier.onProgressChange(download)
        cache.addEpisode(filename, publicDir, download.anime)
    }

    private suspend fun internalDownload(download: Download, sandboxDir: File, filename: String): File {
        val video = download.video!!
        val client = networkHelper.downloadClient
        val host = Uri.parse(video.videoUrl).host ?: ""
        val threadCount = calculateDynamicConcurrency(host)
        
        val headRes = client.newCall(Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf()).head().build()).execute()
        val size = headRes.header("Content-Length")?.toLong() ?: -1L
        if (size > 0) checkFreeSpace(sandboxDir, size)
        
        download.totalSize = size
        download.activeThreads = threadCount

        val finalFile = File(sandboxDir, "$filename.tmp")
        val downloadedBytes = LongAdder()

        if (size > 0 && threadCount > 1) {
            val partSize = size / threadCount
            coroutineScope {
                (0 until threadCount).map { i ->
                    async {
                        val partFile = File(sandboxDir, "$filename.part$i")
                        val expected = (download.partProgress[i] ?: 0f) * (if (i == threadCount - 1) size - (i * partSize) else partSize)
                        if (partFile.exists() && partFile.length() > expected.toLong()) {
                            RandomAccessFile(partFile, "rw").use { it.setLength(expected.toLong()) }
                        }
                        
                        var localDownloaded = partFile.length()
                        downloadedBytes.add(localDownloaded)

                        retry(times = 5) {
                            val start = i * partSize + localDownloaded
                            val end = if (i == threadCount - 1) size - 1 else (i + 1) * partSize - 1
                            if (i > 0) delay(50L)

                            val req = Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf())
                                .header("Range", "bytes=$start-$end").build()
                            client.newCall(req).execute().use { res ->
                                val source = res.body?.source() ?: throw IOException("Empty body")
                                java.io.FileOutputStream(partFile, true).use { out ->
                                    val buffer = BufferPool.obtain()
                                    try {
                                        var read: Int
                                        var lastUpdate = System.currentTimeMillis()
                                        while (source.read(buffer).also { read = it } != -1) {
                                            coroutineContext.ensureActive()
                                            out.write(buffer, 0, read)
                                            localDownloaded += read
                                            downloadedBytes.add(read.toLong())

                                            val now = System.currentTimeMillis()
                                            if (now - lastUpdate > 500) {
                                                download.partProgress[i] = (localDownloaded.toDouble() / (end - (i * partSize) + 1)).toFloat()
                                                download.update(downloadedBytes.sum(), size, false)
                                                notifier.onProgressChange(download)
                                                store.update(download)
                                                lastUpdate = now
                                            }
                                        }
                                        // Final update for this part
                                        download.partProgress[i] = (localDownloaded.toDouble() / (end - (i * partSize) + 1)).toFloat()
                                        download.update(downloadedBytes.sum(), size, false)
                                        notifier.onProgressChange(download)
                                        store.update(download)
                                    } finally {
                                        BufferPool.recycle(buffer)
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()
            }

            download.status = Download.State.MERGING
            download.progress = 0
            notifier.onProgressChange(download)

            var mergedBytes = 0L
            var lastUpdate = System.currentTimeMillis()

            java.io.FileOutputStream(finalFile).use { outStream ->
                val outChannel = outStream.channel
                for (i in 0 until threadCount) {
                    val partFile = File(sandboxDir, "$filename.part$i")
                    if (partFile.exists()) {
                        java.io.FileInputStream(partFile).use { inStream ->
                            val inChannel = inStream.channel
                            val size = inChannel.size()
                            var remaining = size
                            var position = 0L
                            while (remaining > 0) {
                                coroutineContext.ensureActive()
                                val toTransfer = Math.min(remaining, 4L * 1024 * 1024)
                                val transferred = inChannel.transferTo(position, toTransfer, outChannel)
                                if (transferred <= 0) break
                                position += transferred
                                remaining -= transferred
                                mergedBytes += transferred

                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 500) {
                                    download.progress = ((mergedBytes.toDouble() / download.totalSize) * 100).toInt()
                                    notifier.onProgressChange(download)
                                    lastUpdate = now
                                }
                            }
                        }
                        partFile.delete()
                    }
                }
            }
        } else {
            retry {
                val req = Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf()).build()
                client.newCall(req).execute().use { res ->
                    res.body?.byteStream()?.copyTo(FileOutputStream(finalFile))
                }
            }
        }
        return finalFile
    }

    private suspend fun nativeHlsDownload(download: Download, sandboxDir: File, filename: String): File {
        val video = download.video!!
        val client = networkHelper.downloadClient
        val playlistRes = client.newCall(Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf()).build()).execute()
        
        if (!playlistRes.isSuccessful) throw IOException("Failed to fetch playlist: ${playlistRes.code}")
        val lines = playlistRes.body?.string()?.lines() ?: emptyList()

        val baseUrl = video.videoUrl.substringBeforeLast("/") + "/"
        val segments = mutableListOf<String>()
        var encryptionKeyUrl: String? = null
        var mediaSequence = 0

        // Extract correct Media Sequence and AES Key
        for (line in lines) {
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                mediaSequence = line.substringAfter(":").toIntOrNull() ?: 0
            } else if (line.startsWith("#EXT-X-KEY:METHOD=AES-128")) {
                val match = Regex("URI=\"([^\"]+)\"").find(line)
                encryptionKeyUrl = match?.groupValues?.get(1)
                if (encryptionKeyUrl != null && !encryptionKeyUrl.startsWith("http")) {
                    encryptionKeyUrl = baseUrl + encryptionKeyUrl
                }
            } else if (!line.startsWith("#") && line.isNotBlank()) {
                segments.add(if (line.startsWith("http")) line else baseUrl + line)
            }
        }

        if (segments.isEmpty()) throw IOException("No segments found in HLS playlist")
        download.totalSegments = segments.size
        
        var secretKey: javax.crypto.spec.SecretKeySpec? = null
        if (encryptionKeyUrl != null) {
            val keyRes = client.newCall(Request.Builder().url(encryptionKeyUrl).headers(video.headers ?: Headers.headersOf()).build()).execute()
            val keyBytes = keyRes.body?.bytes() ?: throw IOException("Failed to fetch AES key")
            secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
        }

        val downloadedCount = java.util.concurrent.atomic.LongAdder()
        val downloadedBytes = java.util.concurrent.atomic.LongAdder()
        val segmentQueue = segments.mapIndexed { index, url -> index to url }.toMutableList()
        var lastUpdate = System.currentTimeMillis()
        
        val threadCount = calculateDynamicConcurrency("")
        download.activeThreads = threadCount

        coroutineScope {
            repeat(threadCount) {
                launch {
                    while (isActive) {
                        val seg = synchronized(segmentQueue) { if (segmentQueue.isNotEmpty()) segmentQueue.removeAt(0) else null } ?: break
                        val segmentFile = File(sandboxDir, "seg_${seg.first}.part")

                        if (segmentFile.exists() && segmentFile.length() > 0) {
                            downloadedCount.increment()
                            downloadedBytes.add(segmentFile.length())
                            download.segmentProgress[seg.first] = true
                            continue
                        }

                        retry(times = 5) {
                            client.newCall(Request.Builder().url(seg.second).headers(video.headers ?: Headers.headersOf()).build()).execute().use { res ->
                                if (!res.isSuccessful) throw IOException("Segment failed: ${res.code}")
                                var data = res.body?.bytes() ?: throw IOException("Empty segment")

                                coroutineContext.ensureActive()

                                // THREAD-SAFE AES DECRYPTION WITH CORRECT SEQUENCE IV
                                if (secretKey != null) {
                                    val seqNum = mediaSequence + seg.first
                                    val ivBytes = java.nio.ByteBuffer.allocate(16).putLong(8, seqNum.toLong()).array()
                                    val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
                                    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.IvParameterSpec(ivBytes))
                                    data = cipher.doFinal(data)
                                }

                                java.io.FileOutputStream(segmentFile).use { it.write(data) }
                                downloadedCount.increment()
                                downloadedBytes.add(data.size.toLong())

                                val currentCount = downloadedCount.sum().toInt()
                                download.downloadedSegments = currentCount

                                // NEW: Mark this exact segment as complete for the UI's secondary progress bar
                                download.segmentProgress[seg.first] = true

                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 1000 || currentCount == segments.size) {
                                    download.update(downloadedBytes.sum(), -1, false)
                                    store.update(download)
                                    notifier.onProgressChange(download)
                                    lastUpdate = now
                                }
                            }
                        }
                    }
                }
            }
        }

    download.status = if (secretKey != null) Download.State.DECRYPTING else Download.State.MERGING
    download.progress = 0
    notifier.onProgressChange(download)

    val finalFile = File(sandboxDir, "$filename.ts")
    val totalMergeSize = segments.indices.sumOf { File(sandboxDir, "seg_$it.part").length() }
    checkFreeSpace(sandboxDir, totalMergeSize)

    var mergedBytes = 0L
    var lastMergeUpdate = System.currentTimeMillis()

    java.io.FileOutputStream(finalFile).use { outStream ->
        val outChannel = outStream.channel
        for (i in segments.indices) {
            val segmentFile = File(sandboxDir, "seg_$i.part")
            if (segmentFile.exists()) {
                java.io.FileInputStream(segmentFile).use { inStream ->
                    val inChannel = inStream.channel
                    val size = inChannel.size()
                    var remaining = size
                    var position = 0L
                    while (remaining > 0) {
                        coroutineContext.ensureActive()
                        val toTransfer = Math.min(remaining, 4L * 1024 * 1024)
                        val transferred = inChannel.transferTo(position, toTransfer, outChannel)
                        if (transferred <= 0) break
                        position += transferred
                        remaining -= transferred
                        mergedBytes += transferred

                        val now = System.currentTimeMillis()
                        if (now - lastMergeUpdate > 500 && totalMergeSize > 0) {
                            download.progress = ((mergedBytes.toDouble() / totalMergeSize) * 100).toInt()
                            download.updateSpeed(downloadedBytes.sum()) // Keep speed updated during merge
                            notifier.onProgressChange(download)
                            lastMergeUpdate = now
                        }
                    }
                }
                segmentFile.delete()
            }
        }
    }
    return finalFile
}

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    private suspend fun externalDownload(download: Download, animeDir: UniFile, episodeDirname: String): Boolean {
        val video = download.video ?: return false
        val url = video.videoUrl
        val packageName = preferences.externalDownloaderSelection().get()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val animeTitle = download.anime.title
            val episodeName = download.episode.name
            val filename = DiskUtil.buildValidFilename("$animeTitle - $episodeName") + ".mp4"

            // Create the episode directory so external downloader can save inside it
            val episodeDir = animeDir.createDirectory(episodeDirname)
            val dirPath = episodeDir?.filePath ?: animeDir.filePath

            withUIContext {
                if (dirPath != null) {
                    context.copyToClipboard("Episode download location", dirPath)
                }
            }

            intent.setDataAndType(Uri.parse(url), "video/*")

            when {
                packageName.startsWith("idm.internet.download.manager") -> {
                    val headers = video.headers ?: (download.source as? HttpSource)?.headers
                    val bundle = Bundle()
                    headers?.let {
                        for (i in 0 until it.size) {
                            bundle.putString(it.name(i), it.value(i))
                        }
                    }

                    intent.apply {
                        putExtra("extra_filename", filename)
                        putExtra("extra_headers", bundle)
                        if (dirPath != null) {
                            putExtra("extra_path", dirPath)
                        }
                    }
                }
                packageName.startsWith("com.dv.adm") -> {
                    val headers = video.headers ?: (download.source as? HttpSource)?.headers
                    val bundle = Bundle()
                    headers?.let {
                        for (i in 0 until it.size) {
                            bundle.putString(it.name(i), it.value(i).replace("http", "h_ttp"))
                        }
                    }

                    intent.apply {
                        putExtra(
                            "com.dv.get.ACTION_LIST_ADD",
                            "${Uri.parse(url)}<info>$filename",
                        )
                        if (dirPath != null) {
                            putExtra("com.dv.get.ACTION_LIST_PATH", dirPath)
                        }
                        putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                    }
                }
                else -> {
                    val headers = video.headers ?: (download.source as? HttpSource)?.headers
                    if (headers != null) {
                        val headersBundle = Bundle()
                        for (i in 0 until headers.size) {
                            headersBundle.putString(headers.name(i), headers.value(i))
                        }
                        intent.putExtra("android.media.intent.extra.HTTP_HEADERS", headersBundle)
                        
                        val headersArray = Array(headers.size) { i -> "${headers.name(i)}: ${headers.value(i)}" }
                        intent.putExtra("headers", headersArray)
                    }

                    intent.apply {
                        putExtra("title", "${download.anime.title} - ${download.episode.name}")
                        putExtra("filename", filename)
                        putExtra("extra_filename", filename)
                        if (dirPath != null) {
                            putExtra("extra_path", dirPath) // fallback 1DM
                            putExtra("com.dv.get.ext_dir", dirPath) // fallback ADM
                        }
                    }
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            val pm = context.packageManager
            if (packageName.isNotBlank() && packageName != "None" && isPackageInstalled(packageName)) {
                intent.setPackage(packageName)
                // Attempt to find the specific downloader activity to bypass the 'Open With' dialog
                val resolveInfo = pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo.isNotEmpty()) {
                    // Optimized for 1DM+: Look for Editor or Add activity first to avoid browser-only components
                    val bestMatch = resolveInfo.find { it.activityInfo.name.contains("Editor", ignoreCase = true) }
                                     ?: resolveInfo.find { it.activityInfo.name.contains("Add", ignoreCase = true) }
                                     ?: resolveInfo.find { it.activityInfo.name.contains("Download", ignoreCase = true) }
                                     ?: resolveInfo.first()
                    intent.component = ComponentName(bestMatch.activityInfo.packageName, bestMatch.activityInfo.name)
                }
            }
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to chooser if direct launch fails or component is invalid
                intent.component = null
                val chooser = Intent.createChooser(intent, "Download with...")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
            
            // Explicitly remove from queue after successful handoff
            download.status = Download.State.DOWNLOADED
            _queueState.update { it - download }
            store.remove(download)
            notifier.dismissProgress(download)
            
            delay(1500) // Give external downloader time to register intent and prevent dropping multiple downloads
            return true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to launch external downloader: ${e.message}" }
            return false
        }
    }

    private suspend fun nativeDashMuxDownload(download: Download, sandboxDir: File, filename: String): File = File("") // Placeholder
    private suspend fun torrentDownload(download: Download, sandboxDir: File, filename: String): File = File("") // Placeholder

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
    }
}

private const val MIN_DISK_SPACE = 200L * 1024 * 1024

object BufferPool {
    private val pool = java.util.concurrent.ArrayBlockingQueue<ByteArray>(128)
    fun obtain(): ByteArray = pool.poll() ?: ByteArray(256 * 1024)
    fun recycle(buffer: ByteArray) { pool.offer(buffer) }
}
