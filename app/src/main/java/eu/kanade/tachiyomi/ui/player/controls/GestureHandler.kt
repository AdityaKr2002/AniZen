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

package eu.kanade.tachiyomi.ui.player.controls

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.ui.player.LongPressAction
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.PausedLongPressAction
import eu.kanade.tachiyomi.ui.player.PlayerUpdates
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.videoDisplaySize
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun GestureHandler(
    viewModel: PlayerViewModel,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val gesturePreferences = remember { Injekt.get<GesturePreferences>() }
    val audioPreferences = remember { Injekt.get<AudioPreferences>() }

    val panelShown by viewModel.panelShown.collectAsState()
    val allowGesturesInPanels by playerPreferences.allowGestures().collectAsStatePref()
    val duration by viewModel.duration.collectAsState()
    val position by viewModel.pos.collectAsState()
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val seekAmount by viewModel.doubleTapSeekAmount.collectAsState()
    val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
    var isDoubleTapSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(seekAmount) {
        delay(800)
        isDoubleTapSeeking = false
        viewModel.updateSeekAmount(0)
        viewModel.updateSeekText(null)
        delay(100)
        viewModel.hideSeekBar()
    }

    val gestureVolumeBrightness = gesturePreferences.gestureVolumeBrightness().get()
    val swapVolumeBrightness by gesturePreferences.swapVolumeBrightness().collectAsStatePref()
    val seekGesture by gesturePreferences.gestureHorizontalSeek().collectAsStatePref()
    val videoZoomGesture by gesturePreferences.videoZoomGesture().collectAsStatePref()
    val preciseSeeking by gesturePreferences.playerSmoothSeek().collectAsStatePref()
    val showSeekbar by gesturePreferences.showSeekBar().collectAsStatePref()
    
    val longPressAction by gesturePreferences.longPressAction().collectAsStatePref()
    val pausedLongPressAction by gesturePreferences.pausedLongPressAction().collectAsStatePref()
    val longPressSliding by gesturePreferences.gestureLongPressSpeedSliding().collectAsStatePref()

    val currentVolume by viewModel.currentVolume.collectAsState()
    val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val volumeBoostingCap = audioPreferences.volumeBoostCap().get()

    val context = LocalContext.current
    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    
    var speedRampJob by remember { mutableStateOf<Job?>(null) }
    var originalSpeed by remember { mutableFloatStateOf(1f) }

    fun rampSpeed(targetSpeed: Float, onComplete: () -> Unit = {}) {
        speedRampJob?.cancel()
        speedRampJob = scope.launch {
            var currentSpeed = MPVLib.getPropertyDouble("speed").toFloat()
            val step = if (targetSpeed > currentSpeed) 0.1f else -0.1f
            
            while (if (step > 0) currentSpeed < targetSpeed else currentSpeed > targetSpeed) {
                currentSpeed += step
                if (step > 0 && currentSpeed > targetSpeed) currentSpeed = targetSpeed
                if (step < 0 && currentSpeed < targetSpeed) currentSpeed = targetSpeed
                
                MPVLib.setPropertyDouble("speed", currentSpeed.toDouble())
                viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(currentSpeed, false) }
                delay(16)
            }
            MPVLib.setPropertyDouble("speed", targetSpeed.toDouble())
            viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(targetSpeed, false) }
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeGestures)
            // BLOCK 1: Taps & Long Press (OS Optimized)
            .pointerInput(areControlsLocked, longPressAction, pausedLongPressAction, isDoubleTapSeeking, seekAmount) {
                if (areControlsLocked) {
                    detectTapGestures(onTap = { if (controlsShown) viewModel.hideControls() else viewModel.showControls() })
                    return@pointerInput
                }
                detectTapGestures(
                    onTap = {
                        if (!isDoubleTapSeeking && seekAmount == 0) {
                            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isDoubleTapSeeking || seekAmount != 0) return@detectTapGestures
                        if (offset.x > size.width * 3 / 5) {
                            if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleRightDoubleTap()
                            isDoubleTapSeeking = true
                        } else if (offset.x < size.width * 2 / 5) {
                            if (isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleLeftDoubleTap()
                            isDoubleTapSeeking = true
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    },
                    onPress = { offset ->
                        if (panelShown != Panels.None && !allowGesturesInPanels) {
                            viewModel.panelShown.update { Panels.None }
                        }
                        val press = PressInteraction.Press(
                            offset.copy(x = if (offset.x > size.width * 3 / 5) offset.x - size.width * 0.6f else offset.x),
                        )
                        // Hyper-Lane Cumulative Seek (Instant 3rd/4th click)
                        if (isDoubleTapSeeking) {
                            if (offset.x > size.width * 3 / 5) {
                                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleRightDoubleTap()
                            } else if (offset.x < size.width * 2 / 5) {
                                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleLeftDoubleTap()
                            } else {
                                viewModel.handleCenterDoubleTap()
                            }
                        }
                        scope.launch { interactionSource.emit(press) }
                        try {
                            awaitRelease()
                        } finally {
                            scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                        }
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val isPaused = viewModel.paused.value
                        if (isPaused) {
                            when (pausedLongPressAction) {
                                PausedLongPressAction.Screenshot -> viewModel.sheetShown.update { Sheets.Screenshot }
                                PausedLongPressAction.Play2x -> {
                                    viewModel.isLongPressing.update { true }
                                    viewModel.unpause()
                                    originalSpeed = MPVLib.getPropertyDouble("speed").toFloat()
                                    rampSpeed(playerPreferences.playerSpeedLongPress().get())
                                }
                                else -> {}
                            }
                        } else {
                            if (longPressAction == LongPressAction.Speed) {
                                viewModel.isLongPressing.update { true }
                                originalSpeed = MPVLib.getPropertyDouble("speed").toFloat()
                                rampSpeed(playerPreferences.playerSpeedLongPress().get())
                            } else if (longPressAction == LongPressAction.Screenshot) {
                                viewModel.sheetShown.update { Sheets.Screenshot }
                            }
                        }
                    }
                )
            }
            // BLOCK 2: Horizontal Drag (Seeking)
            .pointerInput(areControlsLocked, seekGesture) {
                if (areControlsLocked || !seekGesture) return@pointerInput
                var startingPosition = 0
                var startingX = 0f
                var wasPausedBeforeDrag = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        startingPosition = position.toInt()
                        startingX = it.x
                        wasPausedBeforeDrag = viewModel.paused.value
                        viewModel.pause()
                    },
                    onDragEnd = {
                        viewModel.gestureSeekAmount.update { null }
                        viewModel.hideSeekBar()
                        if (!wasPausedBeforeDrag) viewModel.unpause()
                    },
                    onDragCancel = {
                        viewModel.gestureSeekAmount.update { null }
                        viewModel.hideSeekBar()
                        if (!wasPausedBeforeDrag) viewModel.unpause()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val it = calculateNewHorizontalGestureValue(startingPosition.toFloat(), startingX, change.position.x, 0.15f)
                    viewModel.gestureSeekAmount.update { _ ->
                        Pair(startingPosition, (it - startingPosition).toInt().coerceIn(0 - startingPosition, (duration - startingPosition).toInt()))
                    }
                    viewModel.seekTo(it.toInt().coerceIn(0, duration.toInt()), preciseSeeking)
                    if (showSeekbar) viewModel.showSeekBar()
                }
            }
            // BLOCK 3: Vertical Drag (Volume/Brightness)
            .pointerInput(areControlsLocked, gestureVolumeBrightness) {
                if (areControlsLocked || !gestureVolumeBrightness) return@pointerInput
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f
                val brightnessGestureSens = 0.001f
                val volumeGestureSens = 0.001f * viewModel.maxVolume
                val mpvVolumeGestureSens = 0.001f * volumeBoostingCap

                detectVerticalDragGestures(
                    onDragStart = {
                        startingY = it.y
                        mpvVolumeStartingY = it.y
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    }
                ) { change, _ ->
                    change.consume()
                    val diffY = change.position.y - startingY
                    if (swapVolumeBrightness) {
                        if (change.position.x > size.width / 2) {
                            viewModel.changeBrightnessTo((originalBrightness - diffY * brightnessGestureSens).coerceIn(0f, 1f))
                            viewModel.displayBrightnessSlider()
                        } else {
                            viewModel.setVolume((originalVolume - diffY * volumeGestureSens).toInt())
                            viewModel.displayVolumeSlider()
                        }
                    } else {
                        if (change.position.x < size.width / 2) {
                            viewModel.changeBrightnessTo((originalBrightness - diffY * brightnessGestureSens).coerceIn(0f, 1f))
                            viewModel.displayBrightnessSlider()
                        } else {
                            viewModel.setVolume((originalVolume - diffY * volumeGestureSens).toInt())
                            viewModel.displayVolumeSlider()
                        }
                    }
                }
            }
            // BLOCK 4: Zoom/Pan (Isolated Pro Features)
            .pointerInput(areControlsLocked, videoZoomGesture) {
                if (areControlsLocked || !videoZoomGesture) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var prevDist = 0f
                    var prevMidX = 0f
                    var prevMidY = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.fastAll { it.changedToUp() }) break
                        if (changes.size < 2) continue // Only 2+ fingers for zoom

                        val p1 = changes[0].position
                        val p2 = changes[1].position
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val midX = (p1.x + p2.x) / 2f
                        val midY = (p1.y + p2.y) / 2f

                        if (prevDist == 0f) {
                            prevDist = dist
                            prevMidX = midX
                            prevMidY = midY
                        } else {
                            val zoomDelta = ln((dist / prevDist).toDouble()).toFloat() * 1.2f
                            val newZoom = (viewModel.videoZoom.value + zoomDelta).coerceIn(-1f, 3f)
                            viewModel.setVideoZoom(newZoom)
                            viewModel.playerUpdate.update { PlayerUpdates.VideoZoom(newZoom) }

                            val scale = 2f.pow(newZoom)
                            val (bw, bh) = videoDisplaySize(size)
                            val panDX = midX - prevMidX
                            val panDY = midY - prevMidY
                            val maxPan = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)
                            val newPanX = (viewModel.videoPanX.value + panDX / (bw * scale)).coerceIn(-maxPan, maxPan)
                            val newPanY = (viewModel.videoPanY.value + panDY / (bh * scale)).coerceIn(-maxPan, maxPan)
                            viewModel.setVideoPan(newPanX, newPanY)
                            
                            prevDist = dist
                            prevMidX = midX
                            prevMidY = midY
                        }
                        changes.fastForEach { it.consume() }
                    }
                }
            }
    ) {}
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
    return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
    return originalValue + ((newX - startingX) * sensitivity)
}
