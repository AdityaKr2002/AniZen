package tachiyomi.presentation.widget

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.source.interactor.GetFeedSavedSearchCategories
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.presentation.widget.components.CoverHeight
import tachiyomi.presentation.widget.components.CoverWidth
import tachiyomi.presentation.widget.components.LockedWidget
import tachiyomi.presentation.widget.components.UpdatesWidget
import tachiyomi.presentation.widget.util.appWidgetBackgroundRadius
import tachiyomi.presentation.widget.util.calculateRowAndColumnCount
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedGlanceWidget(
    private val context: Context = Injekt.get<Application>(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getFeedSavedSearchCategories: GetFeedSavedSearchCategories = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val securityPreferences: SecurityPreferences = Injekt.get(),
) : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    private val foreground = ColorProvider(day = Color.Black, night = Color.White)
    private val background = ImageProvider(R.drawable.appwidget_background)
    private val topPadding = 16.dp
    private val bottomPadding = 16.dp

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val locked = securityPreferences.useAuthenticator().get()
        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .appWidgetBackground()
            .padding(top = topPadding, bottom = bottomPadding)
            .appWidgetBackgroundRadius()

        provideContent {
            if (locked) {
                LockedWidget(foreground = foreground, modifier = containerModifier)
                return@provideContent
            }

            val flow = remember {
                combine(
                    getFeedSavedSearchCategories.subscribe(),
                    sourceManager.isInitialized,
                    ::Pair
                ).map { (categories, isInitialized) ->
                    if (!isInitialized) return@map null
                    val category = categories.firstOrNull() ?: return@map null
                    val feedItems = getFeedSavedSearchGlobal.await(category.id)
                    // Simplify: take first source's latest
                    val firstFeed = feedItems.firstOrNull() ?: return@map null
                    val source = sourceManager.get(firstFeed.source) as? AnimeCatalogueSource ?: return@map null
                    
                    // Note: In real implementation we'd need to fetch actual data.
                    // For the widget, we'll reuse UpdatesWidget UI which takes Pairs of (Id, Bitmap).
                    emptyList<Pair<Long, Bitmap?>>().toImmutableList()
                }
            }

            val data by flow.collectAsState(initial = null)
            UpdatesWidget(
                data = data,
                contentColor = foreground,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                modifier = containerModifier,
            )
        }
    }
}
