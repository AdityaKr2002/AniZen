package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.model.NodeStatus
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceHealthCache
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun BaseSourceItem(
    source: Source,
    modifier: Modifier = Modifier,
    isNsfw: Boolean = source.isNsfw,
    status: NodeStatus = NodeStatus.OPERATIONAL,
    showLanguageInContent: Boolean = true,
    onClickItem: () -> Unit = {},
    onLongClickItem: () -> Unit = {},
    icon: @Composable RowScope.(Source) -> Unit = defaultIcon,
    action: @Composable RowScope.(Source) -> Unit = {},
    content: @Composable RowScope.(Source, String?, Boolean, NodeStatus) -> Unit = defaultContent,
) {
    val sourceLangString = LocaleHelper.getSourceDisplayName(source.lang, LocalContext.current).takeIf {
        showLanguageInContent
    }
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { icon.invoke(this, source) },
        action = { action.invoke(this, source) },
        content = { content.invoke(this, source, sourceLangString, isNsfw, status) },
    )
}

private val defaultIcon: @Composable RowScope.(Source) -> Unit = { source ->
    SourceIcon(source = source)
}

private val defaultContent: @Composable RowScope.(Source, String?, Boolean, NodeStatus) -> Unit = { source, sourceLangString, isNsfw, sourceStatus ->
    val extensionManager: ExtensionManager = Injekt.get()
    val extensionName = remember(source.id) { extensionManager.getExtensionNameForSource(source.id) }

    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .weight(1f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = source.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Technical Badges
            val name = source.name.lowercase()
            val isBdix = name.contains("dflix") || 
                         name.contains("dhaka") || 
                         name.contains("bdix") || 
                         name.contains("ftp") ||
                         name.contains("cineplex") ||
                         name.contains("sam") ||
                         name.contains("bijoy") ||
                         name.contains("bas play") ||
                         name.contains("fanush") ||
                         name.contains("icc") ||
                         name.contains("nagordola") ||
                         name.contains("roarzone") ||
                         name.contains("infomedia")

            if (isBdix) {
                StatusBadge("BDIX", Color(0xFF1E88E5))
            }
            
            // Smarter API detection for badges
            val sourceClass = source::class.java.simpleName
            if (name.contains("api") || name.contains("json") || sourceClass.contains("Api") || sourceClass.contains("Json")) {
                StatusBadge("API", Color(0xFF43A047))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val secondaryText = buildString {
                if (sourceLangString != null) {
                    append(sourceLangString)
                }
                if (extensionName != null && extensionName != source.name) {
                    if (isNotEmpty()) append(" • ")
                    append(extensionName)
                }
            }

            if (secondaryText.isNotEmpty()) {
                Text(
                    modifier = Modifier.secondaryItemAlpha(),
                    text = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Health Pulse linked to cache
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when (sourceStatus) {
                            NodeStatus.OPERATIONAL -> Color(0xFF4CAF50)
                            NodeStatus.DEGRADED -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
            )

            if (isNsfw) {
                StatusBadge("18+", MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
    }
}
