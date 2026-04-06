package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

@Composable
fun ListGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    onPlayClick: (() -> Unit)? = null,
) {
    // KMK -->
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        // KMK <--
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onPlayClick != null) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
