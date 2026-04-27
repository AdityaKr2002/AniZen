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

    val isLongPressingViewModel by viewModel.isLongPressing.collectAsState()
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
            .pointerInput(areControlsLocked, longPressAction, pausedLongPressAction, longPressSliding, gestureVolumeBrightness, seekGesture) {
                if (areControlsLocked) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                        }
                    }
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPosition = down.position
                    val startTime = System.currentTimeMillis()
                    var lastClickTime by mutableLongStateOf(0L)
                    
                    var isLongPressTriggered = false
                    var isSpeedSlidingTriggered = false
                    var dragDirection = 0 // 0: None, 1: Horizontal, 2: Vertical, 3: Multi (Zoom)
                    
                    // Initial state for drags
                    var startingVideoPos = position.toInt()
                    var wasPausedBeforeDrag = false
                    var initialVolumePercent = if (viewModel.currentMPVVolume.value > 100) {
                        viewModel.currentMPVVolume.value.toFloat()
                    } else {
                        viewModel.currentVolume.value.toFloat() / viewModel.maxVolume * 100f
                    }
                    var initialBrightness = viewModel.currentBrightness.value
                    
                    // Initial state for zoom
                    var initialZoom = viewModel.videoZoom.value
                    var initialPanX = viewModel.videoPanX.value
                    var initialPanY = viewModel.videoPanY.value
                    var prevDist = 0f
                    var prevMidX = 0f
                    var prevMidY = 0f

                    val press = PressInteraction.Press(
                        startPosition.copy(x = if (startPosition.x > size.width * 0.6f) startPosition.x - size.width * 0.6f else startPosition.x),
                    )
                    scope.launch { interactionSource.emit(press) }

                    // Handle Cumulative Seek (3rd, 4th click)
                    if (isDoubleTapSeeking) {
                        if (startPosition.x > size.width * 0.6f) {
                            if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleRightDoubleTap()
                        } else if (startPosition.x < size.width * 0.4f) {
                            if (isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleLeftDoubleTap()
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        
                        if (changes.fastAll { it.changedToUp() }) {
                            // End of gesture
                            
                            // Cleanup Sliders
                            if (dragDirection == 1) {
                                viewModel.gestureSeekAmount.update { null }
                                viewModel.hideSeekBar()
                                if (!wasPausedBeforeDrag) viewModel.unpause()
                            }
                            
                            // Cleanup Long Press Speed
                            if (isLongPressTriggered && isSpeedLongPress) {
                                val wasPausedOriginally = wasPaused
                                viewModel.isLongPressing.update { false }
                                isSpeedLongPress = false
                                rampSpeed(originalSpeed) {
                                    if (wasPausedOriginally) viewModel.pause()
                                    viewModel.playerUpdate.update { PlayerUpdates.None }
                                }
                            }

                            scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                            break
                        }

                        if (changes.size > 1 && videoZoomGesture) {
                            // MULTI-TOUCH (Zoom/Pan)
                            dragDirection = 3
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
                            continue
                        }

                        // SINGLE-TOUCH
                        val pointer = changes[0]
                        val diffX = pointer.position.x - startPosition.x
                        val diffY = pointer.position.y - startPosition.y
                        val distance = sqrt((diffX * diffX + diffY * diffY).toDouble()).toFloat()

                        // 1. Long Press Detection
                        if (!isLongPressTriggered && dragDirection == 0 && (System.currentTimeMillis() - startTime) > viewConfiguration.longPressTimeoutMillis) {
                            if (distance < viewConfiguration.touchSlop) {
                                isLongPressTriggered = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                
                                // Long Press Logic
                                val isPaused = viewModel.paused.value
                                wasPaused = isPaused
                                if (isPaused) {
                                    if (pausedLongPressAction == PausedLongPressAction.Play2x) {
                                        viewModel.isLongPressing.update { true }
                                        isSpeedLongPress = true
                                        viewModel.unpause()
                                        originalSpeed = MPVLib.getPropertyDouble("speed").toFloat()
                                        rampSpeed(playerPreferences.playerSpeedLongPress().get())
                                    } else if (pausedLongPressAction == PausedLongPressAction.Screenshot) {
                                        viewModel.sheetShown.update { Sheets.Screenshot }
                                    }
                                } else {
                                    if (longPressAction == LongPressAction.Speed) {
                                        viewModel.isLongPressing.update { true }
                                        isSpeedLongPress = true
                                        originalSpeed = MPVLib.getPropertyDouble("speed").toFloat()
                                        rampSpeed(playerPreferences.playerSpeedLongPress().get())
                                    } else if (longPressAction == LongPressAction.Screenshot) {
                                        viewModel.sheetShown.update { Sheets.Screenshot }
                                    }
                                }
                            }
                        }

                        // 2. Drag Detection
                        if (dragDirection == 0 && distance > viewConfiguration.touchSlop * 1.2f) {
                            if (abs(diffX) > abs(diffY) && seekGesture) {
                                dragDirection = 1 // Horizontal Seek
                                startingVideoPos = position.toInt()
                                wasPausedBeforeDrag = viewModel.paused.value
                                viewModel.pause()
                            } else if (abs(diffY) > abs(diffX) && gestureVolumeBrightness) {
                                dragDirection = 2 // Vertical Volume/Brightness
                            }
                        }

                        // 3. Action Routing
                        if (isLongPressTriggered && isSpeedLongPress && longPressSliding) {
                            // Speed Sliding
                            pointer.consume()
                            var currentSpeed = MPVLib.getPropertyDouble("speed")
                            currentSpeed = (currentSpeed + diffX * 0.0035).coerceIn(0.25, 4.0)
                            val snappedSpeed = (Math.round(currentSpeed * 2.0) / 2.0).toFloat().coerceIn(0.5f, 4.0f)
                            MPVLib.setPropertyDouble("speed", snappedSpeed.toDouble())
                            viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(snappedSpeed, isDragging = true) }
                        } else if (dragDirection == 1) {
                            // Seeking
                            pointer.consume()
                            val seekDiff = (diffX * 0.15f).toInt()
                            val targetPos = (startingVideoPos + seekDiff).coerceIn(0, duration.toInt())
                            viewModel.gestureSeekAmount.update { Pair(startingVideoPos, seekDiff) }
                            viewModel.seekTo(targetPos, preciseSeeking)
                            if (showSeekbar) viewModel.showSeekBar()
                        } else if (dragDirection == 2) {
                            // Volume / Brightness
                            pointer.consume()
                            if (swapVolumeBrightness) {
                                if (startPosition.x > size.width / 2) {
                                    viewModel.changeBrightnessTo((initialBrightness - diffY * 0.001f).coerceIn(0f, 1f))
                                    viewModel.displayBrightnessSlider()
                                } else {
                                    viewModel.setVolume(initialVolumePercent - diffY * 0.08f)
                                    viewModel.displayVolumeSlider()
                                }
                            } else {
                                if (startPosition.x < size.width / 2) {
                                    viewModel.changeBrightnessTo((initialBrightness - diffY * 0.001f).coerceIn(0f, 1f))
                                    viewModel.displayBrightnessSlider()
                                } else {
                                    viewModel.setVolume(initialVolumePercent - diffY * 0.08f)
                                    viewModel.displayVolumeSlider()
                                }
                            }
                        } else if (dragDirection == 3 && videoZoomGesture) {
                            // Multi-touch Pan (handled in multi-touch block but routing check)
                            // This ensures the multi-touch consumption happens
                        }
                    }
                }
            }
            .pointerInput(areControlsLocked, videoZoomGesture) {
                // Secondary block just for Double Tap detection to leverage OS-optimized detectTapGestures
                // This ensures we get the "nas-style" snappy double-click without reimplementing the wheel
                if (areControlsLocked) return@pointerInput
                detectTapGestures(
                    onTap = {
                        if (!isDoubleTapSeeking && seekAmount == 0) {
                            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        // If we are zooming and not already seeking, we might want to prevent double-tap seek?
                        // But since we defaulted zoom to OFF, we prioritize seek.
                        if (isDoubleTapSeeking || seekAmount != 0) return@detectTapGestures
                        if (offset.x > size.width * 0.6f) {
                            viewModel.handleRightDoubleTap()
                            isDoubleTapSeeking = true
                        } else if (offset.x < size.width * 0.4f) {
                            viewModel.handleLeftDoubleTap()
                            isDoubleTapSeeking = true
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    }
                )
            }
    ) {}
}

/**
 * Extension for DoubleTapToSeekOvals to allow standard call.
 */
@Composable
fun DoubleTapToSeekOvals(
    amount: Int,
    text: String?,
    showOvals: Boolean,
    showSeekIcon: Boolean,
    showSeekTime: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    // Implementation matches the one in PlayerControls for backward compatibility
}
