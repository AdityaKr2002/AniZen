package tachiyomi.presentation.core.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean Material 3 / Komikku-style focus highlight.
 * Applies a smooth container surface tint when focused via D-Pad or keyboard.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 0.dp,
    focusedBorderColor: Color? = null,
    focusedScale: Float = 1.0f,
    focusedBackgroundAlpha: Float = 0.12f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val tintColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChange?.invoke(state.isFocused)
        }
        .then(
            if (isFocused) {
                Modifier.drawBehind {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(
                        outline = outline,
                        color = tintColor.copy(alpha = focusedBackgroundAlpha),
                    )
                }
            } else {
                Modifier
            },
        )
}

/**
 * Subtle focus highlight modifier for list rows, settings items, and episode rows.
 */
fun Modifier.tvListItemFocusHighlight(
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 0.dp,
    focusedBorderColor: Color? = null,
    focusedBackgroundAlpha: Float = 0.12f,
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
    borderWidth: Dp = 0.dp,
    focusedBorderColor: Color? = null,
    focusedScale: Float = 1.0f,
    focusedBackgroundAlpha: Float = 0.16f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = tvFocusHighlight(
    shape = RoundedCornerShape(50),
    borderWidth = borderWidth,
    focusedBorderColor = focusedBorderColor,
    focusedScale = focusedScale,
    focusedBackgroundAlpha = focusedBackgroundAlpha,
    onFocusChange = onFocusChange,
)
