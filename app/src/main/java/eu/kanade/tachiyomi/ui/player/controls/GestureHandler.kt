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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import eu.kanade.tachiyomi.ui.player.LongPressAction
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.PausedLongPressAction
import eu.kanade.tachiyomi.ui.player.PlayerUpdates
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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
    val preciseSeeking by gesturePreferences.playerSmoothSeek().collectAsStatePref()
    val showSeekbar by gesturePreferences.showSeekBar().collectAsStatePref()
    
    val longPressAction by gesturePreferences.longPressAction().collectAsStatePref()
    val pausedLongPressAction by gesturePreferences.pausedLongPressAction().collectAsStatePref()
    val longPressSliding by playerPreferences.adjustSpeedOnDrag().collectAsStatePref()

    var isLongPressing by remember { mutableStateOf(false) }
    val currentVolume by viewModel.currentVolume.collectAsState()
    val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val volumeBoostingCap = audioPreferences.volumeBoostCap().get()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val scope = rememberCoroutineScope()
    var speedRampJob by remember { mutableStateOf<Job?>(null) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var originalSpeed by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeGestures)
            .pointerInput(areControlsLocked, longPressAction, pausedLongPressAction, longPressSliding, controlsShown) {
                if (areControlsLocked || isTv) {
                    detectTapGestures(
                        onTap = { if (controlsShown) viewModel.hideControls() else viewModel.showControls() }
                    )
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPosition = down.position
                    originalSpeed = viewModel.playbackSpeed.value
                    
                    val press = PressInteraction.Press(
                        down.position.copy(x = if (down.position.x > size.width * 3 / 5) down.position.x - size.width * 0.6f else down.position.x),
                    )
                    scope.launch { interactionSource.emit(press) }

                    longPressJob?.cancel()
                    longPressJob = scope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        val isPaused = viewModel.paused.value
                        if (isPaused) {
                            when (pausedLongPressAction) {
                                PausedLongPressAction.Screenshot -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.sheetShown.update { Sheets.Screenshot }
                                }
                                PausedLongPressAction.Play2x -> {
                                    isLongPressing = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.unpause()
                                    val targetSpeed = 2.0f
                                    speedRampJob?.cancel()
                                    speedRampJob = scope.launch {
                                        val currentSpeed = MPVLib.getPropertyDouble("speed")
                                        val dur = 200L
                                        val startTime = System.currentTimeMillis()
                                        while (System.currentTimeMillis() - startTime < dur) {
                                            val progress = (System.currentTimeMillis() - startTime).toFloat() / dur
                                            val s = currentSpeed + (targetSpeed.toDouble() - currentSpeed) * progress
                                            MPVLib.setPropertyDouble("speed", s)
                                            delay(32)
                                        }
                                        MPVLib.setPropertyDouble("speed", targetSpeed.toDouble())
                                    }
                                    viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(targetSpeed, false) }
                                }
                                else -> {}
                            }
                        } else {
                            if (longPressAction == LongPressAction.Speed) {
                                isLongPressing = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val targetSpeed = playerPreferences.playerSpeedLongPress().get()
                                speedRampJob?.cancel()
                                speedRampJob = scope.launch {
                                    val currentSpeed = MPVLib.getPropertyDouble("speed")
                                    val dur = 200L
                                    val startTime = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - startTime < dur) {
                                        val progress = (System.currentTimeMillis() - startTime).toFloat() / dur
                                        val s = currentSpeed + (targetSpeed.toDouble() - currentSpeed) * progress
                                        MPVLib.setPropertyDouble("speed", s)
                                        delay(32)
                                    }
                                    MPVLib.setPropertyDouble("speed", targetSpeed.toDouble())
                                }
                                viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(targetSpeed, false) }
                            } else if (longPressAction == LongPressAction.Screenshot) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sheetShown.update { Sheets.Screenshot }
                            }
                        }
                    }

                    var up: androidx.compose.ui.input.pointer.PointerInputChange? = null
                    var lastX = down.position.x
                    var lastHapticSpeed = -1f
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        
                        if (pointer.changedToUp()) {
                            up = pointer
                            break
                        }
                        
                        val distance = (pointer.position - startPosition).getDistance()
                        if (!isLongPressing) {
                            if (distance > viewConfiguration.touchSlop) {
                                longPressJob?.cancel()
                                // If it's a drag, let other pointerInputs handle it
                                if (Math.abs(pointer.position.y - startPosition.y) > Math.abs(pointer.position.x - startPosition.x)) {
                                    // Vertical drag (volume/brightness)
                                    break
                                }
                            }
                        } else {
                            if (longPressSliding && !viewModel.paused.value && longPressAction == LongPressAction.Speed) {
                                val diffX = pointer.position.x - lastX
                                if (Math.abs(diffX) > 1f) {
                                    val currentSpeed = MPVLib.getPropertyDouble("speed")
                                    val newSpeed = (currentSpeed + diffX * 0.005).coerceIn(0.5, 4.0)
                                    val snappedSpeed = (Math.round(newSpeed * 2.0) / 2.0).toFloat().coerceIn(0.5f, 4.0f)
                                    
                                    speedRampJob?.cancel() // Stop the initial ramp if they start sliding right away
                                    MPVLib.setPropertyDouble("speed", snappedSpeed.toDouble())
                                    viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed(snappedSpeed, isDragging = true) }
                                    
                                    if (snappedSpeed != lastHapticSpeed) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        lastHapticSpeed = snappedSpeed
                                    }
                                    lastX = pointer.position.x
                                }
                                pointer.consume()
                            }
                        }
                    }

                    longPressJob?.cancel()

                    if (isLongPressing) {
                        val wasPausedOriginally = viewModel.paused.value || (pausedLongPressAction == PausedLongPressAction.Play2x)
                        isLongPressing = false
                        speedRampJob?.cancel()
                        speedRampJob = scope.launch {
                            if (!isTv) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val currentSpeed = MPVLib.getPropertyDouble("speed")
                            val dur = 200L
                            val startTime = System.currentTimeMillis()
                            while (System.currentTimeMillis() - startTime < dur) {
                                val progress = (System.currentTimeMillis() - startTime).toFloat() / dur
                                val s = currentSpeed + (originalSpeed.toDouble() - currentSpeed) * progress
                                MPVLib.setPropertyDouble("speed", s)
                                delay(32)
                            }
                            MPVLib.setPropertyDouble("speed", originalSpeed.toDouble())
                            if (wasPausedOriginally) {
                                viewModel.pause()
                            }
                            viewModel.playerUpdate.update { PlayerUpdates.None }
                        }
                    } else if (up != null) {
                        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        if (secondDown == null) { // Single tap
                            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                        } else { // Double tap
                            if (secondDown.position.x > size.width * 3 / 5) {
                                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleRightDoubleTap()
                                isDoubleTapSeeking = true
                            } else if (secondDown.position.x < size.width * 2 / 5) {
                                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleLeftDoubleTap()
                                isDoubleTapSeeking = true
                            } else {
                                viewModel.handleCenterDoubleTap()
                            }
                        }
                    }
                    scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                }
            }
            .pointerInput(areControlsLocked) {
                if (!seekGesture || areControlsLocked) return@pointerInput
                var startingPosition = position.toInt()
                var startingX = 0f
                var wasPlayerAlreadyPause = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        longPressJob?.cancel()
                        startingPosition = position.toInt()
                        startingX = it.x
                        wasPlayerAlreadyPause = viewModel.paused.value
                        viewModel.pause()
                    },
                    onDragEnd = {
                        viewModel.gestureSeekAmount.update { null }
                        viewModel.hideSeekBar()
                        if (!wasPlayerAlreadyPause) viewModel.unpause()
                    },
                ) { change, dragAmount ->
                    if (position <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
                    if (position >= duration && dragAmount > 0) return@detectHorizontalDragGestures
                    calculateNewHorizontalGestureValue(startingPosition, startingX, change.position.x, 0.15f).let {
                        viewModel.gestureSeekAmount.update { _ ->
                            Pair(
                                startingPosition,
                                (it - startingPosition)
                                    .coerceIn(0 - startingPosition, (duration - startingPosition).toInt()),
                            )
                        }
                        viewModel.seekTo(it.coerceIn(0, duration.toInt()), preciseSeeking)
                    }

                    if (showSeekbar) viewModel.showSeekBar()
                }
            }
            .pointerInput(areControlsLocked) {
                if (!gestureVolumeBrightness || areControlsLocked) return@pointerInput
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = currentVolume
                var originalMPVVolume = currentMPVVolume
                var originalBrightness = currentBrightness
                val brightnessGestureSens = 0.001f
                val volumeGestureSens = 0.03f
                val mpvVolumeGestureSens = 0.02f
                val isIncreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 < volumeBoostingCap &&
                        it < 0
                }
                val isDecreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 in 1..volumeBoostingCap &&
                        it > 0
                }
                detectVerticalDragGestures(
                    onDragEnd = { startingY = 0f },
                    onDragStart = {
                        longPressJob?.cancel()
                        startingY = 0f
                        mpvVolumeStartingY = 0f
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    },
                ) { change, amount ->
                    val changeVolume: () -> Unit = {
                        if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
                            if (mpvVolumeStartingY == 0f) {
                                startingY = 0f
                                originalVolume = currentVolume
                                mpvVolumeStartingY = change.position.y
                            }
                            viewModel.changeMPVVolumeTo(
                                calculateNewVerticalGestureValue(
                                    originalMPVVolume,
                                    mpvVolumeStartingY,
                                    change.position.y,
                                    mpvVolumeGestureSens,
                                )
                                    .coerceIn(100..volumeBoostingCap + 100),
                            )
                        } else {
                            if (startingY == 0f) {
                                mpvVolumeStartingY = 0f
                                originalMPVVolume = currentMPVVolume
                                startingY = change.position.y
                            }
                            val newVal = calculateNewVerticalGestureValue(
                                    originalVolume,
                                    startingY,
                                    change.position.y,
                                    volumeGestureSens,
                                )
                            if ((newVal <= 0 && currentVolume > 0) || (newVal >= viewModel.maxVolume && currentVolume < viewModel.maxVolume)) {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            viewModel.changeVolumeTo(newVal)
                        }
                        viewModel.displayVolumeSlider()
                    }
                    val changeBrightness: () -> Unit = {
                        if (startingY == 0f) startingY = change.position.y
                        val newVal = calculateNewVerticalGestureValue(
                                originalBrightness,
                                startingY,
                                change.position.y,
                                brightnessGestureSens,
                            )
                        if ((newVal <= 0f && currentBrightness > 0f) || (newVal >= 1f && currentBrightness < 1f)) {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.changeBrightnessTo(newVal)
                        viewModel.displayBrightnessSlider()
                    }
                    if (swapVolumeBrightness) {
                        if (change.position.x > size.width / 2) changeBrightness() else changeVolume()
                    } else {
                        if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
                    }
                }
            },
    ) {}
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
    return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
    return originalValue + ((startingY - newY) * sensitivity)
}

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int {
    return originalValue + ((newX - startingX) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
    return originalValue + ((newX - startingX) * sensitivity)
}
