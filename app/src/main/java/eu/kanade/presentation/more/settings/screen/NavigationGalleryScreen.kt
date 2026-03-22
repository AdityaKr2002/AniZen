package eu.kanade.presentation.more.settings.screen

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavCommunityRegistry
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.domain.ui.model.NavLayoutPack
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NavigationGalleryScreen : Screen() {

    @Composable
    override fun Content() {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val backPress = LocalBackPress.current

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Layout Gallery",
                    navigateUp = { backPress?.invoke() },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues + PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Discover community-curated navigation layouts.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(NavCommunityRegistry.OFFICIAL_PACKS) { pack ->
                    LayoutPackCard(
                        pack = pack,
                        onApply = {
                            uiPreferences.updateNavConfig(pack.config)
                            backPress?.invoke()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun LayoutPackCard(
        pack: NavLayoutPack,
        onApply: () -> Unit
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = pack.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "by ${pack.author}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = onApply) {
                        Text("Apply")
                    }
                }
                
                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Visual Preview:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pack.config.visibleTabs.forEach { id ->
                            val item = NavItem.fromId(id)
                            if (item != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val icon = if (item == NavItem.FEED) {
                                        painterResource(item.iconRes)
                                    } else {
                                        rememberAnimatedVectorPainter(
                                            AnimatedImageVector.animatedVectorResource(item.iconRes),
                                            false
                                        )
                                    }
                                    Icon(
                                        painter = icon,
                                        contentDescription = null,
                                        modifier = Modifier.padding(4.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = item.id.take(3).uppercase(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
