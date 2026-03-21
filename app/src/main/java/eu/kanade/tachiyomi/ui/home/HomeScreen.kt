package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.home.NavActionExecutor
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
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
    private val openTabEvent = Channel<Tab>()
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

        val adaptiveEngine = remember { NavAdaptiveEngine(context, scope) }
        val adaptiveDecision by adaptiveEngine.currentDecision.collectAsState()

        LaunchedEffect(Unit) {
            while(true) {
                adaptiveEngine.evaluateRules()
                kotlinx.coroutines.delay(60000)
            }
        }

        val activity = context as? ComponentActivity
        val preferences = Injekt.get<PreferenceStore>()

        var bottomNavVisible by rememberSaveable { mutableStateOf(true) }
        val bottomNavTranslationY by animateFloatAsState(
            targetValue = if (bottomNavVisible) 0f else 1f,
            animationSpec = androidx.compose.animation.core.tween(if (animatedTransitions) 200 else 0),
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
            val visibleTabs = remember(bottomNavTabs, uiPreferences.enableFeed().collectAsState().value, uiPreferences.showFeedInNavigationBar().collectAsState().value) {
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
                                    NavigationRailItem(it, navLabelVisibility)
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
                                                NavigationBarItem(it, navLabelVisibility)
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
                            .consumeWindowInsets(contentPadding),
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
                            is Tab.AnimeLib -> LibraryTab
                            is Tab.Feed -> FeedTab
                            is Tab.Updates -> UpdatesTab
                            is Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.AnimeLib && it.animeIdToOpen != null) {
                            navigator.push(AnimeScreen(it.animeIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
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
        navLabelVisibility: eu.kanade.domain.ui.model.NavLabelVisibility,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val selected = tabNavigator.current.key == tab.key
        val haptic = LocalHapticFeedback.current
        val executor = remember { NavActionExecutor(context, scope, navigator) }
        
        // TODO: Map from preferences in next pass
        val behavior = remember(tab) { NavBehavior() }

        NavigationBarItem(
            selected = selected,
            onClick = {
                // Handled via pointerInput for conflict resolution
            },
            modifier = Modifier.pointerInput(tab) {
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
                    onDoubleTap = if (behavior.onDoubleTap != NavAction.Default) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.DoubleTap)
                            executor.execute(behavior.onDoubleTap)
                        }
                    } else null
                )
            },
            icon = { NavigationIconItem(tab) },
            label = if (navLabelVisibility != eu.kanade.domain.ui.model.NavLabelVisibility.NEVER) {
                {
                    Text(
                        text = tab.options.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else null,
            alwaysShowLabel = navLabelVisibility == eu.kanade.domain.ui.model.NavLabelVisibility.ALWAYS,
        )
    }

    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        navLabelVisibility: eu.kanade.domain.ui.model.NavLabelVisibility,
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

        NavigationRailItem(
            selected = selected,
            onClick = {
                // Handled via pointerInput
            },
            modifier = Modifier.pointerInput(tab) {
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
                    onDoubleTap = if (behavior.onDoubleTap != NavAction.Default) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.DoubleTap)
                            executor.execute(behavior.onDoubleTap)
                        }
                    } else null
                )
            },
            icon = { NavigationIconItem(tab) },
            label = if (navLabelVisibility != eu.kanade.domain.ui.model.NavLabelVisibility.NEVER) {
                {
                    Text(
                        text = tab.options.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else null,
            alwaysShowLabel = navLabelVisibility == eu.kanade.domain.ui.model.NavLabelVisibility.ALWAYS,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val animatedTransitions by uiPreferences.animatedTransitions().collectAsState()
        val selected = tabNavigator.current.key == tab.key
        val scale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (selected && animatedTransitions) 1.2f else 1f,
            animationSpec = androidx.compose.animation.core.tween(
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
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                // TODO: https://issuetracker.google.com/u/0/issues/316327367
                tint = LocalContentColor.current,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class AnimeLib(val animeIdToOpen: Long? = null) : Tab
        data object Feed : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
