package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.PanoramaMode
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA

@Composable
fun PanoramaModeToggle(
    mode: PanoramaMode,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onCycle,
        modifier = modifier,
    ) {
        val (icon, tint, alpha) = when (mode) {
            PanoramaMode.FOLLOW_GLOBAL -> Triple(
                Icons.Outlined.Panorama,
                LocalContentColor.current,
                DISABLED_ALPHA,
            )
            PanoramaMode.FORCE_ON -> Triple(
                Icons.Outlined.Panorama,
                MaterialTheme.colorScheme.primary,
                1f,
            )
            PanoramaMode.FORCE_OFF -> Triple(
                Icons.Outlined.Panorama, // Using same icon but with different visual cue
                MaterialTheme.colorScheme.error.copy(alpha = DISABLED_ALPHA),
                1f,
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = "Panorama Mode: ${mode.name}",
            tint = tint,
            modifier = Modifier.alpha(alpha),
        )
    }
}
