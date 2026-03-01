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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.kanade.presentation.theme.playerRippleConfiguration
import eu.kanade.tachiyomi.ui.player.PlayerUpdates
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.controls.components.DoubleTapToSeekOvals
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@Composable
fun PlayerControls(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val subtitlePreferences = remember { Injekt.get<SubtitlePreferences>() }
    val interactionSource = remember { MutableInteractionSource() }
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val seekBarShown by viewModel.seekBarShown.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingEpisode by viewModel.isLoadingEpisode.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val position by viewModel.pos.collectAsState()
    val paused by viewModel.paused.collectAsState()
    val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
    val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
    val seekText by viewModel.seekText.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()

    val playerTimeToDisappear by playerPreferences.playerTimeToDisappear().collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var resetControls by remember { mutableStateOf(true) }

    val customButtons by viewModel.customButtons.collectAsState()
    val customButton by viewModel.primaryButton.collectAsState()

    LaunchedEffect(
        controlsShown,
        paused,
        isSeeking,
        resetControls,
    ) {
        if (controlsShown && !paused && !isSeeking) {
            delay(playerTimeToDisappear.toLong())
            viewModel.hideControls()
        }
    }

    val transparentOverlay by animateFloatAsState(
        if (controlsShown && !areControlsLocked) .8f else 0f,
        animationSpec = playerControlsExitAnimationSpec(),
        label = "controls_transparent_overlay",
    )
    GestureHandler(
        viewModel = viewModel,
        interactionSource = interactionSource,
    )
    DoubleTapToSeekOvals(doubleTapSeekAmount, seekText, interactionSource)
    CompositionLocalProvider(
        LocalRippleConfiguration provides playerRippleConfiguration,
        LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
        LocalContentColor provides Color.White,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
        ) {
            ConstraintLayout(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            Pair(0f, Color.Black),
                            Pair(1f, Color.Transparent),
                        ),
                        alpha = transparentOverlay,
                    ),
            ) {
                val (
                    topControls,
                    centerControls,
                    bottomControls,
                    playerUpdate,
                    lockedControls,
                ) = createRefs()

                TopControls(
                    viewModel = viewModel,
                    modifier = Modifier.constrainAs(topControls) {
                        linkTo(parent.start, parent.end)
                        top.linkTo(parent.top)
                    },
                )

                MiddleControls(
                    viewModel = viewModel,
                    modifier = Modifier.constrainAs(centerControls) {
                        centerTo(parent)
                    },
                )

                BottomControls(
                    viewModel = viewModel,
                    modifier = Modifier.constrainAs(bottomControls) {
                        linkTo(parent.start, parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                    isSeeking = isSeeking,
                    onSeeking = { isSeeking = it },
                )

                val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
                val aspectRatio by viewModel.aspectRatio.collectAsState()
                Box(
                    modifier = Modifier.constrainAs(playerUpdate) {
                        linkTo(parent.start, parent.end)
                        linkTo(parent.top, parent.bottom, bias = 0.2f)
                    },
                ) {
                    when (currentPlayerUpdate) {
                        is PlayerUpdates.DoubleSpeed -> eu.kanade.tachiyomi.ui.player.controls.components.DoubleSpeedIndicator(
                            speed = (currentPlayerUpdate as PlayerUpdates.DoubleSpeed).speed,
                            isDragging = (currentPlayerUpdate as PlayerUpdates.DoubleSpeed).isDragging,
                        )
                        is PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
                        is PlayerUpdates.ShowText -> TextPlayerUpdate(
                            (currentPlayerUpdate as PlayerUpdates.ShowText).value,
                        )
                        is PlayerUpdates.ShowTextResource -> TextPlayerUpdate(
                            stringResource((currentPlayerUpdate as PlayerUpdates.ShowTextResource).textResource),
                        )
                        PlayerUpdates.None -> {}
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = areControlsLocked && controlsShown,
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { it / 2 },
                    modifier = Modifier.constrainAs(lockedControls) {
                        centerVerticallyTo(parent)
                        end.linkTo(parent.end, 16.dp)
                    },
                ) {
                    IconButton(
                        onClick = { viewModel.unlockControls() },
                        modifier = Modifier
                            .background(Color.Black.copy(.5f), MaterialTheme.shapes.medium),
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    }
                }
            }
        }
    }
}

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> = tween(
    durationMillis = 300,
    easing = FastOutSlowInEasing,
)

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> = tween(
    durationMillis = 100,
    easing = LinearOutSlowInEasing,
)
