package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.NavConfig
import eu.kanade.domain.ui.model.NavConfigSerializer
import eu.kanade.domain.ui.model.NavConfigValidator
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.domain.ui.model.NavLabelVisibility
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.domain.ui.model.PanoramaMode
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.NavBehavior
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun themeMode() = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    fun appTheme() = preferenceStore.getEnum("pref_app_theme_key", AppTheme.DEFAULT)

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun startScreen() = preferenceStore.getEnum("start_screen", StartScreen.LIBRARY)

    fun navStyle() = preferenceStore.getEnum("bottom_rail_nav_style", NavStyle.SHOW_ALL)

    fun bottomNavTabs() = preferenceStore.getObject(
        "bottom_nav_tabs_v2",
        NavItem.defaultTabs,
        { it.joinToString(",") },
        { it.split(",").filter { id -> id.isNotBlank() } },
    )

    fun bottomNavHiddenTabs() = preferenceStore.getObject(
        "bottom_nav_hidden_tabs",
        emptyList<String>(),
        { it.joinToString(",") },
        { it.split(",").filter { id -> id.isNotBlank() } },
    )

    fun bottomNavBehaviors() = preferenceStore.getObject(
        "bottom_nav_behaviors_v1",
        persistentMapOf<String, NavBehavior>(),
        { map -> 
            map.entries.joinToString(";") { (id, b) -> 
                "$id:${b.onLongClick.javaClass.simpleName},${b.onDoubleTap.javaClass.simpleName}" 
            } 
        },
        { str ->
            val map = mutableMapOf<String, NavBehavior>()
            str.split(";").filter { it.isNotBlank() }.forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val tabId = entry.split(":")[0]
                    val actions = entry.split(":")[1].split(",")
                    if (actions.size == 2) {
                        map[tabId] = NavBehavior(
                            onLongClick = NavConfigSerializer.parseAction(actions[0]),
                            onDoubleTap = NavConfigSerializer.parseAction(actions[1])
                        )
                    }
                }
            }
            map.toImmutableMap()
        }
    )

    fun bottomNavConfigVersion() = preferenceStore.getInt("bottom_nav_config_version", 0)

    fun navLabelVisibility() = preferenceStore.getEnum("bottom_nav_label_visibility", NavLabelVisibility.ALWAYS)

    fun hideBottomBarOnScroll() = preferenceStore.getBoolean("bottom_nav_hide_on_scroll", false)

    // Adaptive Navigation Toggles
    fun adaptiveNavEnabled() = preferenceStore.getBoolean("adaptive_nav_enabled", false)
    fun adaptiveConnectivityRule() = preferenceStore.getBoolean("adaptive_rule_connectivity", true)
    fun adaptiveTimeRule() = preferenceStore.getBoolean("adaptive_rule_time", true)
    fun adaptiveTelemetryEnabled() = preferenceStore.getBoolean("adaptive_telemetry_enabled", true)

    fun updateNavConfig(config: NavConfig) {
        val lastVisible = bottomNavTabs().get()
        val lastHidden = bottomNavHiddenTabs().get()
        val lastBehaviors = bottomNavBehaviors().get()
        
        try {
            val validated = NavConfigValidator.validate(config)
            bottomNavTabs().set(validated.visibleTabs)
            bottomNavHiddenTabs().set(validated.hiddenTabs)
            bottomNavBehaviors().set(validated.behaviorMap)
            bottomNavConfigVersion().set(NavConfig.CURRENT_VERSION)
        } catch (e: Exception) {
            // Rollback to Last Known Good (LKG)
            bottomNavTabs().set(lastVisible)
            bottomNavHiddenTabs().set(lastHidden)
            bottomNavBehaviors().set(lastBehaviors)
            throw e
        }
    }

    fun migrateNavStyle() {
        val navStylePref = navStyle()
        if (navStylePref.isSet()) {
            val style = navStylePref.get()
            val visible = mutableListOf(NavItem.LIBRARY.id)
            val hidden = mutableListOf<String>()

            if (enableFeed().get() && showFeedInNavigationBar().get()) {
                visible.add(NavItem.FEED.id)
            }

            when (style) {
                NavStyle.MOVE_UPDATES_TO_MORE -> {
                    hidden.add(NavItem.UPDATES.id)
                    visible.addAll(listOf(NavItem.HISTORY.id, NavItem.BROWSE.id))
                }
                NavStyle.MOVE_HISTORY_TO_MORE -> {
                    hidden.add(NavItem.HISTORY.id)
                    visible.addAll(listOf(NavItem.UPDATES.id, NavItem.BROWSE.id))
                }
                NavStyle.MOVE_BROWSE_TO_MORE -> {
                    hidden.add(NavItem.BROWSE.id)
                    visible.addAll(listOf(NavItem.UPDATES.id, NavItem.HISTORY.id))
                }
                NavStyle.SHOW_ALL -> {
                    visible.addAll(listOf(NavItem.UPDATES.id, NavItem.HISTORY.id, NavItem.BROWSE.id))
                }
            }
            visible.add(NavItem.MORE.id)

            updateNavConfig(NavConfig(visibleTabs = visible.toImmutableList(), hiddenTabs = hidden.toImmutableList()))
            navStylePref.delete()
        }
    }

    fun enableFeed() = preferenceStore.getBoolean("enable_feed", false)

    fun showFeedInNavigationBar() = preferenceStore.getBoolean("show_feed_in_navigation_bar", false)

    fun showFeedInBrowse() = preferenceStore.getBoolean("show_feed_in_browse", false)

    fun bottomBarLabels() = preferenceStore.getBoolean("pref_show_bottom_bar_labels", true)

    fun animatedTransitions() = preferenceStore.getBoolean("pref_animated_transitions_key", true)

    fun panoramaCover() = preferenceStore.getBoolean("pref_panorama_cover", false)

    fun browsePanoramaMode() = getPanoramaMode("pref_browse_panorama_mode", "pref_browse_panorama")

    private fun getPanoramaMode(key: String, legacyKey: String) = preferenceStore.getEnum(
        key = key,
        defaultValue = PanoramaMode.valueOf(preferenceStore.getString(legacyKey, PanoramaMode.ALWAYS.name).get()),
    )

    fun dateFormat() = preferenceStore.getString("pref_date_format_key", "")

    fun relativeTime() = preferenceStore.getInt("relative_time", 7)

    fun sendCrashReports() = preferenceStore.getBoolean("pref_send_crash_reports_key", true)

    fun backClickExit() = preferenceStore.getBoolean("pref_back_click_exit_key", true)

    fun sideNavIconAlignment() = preferenceStore.getInt("pref_side_nav_icon_alignment", 0)

    fun useExternalDownloader() = preferenceStore.getBoolean("pref_use_external_downloader_key", false)

    fun externalDownloaderSelection() = preferenceStore.getString("pref_external_downloader_selection_key", "")

    companion object {
        const val DEVICE_ONLY_ONBOARDING = "device_only_onboarding"
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
