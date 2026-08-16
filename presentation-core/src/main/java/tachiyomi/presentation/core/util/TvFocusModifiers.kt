package tachiyomi.presentation.core.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds high-contrast animated border highlight and optional scale effect
 * when the composable receives focus via D-Pad or keyboard.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color? = null,
    focusedScale: Float = 1.03f,
    focusedBackgroundAlpha: Float = 0.08f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "tvFocusScale",
    )

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChange?.invoke(state.isFocused)
        }
        .then(
            if (focusedScale > 1f) {
                Modifier.scale(scale)
            } else {
                Modifier
            },
        )
        .then(
            if (isFocused) {
                Modifier
                    .drawBehind {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        if (focusedBackgroundAlpha > 0f) {
                            drawOutline(
                                outline = outline,
                                color = borderColor.copy(alpha = focusedBackgroundAlpha),
                            )
                        }
                        drawOutline(
                            outline = outline,
                            color = borderColor,
                            style = Stroke(width = borderWidth.toPx()),
                        )
                    }
            } else {
                Modifier
            },
        )
}

/**
 * Subtle focus highlight modifier specifically designed for list rows,
 * settings items, and episode rows.
 */
fun Modifier.tvListItemFocusHighlight(
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp,
    focusedBorderColor: Color? = null,
    focusedBackgroundAlpha: Float = 0.15f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = tvFocusHighlight(
    shape = shape,
    borderWidth = borderWidth,
    focusedBorderColor = focusedBorderColor,
    focusedScale = 1.0f,
    focusedBackgroundAlpha = focusedBackgroundAlpha,
    onFocusChange = onFocusChange,
)

/**
 * Circular focus highlight modifier for circular icon buttons and player controls.
 */
fun Modifier.tvCircleFocusHighlight(
    borderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color? = null,
    focusedScale: Float = 1.08f,
    focusedBackgroundAlpha: Float = 0.2f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = tvFocusHighlight(
    shape = RoundedCornerShape(50),
    borderWidth = borderWidth,
    focusedBorderColor = focusedBorderColor,
    focusedScale = focusedScale,
    focusedBackgroundAlpha = focusedBackgroundAlpha,
    onFocusChange = onFocusChange,
)
