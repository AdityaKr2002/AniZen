package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.domain.ui.model.PanoramaMode
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun themeMode() = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    fun appTheme() = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    fun colorTheme() = preferenceStore.getInt("pref_color_theme", 0)

    fun themeDarkAmoled() = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun startScreen() = preferenceStore.getEnum("start_screen", StartScreen.LIBRARY)

    fun navStyle() = preferenceStore.getEnum("bottom_rail_nav_style", NavStyle.SHOW_ALL)

    fun enableFeed() = preferenceStore.getBoolean("enable_feed", false)

    fun showFeedInNavigationBar() = preferenceStore.getBoolean("show_feed_in_navigation_bar", false)

    fun showFeedInBrowse() = preferenceStore.getBoolean("show_feed_in_browse", false)

    // SY -->
    fun bottomBarLabels() = preferenceStore.getBoolean("pref_show_bottom_bar_labels", true)

    fun dynamicAnimeTheme() = preferenceStore.getBoolean("pref_dynamic_manga_theme", true)

    fun dynamicPlayerTheme() = preferenceStore.getBoolean("pref_dynamic_player_theme", true)

    fun autoExpandAnimeDescription() = preferenceStore.getBoolean("pref_auto_expand_anime_description", false)

    fun showSeasonsSection() = preferenceStore.getBoolean("pref_show_seasons_section", true)

    fun animeItemSpacing() = preferenceStore.getInt("pref_anime_item_spacing", 24)

    fun panoramaCover() = preferenceStore.getBoolean("pref_panorama_cover", false)

    fun libraryPanoramaMode() = getPanoramaMode("pref_library_panorama_mode", "pref_library_panorama")

    fun browsePanoramaMode() = getPanoramaMode("pref_browse_panorama_mode", "pref_browse_panorama")

    fun feedPanoramaMode() = getPanoramaMode("pref_feed_panorama_mode", "pref_feed_panorama")

    fun updatesPanoramaMode() = getPanoramaMode("pref_updates_panorama_mode", "pref_updates_panorama")

    fun historyPanoramaMode() = getPanoramaMode("pref_history_panorama_mode", "pref_history_panorama")

    private fun getPanoramaMode(key: String, oldKey: String): tachiyomi.core.common.preference.Preference<PanoramaMode> {
        val pref = preferenceStore.getEnum(key, PanoramaMode.FOLLOW_GLOBAL)
        if (!pref.isSet()) {
            val oldPref = preferenceStore.getBoolean(oldKey)
            if (oldPref.isSet()) {
                val newValue = if (oldPref.get()) PanoramaMode.FORCE_ON else PanoramaMode.FORCE_OFF
                pref.set(newValue)
                oldPref.delete()
            }
        }
        return pref
    }

    // Deprecated boolean toggles - use enum modes instead
    @Deprecated("Use feedPanoramaMode")
    fun feedPanorama() = preferenceStore.getBoolean("pref_feed_panorama", true)

    @Deprecated("Use updatesPanoramaMode")
    fun updatesPanorama() = preferenceStore.getBoolean("pref_updates_panorama", false)

    @Deprecated("Use historyPanoramaMode")
    fun historyPanorama() = preferenceStore.getBoolean("pref_history_panorama", false)

    fun containerStyles() = preferenceStore.getStringSet("pref_ui_container_styles", emptySet())

    fun animatedTransitions() = preferenceStore.getBoolean("animated_transitions", true)

    fun preloadLibraryColor() = preferenceStore.getBoolean("preload_library_color", true)
    // SY <--

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}

object ContainerStyle {
    const val LIBRARY = "library"
    const val UPDATES = "updates"
    const val HISTORY = "history"
    const val DETAILS = "details"
    const val SETTINGS = "settings"
    const val BROWSE = "browse"
}
