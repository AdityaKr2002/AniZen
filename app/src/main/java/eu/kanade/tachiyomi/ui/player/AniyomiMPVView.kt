/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.player.controls.components.panels.toColorHexString
import eu.kanade.tachiyomi.ui.player.settings.AdvancedPlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import eu.kanade.tachiyomi.ui.player.applyAnime4K
import eu.kanade.tachiyomi.ui.player.buildVFChain
import eu.kanade.tachiyomi.ui.player.checkAndSetCopyMode
import eu.kanade.tachiyomi.ui.player.utils.Anime4KManager
import eu.kanade.tachiyomi.util.system.DeviceTierManager
import eu.kanade.tachiyomi.util.system.findActivity
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.injectLazy
import kotlin.reflect.KProperty

class AniyomiMPVView(context: Context, attributes: AttributeSet) : BaseMPVView(context, attributes) {

    private val playerPreferences: PlayerPreferences by injectLazy()
    private val decoderPreferences: DecoderPreferences by injectLazy()
    private val subtitlePreferences: SubtitlePreferences by injectLazy()
    private val audioPreferences: AudioPreferences by injectLazy()
    private val advancedPreferences: AdvancedPlayerPreferences by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val anime4kManager: Anime4KManager by injectLazy()

    var isExiting = false
    var initialized = false
    private var lastAdaptiveCheckTime = 0L

    private fun getPropertyInt(property: String): Int? {
        if (!initialized) return null
        return MPVLib.getPropertyInt(property) as Int?
    }

    private fun getPropertyBoolean(property: String): Boolean? {
        if (!initialized) return null
        return MPVLib.getPropertyBoolean(property) as Boolean?
    }

    private fun getPropertyDouble(property: String): Double? {
        if (!initialized) return null
        return MPVLib.getPropertyDouble(property) as Double?
    }

    private fun getPropertyString(property: String): String? {
        if (!initialized) return null
        return MPVLib.getPropertyString(property) as String?
    }

    val duration: Int?
        get() = getPropertyInt("duration")

    var timePos: Int?
        get() = getPropertyInt("time-pos")
        set(position) {
            if (initialized) MPVLib.setPropertyInt("time-pos", position!!)
        }

    var paused: Boolean?
        get() = getPropertyBoolean("pause")
        set(paused) {
            if (initialized) MPVLib.setPropertyBoolean("pause", paused!!)
        }

    val hwdecActive: String
        get() = getPropertyString("hwdec-current") ?: "no"

    val coreIdle: Boolean?
        get() = getPropertyBoolean("core-idle")

    val pausedForCache: Boolean?
        get() = getPropertyBoolean("paused-for-cache")

    val videoH: Int?
        get() = getPropertyInt("video-params/h")

    /**
     * Returns the video aspect ratio. Rotation is taken into account.
     */
    fun getVideoOutAspect(): Double? {
        return getPropertyDouble("video-params/aspect")?.let {
            if (it < 0.001) return 0.0
            if ((getPropertyInt("video-params/rotate") ?: 0) % 180 == 90) 1.0 / it else it
        }
    }

