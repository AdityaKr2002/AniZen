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

import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import eu.kanade.tachiyomi.ui.player.utils.Anime4KManager
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import logcat.logcat

fun applyFilter(filter: VideoFilters, value: Int, prefs: DecoderPreferences, manager: Anime4KManager? = null) {
    val property = filter.mpvProperty
    
    when (property) {
        "vf_sharpen" -> {
            if (manager != null) {
                applyGLSLShaders(prefs, manager)
            }
        }
        "vf_blur" -> {
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        else -> MPVLib.setPropertyInt(property, value)
    }
}

fun applyDebandMode(mode: Debanding, prefs: DecoderPreferences) {
    when (mode) {
        Debanding.None -> {
            MPVLib.setPropertyBoolean("deband", false)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        Debanding.CPU -> {
            MPVLib.setPropertyBoolean("deband", false)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        Debanding.GPU -> {
            MPVLib.setPropertyBoolean("deband", true)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
            // Apply current GPU settings
            DebandSettings.entries.forEach {
                MPVLib.setPropertyInt(it.mpvProperty, it.preference(prefs).get())
            }
        }
    }
}

fun applyDebandSetting(setting: DebandSettings, value: Int) {
    MPVLib.setPropertyInt(setting.mpvProperty, value)
}

fun buildVFChain(decoderPreferences: DecoderPreferences): String {
    val blur = decoderPreferences.blurFilter().get()
    val deband = decoderPreferences.videoDebanding().get()
    val useYuv420p = decoderPreferences.useYUV420P().get()

    val cpuFilters = mutableListOf<String>()

    if (deband == Debanding.CPU) {
        cpuFilters.add("deband=1:1:64:16")
    }

    if (blur > 0) {
        val luma = blur / 10f
        // Blur both luma and chroma planes to prevent green artifacts
        cpuFilters.add("boxblur=$luma:1:$luma:1")
    }

    val finalChain = when {
        cpuFilters.isNotEmpty() -> {
            // If any filter requires CPU processing, we MUST ensure a stable pixel format 
            // inside the lavfi context.
            "lavfi=[format=yuv420p,${cpuFilters.joinToString(",")}]"
        }
        useYuv420p -> {
            // Use native mpv filter instead of lavfi wrapper for better performance
            "format=yuv420p"
        }
        else -> ""
    }

    updateHardwareDecoding(decoderPreferences, hasCpuFilters = cpuFilters.isNotEmpty())
    return finalChain
}

private var currentHasShaders = false
private var currentHasCpuFilters = false

fun updateHardwareDecoding(prefs: DecoderPreferences, hasShaders: Boolean = currentHasShaders, hasCpuFilters: Boolean = currentHasCpuFilters) {
    currentHasShaders = hasShaders
    currentHasCpuFilters = hasCpuFilters

    // Optimization: Only update HW decoder if playback is actually active
    // This prevents triggering decoder re-negotiation (and black screens) while paused.
    val isPaused = MPVLib.getPropertyBoolean("pause") ?: true
    if (!isPaused) {
        // MPV Android Opaque Surface limitation:
        // To apply GLSL shaders or CPU filters without opengl-pbo (which crashes Mali GPUs), 
        // we MUST use mediacodec-copy so the shader/CPU can access the pixel data in RAM.
        if (hasShaders || hasCpuFilters) {
            MPVLib.setPropertyString("hwdec", "mediacodec-copy")
        } else {
            val hwdec = if (prefs.tryHWDecoding().get()) "auto" else "no"
            MPVLib.setPropertyString("hwdec", hwdec)
        }
    }
}

fun applyTheme(theme: VideoFilterTheme, prefs: DecoderPreferences, manager: Anime4KManager? = null) {
    prefs.brightnessFilter().set(theme.brightness)
    prefs.contrastFilter().set(theme.contrast)
    prefs.saturationFilter().set(theme.saturation)
    prefs.gammaFilter().set(theme.gamma)
    prefs.hueFilter().set(theme.hue)
    prefs.sharpenFilter().set(theme.sharpen)
    prefs.blurFilter().set(0)
    
    // Reset deband
    prefs.debandFilter().set(0)
    prefs.grainFilter().set(0)
    prefs.debandThreshold().set(32)
    prefs.debandRange().set(16)

    // Apply direct properties
    MPVLib.setPropertyInt("brightness", theme.brightness)
    MPVLib.setPropertyInt("contrast", theme.contrast)
    MPVLib.setPropertyInt("saturation", theme.saturation)
    MPVLib.setPropertyInt("gamma", theme.gamma)
    MPVLib.setPropertyInt("hue", theme.hue)
    
    // Apply VF chain once
    MPVLib.setPropertyString("vf", buildVFChain(prefs))
    
    if (manager != null) {
        applyGLSLShaders(prefs, manager)
    }
    
    // Reset deband engine properties
    MPVLib.setPropertyBoolean("deband", false)
    MPVLib.setPropertyInt("deband-iterations", 1)
    MPVLib.setPropertyInt("deband-threshold", 32)
    MPVLib.setPropertyInt("deband-range", 16)
    MPVLib.setPropertyInt("deband-grain", 48)
}

fun applyGLSLShaders(prefs: DecoderPreferences, manager: Anime4KManager, isInit: Boolean = false) {
    // 1. Collect Shader Chains
    val shaderPaths = mutableListOf<String>()
    
    // Anime4K
    val anime4kEnabled = prefs.enableAnime4K().get()
    val gpuNext = prefs.gpuNext().get()
    
    if (anime4kEnabled && gpuNext) {
        logcat("Anime4K", LogPriority.WARN) { "Anime4K is incompatible with gpu-next. Skipping Anime4K." }
    } else if (anime4kEnabled) {
        val mode = try { Anime4KManager.Mode.valueOf(prefs.anime4kMode().get()) } catch (e: Exception) { Anime4KManager.Mode.OFF }
        val quality = try { Anime4KManager.Quality.valueOf(prefs.anime4kQuality().get()) } catch (e: Exception) { Anime4KManager.Quality.BALANCED }
        
        manager.initialize()
        val anime4kChain = manager.getShaderChain(mode, quality)
        if (anime4kChain.isNotEmpty()) {
            shaderPaths.add(anime4kChain)
        }
    }
    
    // Adaptive Sharpen
    val sharpenIntensity = prefs.sharpenFilter().get()
    if (sharpenIntensity > 0) {
        val sharpenPath = manager.getAdaptiveSharpenShader(sharpenIntensity)
        if (sharpenPath.isNotEmpty()) {
            shaderPaths.add(sharpenPath)
        }
    }

    // 2. Apply combined chain
    val combinedChain = shaderPaths.joinToString(":")
    logcat("GLSL", LogPriority.DEBUG) { "Applying GLSL Shaders: $combinedChain" }
    
    updateHardwareDecoding(prefs, hasShaders = combinedChain.isNotEmpty())
    
    if (combinedChain.isNotEmpty()) {
        if (isInit) {
            MPVLib.setOptionString("glsl-shaders", combinedChain)
        } else {
            MPVLib.setPropertyString("glsl-shaders", combinedChain)
        }
    } else {
        if (isInit) {
            MPVLib.setOptionString("glsl-shaders", "")
        } else {
            MPVLib.setPropertyString("glsl-shaders", "")
        }
    }
}

@Deprecated("Use applyGLSLShaders instead", ReplaceWith("applyGLSLShaders(prefs, manager, isInit)"))
fun applyAnime4K(prefs: DecoderPreferences, manager: Anime4KManager, isInit: Boolean = false) {
    applyGLSLShaders(prefs, manager, isInit)
}
