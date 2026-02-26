package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings

import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.ui.UiPreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

fun feedTab(): Tab = FeedTab

data object FeedTab : Tab {

    override fun isEnabled(): Boolean {
        return Injekt.get<UiPreferences>().enableFeed().get()
    }

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val title = SYMR.strings.feed
            return TabOptions(
                index = 1u,
                title = stringResource(title),
                icon = painterResource(R.drawable.ic_browse_filled_24dp),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                eu.kanade.presentation.components.AppBar(
                    title = stringResource(SYMR.strings.feed),
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { navigator.push(FeedManageScreen()) }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Settings,
                                contentDescription = "Edit Feed",
                            )
                        }
                    }
                )
            }
        ) { contentPadding ->
            Content(contentPadding)
        }
    }

    @Composable
    fun Content(contentPadding: PaddingValues) {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { FeedScreenModel() }
        
        FeedScreen(
            screenModel = screenModel,
            onAnimeClick = { anime, feedId -> 
                screenModel.onAnimeClicked(anime, feedId)
                navigator.push(AnimeScreen(anime.id)) 
            },
            onAddSourceClick = { 
                scope.launch {
                    tabNavigator.current = BrowseTab
                    // BrowseTab is already at index 0 (sourcesTab)
                }
            },
            contentPadding = contentPadding,
        )
    }
}
