package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material.icons.outlined.HideImage
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
import tachiyomi.presentation.core.i18n.stringResource

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
                Icons.Outlined.HideImage,
                MaterialTheme.colorScheme.error,
                DISABLED_ALPHA,
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = stringResource(mode.getLabelRes()),
            tint = tint,
            modifier = Modifier.alpha(alpha),
        )
    }
}