    inner class TrackDelegate(private val name: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = getPropertyString(name)
            if (v == "no" || v == null) return -1
            return v.toIntOrNull() ?: -1
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            if (value == -1) {
                MPVLib.setPropertyString(name, "no")
            } else {
                MPVLib.setPropertyInt(name, value)
            }
        }
    }

    var sid: Int by TrackDelegate("sid")
    var secondarySid: Int by TrackDelegate("secondary-sid")
    var aid: Int by TrackDelegate("aid")

    private var currentMaxBytes = 192 * 1024 * 1024L
    private var currentMaxBackBytes = 64 * 1024 * 1024L

    fun applyPlaybackStrategy() {
        val performanceProfile = decoderPreferences.performanceProfile().get()
        val tier = when (performanceProfile) {
            PlayerEfficiency.MaxPerformance -> DeviceTierManager.Tier.HIGH
            PlayerEfficiency.Balanced -> DeviceTierManager.Tier.MID
            PlayerEfficiency.PowerSaver -> DeviceTierManager.Tier.LOW
            else -> DeviceTierManager.getTier(context)
        }

        val (maxMb, maxBackMb, readahead) = when (tier) {
            DeviceTierManager.Tier.LOW -> Triple(64, 32, 60)
            DeviceTierManager.Tier.MID -> Triple(128, 64, 120)
            DeviceTierManager.Tier.HIGH -> Triple(192, 128, 180)
        }

        currentMaxBytes = maxMb * 1024 * 1024L
        currentMaxBackBytes = maxBackMb * 1024 * 1024L

        MPVLib.setOptionString("demuxer-readahead-secs", "$readahead")
        MPVLib.setOptionString("demuxer-max-bytes", "$currentMaxBytes")
        MPVLib.setOptionString("demuxer-max-back-bytes", "$currentMaxBackBytes")
    }

    fun restoreCache() {
        MPVLib.setPropertyString("demuxer-max-bytes", "$currentMaxBytes")
        MPVLib.setPropertyString("demuxer-max-back-bytes", "$currentMaxBackBytes")
    }

    fun shrinkCache() {
        // Shrink to 64MB to release memory when backgrounded
        val shrinkBytes = 64 * 1024 * 1024L
        MPVLib.setPropertyString("demuxer-max-bytes", "$shrinkBytes")
        MPVLib.setPropertyString("demuxer-max-back-bytes", "$shrinkBytes")
    }

    override fun initOptions(vo: String) {
        initialized = true
        val useAnime4K = decoderPreferences.enableAnime4K().get()
        // Anime4K is incompatible with gpu-next
        setVo(if (decoderPreferences.gpuNext().get() && !useAnime4K) "gpu-next" else "gpu")
        
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.setOptionString("profile", "fast")
        
        checkAndSetCopyMode(decoderPreferences)
        val hwdec = if (decoderPreferences.forceMediaCodecCopy().get()) {
            "mediacodec-copy"
        } else if (decoderPreferences.tryHWDecoding().get()) {
            "auto"
        } else {
            "no"
        }
        MPVLib.setOptionString("hwdec", hwdec)
        MPVLib.setOptionString("hwdec-codecs", "all")
        MPVLib.setOptionString("vd-lavc-dr", "yes")
        MPVLib.setOptionString("vd-lavc-fast", "yes")
        MPVLib.setOptionString("vd-lavc-threads", "0")
        MPVLib.setOptionString("opengl-pbo", "yes")
        
        val smoothMotionEnabled = decoderPreferences.smoothMotion().get()
        
        // Force detect refresh rate
        val displayRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate ?: 60f
        } else {
            60f
        }

        // Use display-resample ONLY if smooth motion is enabled.
        // For standard playback, fallback to audio sync for power efficiency and to respect native frame rates.
        if (smoothMotionEnabled) {
            val interpolationFPSLimit = decoderPreferences.interpolationFPSLimit().get()
            val targetFPS = if (interpolationFPSLimit > 0 && interpolationFPSLimit < displayRefreshRate) {
                context.findActivity()?.window?.let { window ->
                    val params = window.attributes
                    params.preferredRefreshRate = interpolationFPSLimit.toFloat()
                    window.attributes = params
                }
                interpolationFPSLimit.toFloat()
            } else {
                displayRefreshRate
            }

            MPVLib.setOptionString("display-fps", targetFPS.toString())
            MPVLib.setOptionString("override-display-fps", targetFPS.toString())

            // Interpolation requires display-resample
            MPVLib.setOptionString("video-sync", "display-resample")
            MPVLib.setOptionString("interpolation", "yes")
            val mode = decoderPreferences.interpolationMode().get()
            MPVLib.setOptionString("tscale", mode.value)
        } else {
            MPVLib.setOptionString("video-sync", "audio")
        }

        if (decoderPreferences.highQualityScaling().get()) {
            MPVLib.setOptionString("scale", "ewa_lanczossharp")
            MPVLib.setOptionString("cscale", "mitchell")
            MPVLib.setOptionString("dscale", "mitchell")
        }


        // Initialize Debanding
        when (val mode = decoderPreferences.videoDebanding().get()) {
            Debanding.None -> MPVLib.setOptionString("deband", "no")
            Debanding.CPU -> {
                // Handled in buildVFChain via gradfun
            }
            Debanding.GPU -> {
                MPVLib.setOptionString("deband", "yes")
                MPVLib.setOptionString("deband-iterations", decoderPreferences.debandFilter().get().toString())
                MPVLib.setOptionString("deband-threshold", decoderPreferences.debandThreshold().get().toString())
                MPVLib.setOptionString("deband-range", decoderPreferences.debandRange().get().toString())
                MPVLib.setOptionString("deband-grain", decoderPreferences.grainFilter().get().toString())
            }
        }

        val vfChain = buildVFChain(decoderPreferences)
        if (vfChain.isNotEmpty()) {
            MPVLib.setOptionString("vf", vfChain)
        }

        anime4kManager.initialize()
        applyAnime4K(decoderPreferences, anime4kManager, isInit = true)

        MPVLib.setOptionString("msg-level", "all=" + if (networkPreferences.verboseLogging().get()) "v" else "warn")

        MPVLib.setPropertyBoolean("input-default-bindings", true)
        MPVLib.setOptionString("keep-open", "yes")
        MPVLib.setOptionString("idle", "yes")

        // We handle selecting this in the viewmodel
        MPVLib.setOptionString("sid", "no")
        MPVLib.setOptionString("aid", "no")

        MPVLib.setOptionString("tls-verify", "yes")
        MPVLib.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString("http-proxy", "")
        MPVLib.setOptionString("cookies", "yes")

        applyPlaybackStrategy()

        // Let FFmpeg and Android MediaCodec auto-negotiate thread counts and hardware frame buffers.
        // Forcing these values causes instability across fragmented Android GPU/VPU drivers.
        MPVLib.setOptionString("vd-lavc-threads", "0") // Auto
        
        // Stability and compatibility safeguards
        // Let MPV auto-negotiate the GPU context for maximum compatibility
        
        MPVLib.setOptionString("hr-seek", "default")
        MPVLib.setOptionString("hr-seek-framedrop", "yes")
        
        // Enable automatic external subtitle detection
        MPVLib.setOptionString("sub-auto", "fuzzy")
        
        val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        screenshotDir.mkdirs()
        MPVLib.setOptionString("screenshot-directory", screenshotDir.path)

        VideoFilters.entries.forEach {
            if (!it.mpvProperty.startsWith("vf_")) {
                MPVLib.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
            }
        }

        MPVLib.setOptionString("speed", playerPreferences.playerSpeed().get().toString())
        // workaround for <https://github.com/mpv-player/mpv/issues/14651>
        MPVLib.setOptionString("vd-lavc-film-grain", "cpu")

        setupSubtitlesOptions()
        setupAudioOptions()
    }

    override fun observeProperties() {
        for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
    }

    override fun postInitOptions() {
        advancedPreferences.playerStatisticsPage().get().let {
            if (it in 1..5) {
                MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
                MPVLib.command(arrayOf("script-binding", "stats/display-page-$it"))
            } else if (it == 6 || it == 0) {
                // Explicitly ensure internal stats are OFF for Page 6 or Off mode
                // We use a dummy command to ensure the toggle state is predictable
                MPVLib.setPropertyString("user-data/stats/display-page", "0")
            }
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
            return false
        }

        var mapped = KeyMapping.map.get(event.keyCode)
        if (mapped == null) {
            // Fallback to produced glyph
            if (!event.isPrintingKey) {
                if (event.repeatCount == 0) {
                    logcat(LogPriority.DEBUG) { "Unmapped non-printable key ${event.keyCode}" }
                }
                return false
            }

            val ch = event.unicodeChar
            if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
                return false // dead key
            }
            mapped = ch.toChar().toString()
        }

        if (event.repeatCount > 0) {
            return true // eat event but ignore it, mpv has its own key repeat
        }

        val mod: MutableList<String> = mutableListOf()
        event.isShiftPressed && mod.add("shift")
        event.isCtrlPressed && mod.add("ctrl")
        event.isAltPressed && mod.add("alt")
        event.isMetaPressed && mod.add("meta")

        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        mod.add(mapped)
        MPVLib.command(arrayOf(action, mod.joinToString("+")))

        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private val observedProps = mapOf(
        "chapter" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "chapter-list" to MPVLib.mpvFormat.MPV_FORMAT_NONE,
        "track-list" to MPVLib.mpvFormat.MPV_FORMAT_NONE,

        "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "demuxer-cache-time" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "duration" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "volume" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "volume-max" to MPVLib.mpvFormat.MPV_FORMAT_INT64,

        "sid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "secondary-sid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "aid" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "speed" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-zoom" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-pan-x" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-pan-y" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-params/aspect" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "paused-for-cache" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "core-idle" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "seeking" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "eof-reached" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "hwdec-current" to MPVLib.mpvFormat.MPV_FORMAT_STRING,

        "user-data/current-anime/intro-length" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "user-data/aniyomi/show_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/toggle_ui" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/show_panel" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/software_keyboard" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/set_button_title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/reset_button_title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/toggle_button" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/switch_episode" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/pause" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/seek_by" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/seek_to" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/seek_by_with_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/seek_to_with_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "user-data/aniyomi/launch_int_picker" to MPVLib.mpvFormat.MPV_FORMAT_STRING,

        "video-sync" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "tscale" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "video-codec" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "video-params/pixelformat" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "video-params/colorlevels" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
        "video-params/primaries" to MPVLib.mpvFormat.MPV_FORMAT_STRING,

        "video-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-out-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-out-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "dwidth" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "dheight" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-bitrate" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "vo-delayed-frame-count" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "vo-passes" to MPVLib.mpvFormat.MPV_FORMAT_INT64,

        "estimated-vf-fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-params/fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "video-out-params/fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "container-fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "display-fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "estimated-display-fps" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "mistime" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    )

    private fun setupAudioOptions() {
        MPVLib.setOptionString("alang", audioPreferences.preferredAudioLanguages().get())
        MPVLib.setOptionString("audio-delay", (audioPreferences.audioDelay().get() / 1000.0).toString())
        MPVLib.setOptionString("audio-pitch-correction", audioPreferences.enablePitchCorrection().get().toString())
        MPVLib.setOptionString("volume-max", (audioPreferences.volumeBoostCap().get() + 100).toString())
    }

    private fun setupSubtitlesOptions() {
        MPVLib.setOptionString("sub-delay", (subtitlePreferences.subtitlesDelay().get() / 1000.0).toString())
        MPVLib.setOptionString("sub-speed", subtitlePreferences.subtitlesSpeed().get().toString())
        MPVLib.setOptionString(
            "secondary-sub-delay",
            (subtitlePreferences.subtitlesSecondaryDelay().get() / 1000.0).toString(),
        )

        MPVLib.setOptionString("sub-font", subtitlePreferences.subtitleFont().get())
        if (subtitlePreferences.overrideSubsASS().get()) {
            MPVLib.setOptionString("sub-ass-override", "force")
            MPVLib.setOptionString("sub-ass-justify", "yes")
        }
        MPVLib.setOptionString("sub-font-size", subtitlePreferences.subtitleFontSize().get().toString())
        MPVLib.setOptionString("sub-bold", if (subtitlePreferences.boldSubtitles().get()) "yes" else "no")
        MPVLib.setOptionString("sub-italic", if (subtitlePreferences.italicSubtitles().get()) "yes" else "no")
        MPVLib.setOptionString("sub-justify", subtitlePreferences.subtitleJustification().get().value)
        MPVLib.setOptionString("sub-color", subtitlePreferences.textColorSubtitles().get().toColorHexString())
        MPVLib.setOptionString(
            "sub-back-color",
            subtitlePreferences.backgroundColorSubtitles().get().toColorHexString(),
        )
        MPVLib.setOptionString("sub-border-color", subtitlePreferences.borderColorSubtitles().get().toColorHexString())
        MPVLib.setOptionString("sub-border-size", subtitlePreferences.subtitleBorderSize().get().toString())
        MPVLib.setOptionString("sub-border-style", subtitlePreferences.borderStyleSubtitles().get().value)
        MPVLib.setOptionString("sub-shadow-offset", subtitlePreferences.shadowOffsetSubtitles().get().toString())
        MPVLib.setOptionString("sub-pos", subtitlePreferences.subtitlePos().get().toString())
        MPVLib.setOptionString("sub-scale", subtitlePreferences.subtitleFontScale().get().toString())
    }

    fun checkAdaptiveScaling(delayedFrames: Long) {
        if (!decoderPreferences.adaptiveShaderScaling().get() || 
            !decoderPreferences.enableAnime4K().get() || 
            PlayerStats.isAdaptiveDowngraded.value) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdaptiveCheckTime < 5000) return // Check every 5s
        lastAdaptiveCheckTime = currentTime

        // If we have more than 10 delayed frames in a short window and using High quality
        if (delayedFrames > 10 && decoderPreferences.anime4kQuality().get() == "HIGH") {
            logcat("Performance", LogPriority.WARN) { "High frame drops ($delayedFrames) detected. Downgrading Anime4K quality." }
            
            // Downgrade to Balanced
            decoderPreferences.anime4kQuality().set("BALANCED")
            applyAnime4K(decoderPreferences, anime4kManager)
            PlayerStats.isAdaptiveDowngraded.value = true
            
            (context as? PlayerActivity)?.runOnUiThread {
                (context as? PlayerActivity)?.showToast("Performance: Anime4K downgraded to Balanced")
            }
        }
    }
}
