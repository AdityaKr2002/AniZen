package eu.kanade.presentation.more.settings.screen

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavConfig
import eu.kanade.domain.ui.model.NavConfigSerializer
import eu.kanade.domain.ui.model.NavConfigValidator
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.domain.ui.model.NavLabelVisibility
import eu.kanade.domain.ui.model.NavPresets
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import sh.calvin.reorderable.*
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NavigationSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val backPress = LocalBackPress.current

        val bottomNavTabs by uiPreferences.bottomNavTabs().collectAsState()
        val bottomNavHiddenTabs by uiPreferences.bottomNavHiddenTabs().collectAsState()
        val navLabelVisibility by uiPreferences.navLabelVisibility().collectAsState()
        val hideOnScroll by uiPreferences.hideBottomBarOnScroll().collectAsState()

        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current
        
        var showImportDialog by remember { mutableStateOf(false) }
        var importInput by remember { mutableStateOf("") }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Layout") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste a layout string to apply a shared navigation configuration.")
                        TextField(
                            value = importInput,
                            onValueChange = { importInput = it },
                            placeholder = { Text("v1|library,updates...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val preview = NavConfigSerializer.deserialize(importInput)
                        if (preview != null) {
                            Text("Preview:", fontWeight = FontWeight.Bold)
                            Text(preview.visibleTabs.joinToString(" → "))
                        } else if (importInput.isNotBlank()) {
                            Text("Invalid format or ID", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val config = NavConfigSerializer.deserialize(importInput)
                            if (config != null) {
                                uiPreferences.updateNavConfig(config)
                                showImportDialog = false
                                importInput = ""
                                context.toast("Layout applied successfully")
                            }
                        },
                        enabled = NavConfigSerializer.deserialize(importInput) != null
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                }
            )
        }
Scaffold(
    topBar = { scrollBehavior ->
        AppBar(
            title = stringResource(MR.strings.pref_bottom_nav_settings),
            navigateUp = backPress::invoke,
            actions = {
                AppBarActions(
                    persistentListOf(
                        AppBar.Action(
                            title = "Browse Gallery",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = { navigator.push(NavigationGalleryScreen()) },
                        ),
                        AppBar.OverflowAction(
                            title = "Copy Layout Link (Deep Link)",
                            onClick = {
                                val config = NavConfig(visibleTabs = bottomNavTabs.toImmutableList(), hiddenTabs = bottomNavHiddenTabs.toImmutableList())
                                val serialized = NavConfigSerializer.serialize(config)
                                val deepLink = "anizen://nav/import?data=$serialized"
                                context.copyToClipboard("AniZen Layout", deepLink)
                                context.toast("Deep link copied to clipboard")
                            }
                        ),
                        AppBar.OverflowAction(
                            title = "Default Preset",
...

                                    onClick = { 
                                        Log.d("AniZenNav", "Preset applied: Default")
                                        uiPreferences.updateNavConfig(NavPresets.DEFAULT) 
                                    },
                                ),
                                AppBar.OverflowAction(
                                    title = "Minimal Preset",
                                    onClick = { 
                                        Log.d("AniZenNav", "Preset applied: Minimal")
                                        uiPreferences.updateNavConfig(NavPresets.MINIMAL) 
                                    },
                                ),
                                AppBar.OverflowAction(
                                    title = "Power Preset",
                                    onClick = { 
                                        Log.d("AniZenNav", "Preset applied: Power")
                                        uiPreferences.updateNavConfig(NavPresets.POWER) 
                                    },
                                ),
                                AppBar.OverflowAction(
                                    title = "Copy Layout String",
                                    onClick = {
                                        val config = NavConfig(visibleTabs = bottomNavTabs.toImmutableList(), hiddenTabs = bottomNavHiddenTabs.toImmutableList())
                                        val serialized = NavConfigSerializer.serialize(config)
                                        context.copyToClipboard("AniZen Navigation", serialized)
                                        context.toast("Layout copied to clipboard")
                                    }
                                ),
                                AppBar.OverflowAction(
                                    title = "Import Layout String",
                                    onClick = { showImportDialog = true }
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.pref_bottom_nav_reset_layout),
                                    icon = Icons.Outlined.RestartAlt,
                                    onClick = {
                                        Log.d("AniZenNav", "Layout Reset triggered")
                                        uiPreferences.bottomNavTabs().delete()
                                        uiPreferences.bottomNavHiddenTabs().delete()
                                    },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            val lazyListState = rememberLazyListState()
            
            val visibleItems = remember(bottomNavTabs) {
                bottomNavTabs.mapNotNull { NavItem.fromId(it) }
            }
            val hiddenItems = remember(bottomNavHiddenTabs) {
                bottomNavHiddenTabs.mapNotNull { NavItem.fromId(it) }
            }

            val onMoveToHidden = { item: NavItem ->
                Log.d("AniZenNav", "Hiding tab: ${item.id}")
                val newVisible = visibleItems.filter { it != item }.map { it.id }.toImmutableList()
                val newHidden = (hiddenItems.map { it.id } + item.id).distinct().toImmutableList()
                uiPreferences.updateNavConfig(NavConfig(visibleTabs = newVisible, hiddenTabs = newHidden))
            }

            val onMoveToVisible = { item: NavItem ->
                if (visibleItems.size < NavConfigValidator.MAX_BOTTOM_TABS) {
                    Log.d("AniZenNav", "Showing tab: ${item.id}")
                    val newHidden = hiddenItems.filter { it != item }.map { it.id }.toImmutableList()
                    val newVisible = (visibleItems.map { it.id } + item.id).distinct().toImmutableList()
                    uiPreferences.updateNavConfig(NavConfig(visibleTabs = newVisible, hiddenTabs = newHidden))
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.toast(MR.strings.pref_bottom_nav_max_tabs_reached)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = paddingValues + PaddingValues(vertical = MaterialTheme.padding.medium),
            ) {
                item {
                    PreferenceGroupHeader(title = stringResource(MR.strings.pref_behavior))
                    ListPreferenceWidget(
                        value = navLabelVisibility,
                        title = stringResource(MR.strings.pref_bottom_nav_style),
                        subtitle = stringResource(navLabelVisibility.titleRes),
                        entries = NavLabelVisibility.entries
                            .associateWith { stringResource(it.titleRes) }
                            .toImmutableMap(),
                        onValueChange = { uiPreferences.navLabelVisibility().set(it) },
                    )
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.pref_bottom_nav_hide_on_scroll),
                        checked = hideOnScroll,
                        onCheckedChanged = { uiPreferences.hideBottomBarOnScroll().set(it) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferenceGroupHeader(title = "Adaptive Navigation (Beta)")
                    SwitchPreferenceWidget(
                        title = "Enable Adaptive Engine",
                        subtitle = "Allow the app to suggest layout changes based on context.",
                        checked = uiPreferences.adaptiveNavEnabled().collectAsState().value,
                        onCheckedChanged = { uiPreferences.adaptiveNavEnabled().set(it) }
                    )
                    if (uiPreferences.adaptiveNavEnabled().collectAsState().value) {
                        SwitchPreferenceWidget(
                            title = "Connectivity Rules",
                            subtitle = "Suggest offline layouts when WiFi is lost.",
                            checked = uiPreferences.adaptiveConnectivityRule().collectAsState().value,
                            onCheckedChanged = { uiPreferences.adaptiveConnectivityRule().set(it) }
                        )
                        SwitchPreferenceWidget(
                            title = "Late Night Rules",
                            subtitle = "Simplify navigation during late hours.",
                            checked = uiPreferences.adaptiveTimeRule().collectAsState().value,
                            onCheckedChanged = { uiPreferences.adaptiveTimeRule().set(it) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferenceGroupHeader(title = "Privacy & Telemetry")
                    SwitchPreferenceWidget(
                        title = "On-Device Telemetry",
                        subtitle = "Logs gesture interactions locally for engine optimization. Data never leaves your device.",
                        checked = uiPreferences.adaptiveTelemetryEnabled().collectAsState().value,
                        onCheckedChanged = { uiPreferences.adaptiveTelemetryEnabled().set(it) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferenceGroupHeader(title = stringResource(MR.strings.pref_bottom_nav_visible_tabs))
                    if (visibleItems.isEmpty()) {
                        Text(
                            text = "No visible tabs",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(
                    items = visibleItems,
                    key = { "visible-${it.id}" }
                ) { item ->
                    val isRequired = NavConfigValidator.REQUIRED_TABS.contains(item.id)
                    key(item.id) {
                        NavigationSettingsItem(
                            item = item,
                            isVisible = true,
                            onToggle = { if (!isRequired) onMoveToHidden(item) },
                            onReorder = { from, to ->
                                val newList = visibleItems.toMutableList().apply {
                                    add(to, removeAt(from))
                                }
                                uiPreferences.updateNavConfig(NavConfig(visibleTabs = newList.map { it.id }.toImmutableList(), hiddenTabs = hiddenItems.map { it.id }.toImmutableList()))
                            },
                            canMoveUp = visibleItems.indexOf(item) > 0,
                            canMoveDown = visibleItems.indexOf(item) < visibleItems.size - 1,
                            onMoveUp = {
                                val index = visibleItems.indexOf(item)
                                val newList = visibleItems.toMutableList().apply {
                                    add(index - 1, removeAt(index))
                                }
                                uiPreferences.updateNavConfig(NavConfig(visibleTabs = newList.map { it.id }.toImmutableList(), hiddenTabs = hiddenItems.map { it.id }.toImmutableList()))
                            },
                            onMoveDown = {
                                val index = visibleItems.indexOf(item)
                                val newList = visibleItems.toMutableList().apply {
                                    add(index + 1, removeAt(index))
                                }
                                uiPreferences.updateNavConfig(NavConfig(visibleTabs = newList.map { it.id }.toImmutableList(), hiddenTabs = hiddenItems.map { it.id }.toImmutableList()))
                            },
                            toggleEnabled = !isRequired
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PreferenceGroupHeader(title = stringResource(MR.strings.pref_bottom_nav_hidden_tabs))
                    if (hiddenItems.isEmpty()) {
                        Text(
                            text = "No hidden tabs",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(
                    items = hiddenItems,
                    key = { "hidden-${it.id}" }
                ) { item ->
                    key(item.id) {
                        NavigationSettingsItem(
                            item = item,
                            isVisible = false,
                            onToggle = { onMoveToVisible(item) },
                            onReorder = null,
                            canMoveUp = false,
                            canMoveDown = false,
                            onMoveUp = {},
                            onMoveDown = {}
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    PreferenceGroupHeader(title = "Telemetry Debug (Dev Only)")
                    NavActionExecutor.getHistory().forEach { trace ->
                        Text(
                            text = "[${trace.timestamp % 100000}] ${trace.actionName} -> ${trace.result}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NavigationSettingsItem(
        item: NavItem,
        isVisible: Boolean,
        onToggle: () -> Unit,
        onReorder: ((Int, Int) -> Unit)?,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        toggleEnabled: Boolean = true,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = item.tab.options.icon!!,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(item.titleRes),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (isVisible) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.DragHandle, // Just a placeholder for reorder intent
                            contentDescription = "Move Up",
                            tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(onClick = onToggle, enabled = toggleEnabled) {
                    Icon(
                        imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isVisible) "Hide" else "Show",
                        tint = if (toggleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
