package eu.kanade.tachiyomi.ui.player.loader

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.HosterState
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.getChangedAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class HosterLoader {
    companion object {
        /**
         * Check for the best video from the current hosterState.
         *
         * The first video with the `preferred` attribute is selected, however
         * if no such video is selected the first video with a non-empty url is selected.
         * If there are no viable videos at all, an error is thrown.
         *
         * @return the indices of the hoster & video
         */
        fun selectBestVideo(hosterState: List<HosterState>): Pair<Int, Int> {
            val availableHosters = hosterState.withIndex()
                .filter { (_, state) -> state is HosterState.Ready }

            // Check for first preferred video across all ready hosters
            // (We follow the hoster order provided by the extension)
            availableHosters.forEach { (index, state) ->
                val readyState = state as HosterState.Ready
                val prefIdx = readyState.videoList.indexOfFirst { it.preferred }
                if (prefIdx != -1) {
                    val videoState = readyState.videoState[prefIdx]
                    if (videoState == Video.State.READY || videoState == Video.State.QUEUE) {
                        return index to prefIdx
                    }
                }
            }

            // Fallback: Check for first video with non-empty url
            availableHosters.forEach { (index, state) ->
                val readyState = state as HosterState.Ready
                val firstIdx = readyState.videoList.indexOfFirst { it.videoUrl.isNotEmpty() }
                if (firstIdx != -1) {
                    val videoState = readyState.videoState[firstIdx]
                    if (videoState == Video.State.READY || videoState == Video.State.QUEUE) {
                        return index to firstIdx
                    }
                }
            }

            // No success
            return Pair(-1, -1)
        }

        /**
         * Return the first loaded and valid "best" video, based on the criteria in the function `selectBestVideo` above.
         *
         * @param source The source for the episode
         * @param hosterList the list of hosters
         * @return the video, or null if no valid video was found
         */
        suspend fun getBestVideo(source: AnimeSource, hosterList: List<Hoster>): Video? {
            val hosterStates = MutableList<HosterState>(hosterList.size) { HosterState.Idle("") }
            val semaphore = kotlinx.coroutines.sync.Semaphore(5)

            return try {
                withContext<Video?>(Dispatchers.IO) {
                    hosterList.mapIndexed { hosterIdx, hoster ->
                        async {
                            semaphore.acquire()
                            try {
                                val hosterState = try {
                                    kotlinx.coroutines.withTimeout(45000) {
                                        EpisodeLoader.loadHosterVideos(source, hoster)
                                    }
                                } catch (e: Exception) {
                                    HosterState.Error(hoster.hosterName)
                                }
                                hosterStates[hosterIdx] = hosterState

                                if (hosterState is HosterState.Ready) {
                                    // Pre-resolve preferred videos to avoid selecting broken ones
                                    hosterState.videoList.forEachIndexed { index, video ->
                                        if (video.preferred && !video.initialized) {
                                            getResolvedVideo(source, video)
                                        }
                                    }
                                }
                            } finally {
                                semaphore.release()
                            }
                        }
                    }.awaitAll()

                    // Final attempt to find any READY video using our established priority (Preferred first, then First Valid)
                    var (hosterIdx, videoIdx) = selectBestVideo(hosterStates)
                    while (hosterIdx != -1) {
                        val hosterState = hosterStates[hosterIdx] as HosterState.Ready
                        val video = hosterState.videoList[videoIdx]
                        val resolvedVideo = getResolvedVideo(source, video)
                        if (resolvedVideo?.videoUrl?.isNotEmpty() == true) {
                            coroutineContext.cancelChildren()
                            return@withContext resolvedVideo
                        }

                        hosterStates[hosterIdx] =
                            (hosterStates[hosterIdx] as HosterState.Ready).getChangedAt(
                                videoIdx,
                                video,
                                Video.State.ERROR,
                            )
                        val newResult = selectBestVideo(hosterStates)
                        hosterIdx = newResult.first
                        videoIdx = newResult.second
                    }

                    coroutineContext.cancelChildren()
                    return@withContext null
                }
            } finally {
                // Ensure everything is cleaned up
            }
        }

        suspend fun getResolvedVideo(source: AnimeSource?, video: Video): Video? {
            val resolvedVideo = if (source is AnimeHttpSource && !video.initialized) {
                try {
                    kotlinx.coroutines.withTimeout(45000) {
                        source.resolveVideo(video)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    null
                }
            } else {
                video
            }

            return resolvedVideo?.copy(initialized = true)
        }
    }
}
