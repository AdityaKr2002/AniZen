package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavAction
import eu.kanade.domain.ui.model.NavBehavior
import eu.kanade.domain.ui.model.NavLabelVisibility
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<HomeTab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    private val uiPreferences: UiPreferences by injectLazy()
    private val defaultTab = uiPreferences.startScreen().get().tab.let { 
        if (it.isEnabled()) it else LibraryTab
    }

    @Composable
    override fun Content() {
        val navLabelVisibility by uiPreferences.navLabelVisibility().collectAsState()
        val hideOnScroll by uiPreferences.hideBottomBarOnScroll().collectAsState()
        val bottomNavTabs by uiPreferences.bottomNavTabs().collectAsState()
        val animatedTransitions by uiPreferences.animatedTransitions().collectAsState()
        val tabFadeDuration = remember(animatedTransitions) { if (animatedTransitions) 200 else 0 }

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val screenModel = rememberScreenModel { HomeScreenModel(context) }
        val adaptiveEngine = screenModel.adaptiveEngine
        val adaptiveDecision by adaptiveEngine.currentDecision.collectAsState()

        val activity = context as? ComponentActivity
        val preferences = Injekt.get<PreferenceStore>()

        var bottomNavVisible by rememberSaveable { mutableStateOf(true) }
        val bottomNavTranslationY by animateFloatAsState(
            targetValue = if (bottomNavVisible) 0f else 1f,
            animationSpec = tween(if (animatedTransitions) 200 else 0),
            label = "bottomNavTranslation"
        )

        val nestedScrollConnection = remember(hideOnScroll) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    if (hideOnScroll && available.y < -10f && bottomNavVisible) {
                        bottomNavVisible = false
                    } else if (hideOnScroll && available.y > 10f && !bottomNavVisible) {
                        bottomNavVisible = true
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }

        TabNavigator(
            tab = defaultTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            val visibleTabs: List<eu.kanade.presentation.util.Tab> = remember(bottomNavTabs, uiPreferences.enableFeed().collectAsState().value, uiPreferences.showFeedInNavigationBar().collectAsState().value) {
                bottomNavTabs.mapNotNull { id -> NavItem.fromId(id)?.tab }.fastFilter { it.isEnabled() }
            }
            val isCurrentTabVisible = visibleTabs.any { it.key == tabNavigator.current.key }

            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    modifier = Modifier.nestedScroll(nestedScrollConnection),
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                visibleTabs.fastForEach {
                                    NavigationRailItem(it, navLabelVisibility, adaptiveDecision)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            Column(
                                modifier = Modifier.graphicsLayer {
                                    translationY = bottomNavTranslationY * size.height
                                    alpha = 1f - (bottomNavTranslationY * 0.5f)
                                }
                            ) {
                                LaunchedEffect(Unit) {
                                    showBottomNavEvent.receiveAsFlow().collectLatest { bottomNavVisible = it }
                                }

                                if (isCurrentTabVisible) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ) {
                                        visibleTabs.fastForEach {
                                            key(it.key) {
                                                NavigationBarItem(it, navLabelVisibility, adaptiveDecision)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding)
                            .fillMaxSize(),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    durationMillis = tabFadeDuration,
                                ) togetherWith
                                    materialFadeThroughOut(
                                        durationMillis = tabFadeDuration,
                                    )
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }

                        // Explainability Layer (Smart Suggestions)
                        adaptiveDecision?.let { decision ->
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Smart Suggestion", style = MaterialTheme.typography.labelSmall)
                                        Text(text = decision.reason, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = { adaptiveEngine.dismissDecision() }) {
                                        Text("Dismiss")
                                    }
                                    Button(onClick = { adaptiveEngine.applyDecision(decision) }) {
                                        Text("Apply")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val goToStartScreen = {
                tabNavigator.current = defaultTab
            }
            BackHandler(
                enabled = tabNavigator.current != defaultTab,
                onBack = goToStartScreen,
            )
            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToStartScreen()
                        when (defaultTab) {
                            LibraryTab -> LibraryTab.search(it)
                            else -> {}
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is HomeTab.AnimeLib -> LibraryTab
                            is HomeTab.Feed -> FeedTab
                            is HomeTab.Updates -> UpdatesTab
                            is HomeTab.History -> HistoryTab
                            is HomeTab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is HomeTab.More -> MoreTab
                        }

                        if (it is HomeTab.AnimeLib && it.animeIdToOpen != null) {
                            navigator.push(AnimeScreen(it.animeIdToOpen))
                        }
                        if (it is HomeTab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.NavigationBarItem(
        tab: eu.kanade.presentation.util.Tab,
        navLabelVisibility: NavLabelVisibility,
        adaptiveDecision: AdaptiveDecision?,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val behaviorMap by uiPreferences.bottomNavBehaviors().collectAsState()
        val navItem = remember(tab) { NavItem.entries.find { it.tab == tab } }
        val behavior = behaviorMap[navItem?.id] ?: NavBehavior()

        val selected = tabNavigator.current.key == tab.key
        val haptic = LocalHapticFeedback.current
        val executor = remember { NavActionExecutor(context, scope, navigator) }
        
        val title = remember(tab, adaptiveDecision) {
            if (navItem == NavItem.ADAPTIVE && adaptiveDecision != null) {
                adaptiveDecision.reason
            } else {
                tab.options.title
            }
        }

        NavigationBarItem(
            selected = selected,
            onClick = {
                // Handled via pointerInput for conflict resolution
            },
            modifier = Modifier.pointerInput(tab, behavior) {
                detectTapGestures(
                    onTap = {
                        if (!selected) {
                            tabNavigator.current = tab
                        } else {
                            scope.launch { tab.onReselect(navigator) }
                        }
                    },
                    onLongPress = {
                        if (behavior.onLongClick != NavAction.Default) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            executor.execute(behavior.onLongClick)
                        }
                    },
                    onDoubleTap = {
                        if (behavior.onDoubleTap != NavAction.Default) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            executor.execute(behavior.onDoubleTap)
                        }
                    }
                )
            },
            icon = { NavigationIconItem(tab, adaptiveDecision) },
            label = if (navLabelVisibility != NavLabelVisibility.NEVER) {
                {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else null,
            alwaysShowLabel = navLabelVisibility == NavLabelVisibility.ALWAYS,
        )
    }

    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        navLabelVisibility: NavLabelVisibility,
        adaptiveDecision: AdaptiveDecision?,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        
        val behaviorMap by uiPreferences.bottomNavBehaviors().collectAsState()
        val navItem = remember(tab) { NavItem.entries.find { it.tab == tab } }
        val behavior = behaviorMap[navItem?.id] ?: NavBehavior()

        val selected = tabNavigator.current.key == tab.key
        val haptic = LocalHapticFeedback.current
        val executor = remember { NavActionExecutor(context, scope, navigator) }

        val title = remember(tab, adaptiveDecision) {
            if (navItem == NavItem.ADAPTIVE && adaptiveDecision != null) {
                adaptiveDecision.reason
            } else {
                tab.options.title
            }
        }

        NavigationRailItem(
            selected = selected,
            onClick = {
                // Handled via pointerInput
            },
            modifier = Modifier.pointerInput(tab, behavior) {
                detectTapGestures(
                    onTap = {
                        if (!selected) {
                            tabNavigator.current = tab
                        } else {
                            scope.launch { tab.onReselect(navigator) }
                        }
                    },
                    onLongPress = {
                        if (behavior.onLongClick != NavAction.Default) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            executor.execute(behavior.onLongClick)
                        }
                    },
                    onDoubleTap = {
                        if (behavior.onDoubleTap != NavAction.Default) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            executor.execute(behavior.onDoubleTap)
                        }
                    }
                )
            },
            icon = { NavigationIconItem(tab, adaptiveDecision) },
            label = if (navLabelVisibility != NavLabelVisibility.NEVER) {
                {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else null,
            alwaysShowLabel = navLabelVisibility == NavLabelVisibility.ALWAYS,
        )
    }

    @Composable
    private fun NavigationIconItem(
        tab: eu.kanade.presentation.util.Tab,
        adaptiveDecision: AdaptiveDecision?,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val animatedTransitions by uiPreferences.animatedTransitions().collectAsState()
        val selected = tabNavigator.current.key == tab.key
        val scale by animateFloatAsState(
            targetValue = if (selected && animatedTransitions) 1.2f else 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "iconScale",
        )

        BadgedBox(
            modifier = Modifier.scale(scale),
            badge = {
                when {
                    UpdatesTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newUpdatesCount().changes(),
                                pref.newMangaUpdatesCount().changes(),
                            ) { countAnime, countManga -> countAnime + countManga }
                                .collectLatest { value = if (pref.newShowUpdatesCount().get()) it else 0 }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<SourcePreferences>()
                            pref.animeExtensionUpdatesCount().changes().collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            val navItem = remember(tab) { NavItem.entries.find { it.tab == tab } }
            if (navItem == NavItem.ADAPTIVE && adaptiveDecision != null) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = tab.options.title,
                    tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            } else {
                Icon(
                    painter = tab.options.icon!!,
                    contentDescription = tab.options.title,
                    tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: HomeTab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface HomeTab {
        data class AnimeLib(val animeIdToOpen: Long? = null) : HomeTab
        data object Feed : HomeTab
        data object Updates : HomeTab
        data object History : HomeTab
        data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : HomeTab
        data class More(val toDownloads: Boolean) : HomeTab
    }
}
