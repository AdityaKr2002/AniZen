package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.net.Uri
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
        val pending = queueState.value.filter { it.status != Download.State.DOWNLOADED }
        if (pending.isEmpty()) return false

        _isRunningFlow.value = true
        downloaderJob = scope.launch {
            pending.forEach { download ->
                if (!isRunning) return@launch
                try {
                    downloadEpisode(download)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e)
                    download.status = Download.State.ERROR
                    notifier.onError(e.message)
                }
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
        sandboxDir.mkdirs()

        notifier.onProgressChange(download)
        try {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            
            val finalExt = if (download.video?.videoUrl?.contains(".mp4") == true) "mp4" else "mkv"
            val mergedFile = File(sandboxDir, "$episodeDirname.$finalExt")

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
            }
            download.video = video

            val videoFile = if (video.videoUrl.startsWith("magnet") || video.videoUrl.endsWith(".torrent")) {
                download.engineType = "Torrent"; torrentDownload(download, sandboxDir, episodeDirname)
            } else if (video.videoUrl.contains(".m3u8")) {
                download.engineType = "HLS"; nativeHlsDownload(download, sandboxDir, episodeDirname)
            } else if (video.videoUrl.contains(".mpd") || video.audioTracks.isNotEmpty()) {
                download.engineType = "DASH"; nativeDashMuxDownload(download, sandboxDir, episodeDirname)
            } else {
                download.engineType = "Normal"; internalDownload(download, sandboxDir, episodeDirname)
            }

            finalizeDownload(download, videoFile, animeDir, episodeDirname)
            
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            download.status = Download.State.ERROR
            notifier.onError(e.message)
        }
    }

    private suspend fun finalizeDownload(download: Download, sandboxFile: File, publicDir: UniFile, filename: String) {
        download.status = Download.State.FINALIZING
        store.update(download, force = true)
        
        val finalFile = publicDir.createFile(sandboxFile.name!!)!!
        
        context.contentResolver.openFileDescriptor(finalFile.uri, "w")?.use { opfd ->
            java.io.FileInputStream(sandboxFile).channel.use { inChannel ->
                java.io.FileOutputStream(opfd.fileDescriptor).channel.use { outChannel ->
                    inChannel.transferTo(0, inChannel.size(), outChannel)
                }
            }
        }
        
        sandboxFile.parentFile?.deleteRecursively()
        download.status = Download.State.DOWNLOADED
        store.update(download, force = true)
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

                            kotlinx.coroutines.withTimeout(15000L) {
                                val req = Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf())
                                    .header("Range", "bytes=$start-$end").build()
                                client.newCall(req).execute().use { res ->
                                    val source = res.body?.source() ?: throw IOException("Empty body")
                                    FileOutputStream(partFile, true).use { out ->
                                        val buffer = BufferPool.obtain()
                                        try {
                                            var read: Int
                                            var lastUpdate = System.currentTimeMillis()
                                            while (source.read(buffer).also { read = it } != -1) {
                                                ensureActive()
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
                    }
                }.awaitAll()
            }
            FileOutputStream(finalFile).channel.use { outChannel ->
                for (i in 0 until threadCount) {
                    val partFile = File(sandboxDir, "$filename.part$i")
                    java.io.FileInputStream(partFile).channel.use { it.transferTo(0, it.size(), outChannel) }
                    partFile.delete()
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
        val baseUrl = video.videoUrl.substringBeforeLast("/") + "/"
        
        val segments = client.newCall(Request.Builder().url(video.videoUrl).headers(video.headers ?: Headers.headersOf()).build()).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Failed to fetch playlist: ${res.code}")
            res.body?.string()?.lines()?.filter { it.isNotBlank() && !it.startsWith("#") } ?: emptyList()
        }
        
        if (segments.isEmpty()) throw IOException("No segments found in HLS playlist")
        
        download.totalSegments = segments.size
        val finalFile = File(sandboxDir, "$filename.ts")
        val outStream = FileOutputStream(finalFile, true)
        val segmentQueue = segments.mapIndexed { index, url -> 
            val fullUrl = if (url.startsWith("http")) url else baseUrl + url
            index to fullUrl 
        }.toMutableList()
        val downloadedCount = AtomicLong(0)

        coroutineScope {
            repeat(calculateDynamicConcurrency("")) {
                launch {
                    while (isActive) {
                        val seg = synchronized(segmentQueue) { if (segmentQueue.isNotEmpty()) segmentQueue.removeAt(0) else null } ?: break
                        retry(times = 5) {
                            kotlinx.coroutines.withTimeout(30000L) {
                                client.newCall(Request.Builder().url(seg.second).headers(video.headers ?: Headers.headersOf()).build()).execute().use { res ->
                                    if (!res.isSuccessful) throw IOException("Failed to download segment: ${res.code}")
                                    val data = res.body?.bytes() ?: throw IOException("Empty segment")
                                    synchronized(outStream) { outStream.write(data) }
                                    
                                    val currentCount = downloadedCount.incrementAndGet().toInt()
                                    download.downloadedSegments = currentCount
                                    
                                    // Throttle updates for HLS too
                                    if (currentCount % 5 == 0 || currentCount == segments.size) {
                                        store.update(download)
                                        notifier.onProgressChange(download)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        outStream.close()
        return finalFile
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
