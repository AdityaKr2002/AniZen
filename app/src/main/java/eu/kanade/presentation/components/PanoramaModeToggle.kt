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
import eu.kanade.domain.ui.model.PanoramaMode
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA

@Composable
fun PanoramaModeToggle(
    panoramaMode: PanoramaMode,
    globalPanorama: Boolean,
    onPanoramaModeChange: (PanoramaMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEffectivePanorama = panoramaMode.resolve(globalPanorama)

    IconButton(
        onClick = {
            val nextMode = if (isEffectivePanorama) {
                if (!globalPanorama) PanoramaMode.FOLLOW_GLOBAL else PanoramaMode.FORCE_OFF
            } else {
                if (globalPanorama) PanoramaMode.FOLLOW_GLOBAL else PanoramaMode.FORCE_ON
            }
            onPanoramaModeChange(nextMode)
        },
        modifier = modifier,
    ) {
        val (icon, tint, alpha) = if (isEffectivePanorama) {
            Triple(
                Icons.Outlined.Panorama,
                MaterialTheme.colorScheme.primary,
                1f,
            )
        } else {
            Triple(
                Icons.Outlined.Panorama,
                LocalContentColor.current,
                DISABLED_ALPHA,
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = if (isEffectivePanorama) "Panorama Enabled" else "Panorama Disabled",
            tint = tint,
            modifier = Modifier.alpha(alpha),
        )
    }
}
