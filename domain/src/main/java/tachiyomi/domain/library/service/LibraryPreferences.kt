package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.model.LibraryManga

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun libraryDisplayMode() = preferenceStore.getLong("pref_library_display_mode_key", 0L)

    fun libraryMangaSortingMode() = preferenceStore.getLong("library_sorting_mode", 0L)

    fun libraryAnimeSortingMode() = preferenceStore.getLong("library_anime_sorting_mode", 0L)

    fun libraryUpdateInterval() = preferenceStore.getInt("pref_library_update_interval_key", 0)

    fun libraryUpdateLastTimestamp() = preferenceStore.getLong("library_update_last_timestamp", 0L)

    fun libraryUpdateDeviceRestriction() = preferenceStore.getStringSet("library_update_restriction", setOf())

    fun libraryUpdateMangaRestriction() = preferenceStore.getStringSet("library_update_manga_restriction", setOf())

    fun libraryUpdateAnimeRestriction() = preferenceStore.getStringSet("library_update_anime_restriction", setOf())

    fun libraryUpdateCategories() = preferenceStore.getStringSet("library_update_categories", setOf())

    fun libraryUpdateCategoriesFlags() = preferenceStore.getInt("library_update_categories_flags", 1)

    fun autoUpdateMangaRestrictions() = preferenceStore.getStringSet("pref_library_update_manga_restriction", setOf())

    fun autoUpdateAnimeRestrictions() = preferenceStore.getStringSet("pref_library_update_anime_restriction", setOf())

    fun showMangaContinueReadingButton() = preferenceStore.getBoolean("pref_show_manga_continue_reading_button", false)

    fun showAnimeContinueWatchingButton() = preferenceStore.getBoolean("pref_show_anime_continue_watching_button", false)

    fun filterMangaByDownload() = preferenceStore.getLong("pref_filter_manga_by_download", 0L)

    fun filterAnimeByDownload() = preferenceStore.getLong("pref_filter_anime_by_download", 0L)

    fun filterMangaByUnread() = preferenceStore.getLong("pref_filter_manga_by_unread", 0L)

    fun filterAnimeByUnseen() = preferenceStore.getLong("pref_filter_anime_by_unseen", 0L)

    fun filterMangaByStarted() = preferenceStore.getLong("pref_filter_manga_by_started", 0L)

    fun filterAnimeByStarted() = preferenceStore.getLong("pref_filter_anime_by_started", 0L)

    fun filterMangaByCompleted() = preferenceStore.getLong("pref_filter_manga_by_completed", 0L)

    fun filterAnimeByCompleted() = preferenceStore.getLong("pref_filter_anime_by_completed", 0L)

    fun filterMangaByBookmarked() = preferenceStore.getLong("pref_filter_manga_by_bookmarked", 0L)

    fun filterAnimeByBookmarked() = preferenceStore.getLong("pref_filter_anime_by_bookmarked", 0L)

    // AM (FILLERMARK) -->
    fun filterAnimeByFillermarked() = preferenceStore.getLong("pref_filter_anime_by_fillermarked", 0L)
    // <-- AM (FILLERMARK)

    fun mangaSortingMode() = preferenceStore.getLong("manga_sorting_mode", 0L)

    fun animeSortingMode() = preferenceStore.getLong("anime_sorting_mode", 0L)

    fun mangaSortingDirection() = preferenceStore.getLong("manga_sorting_direction", 0L)

    fun animeSortingDirection() = preferenceStore.getLong("anime_sorting_direction", 0L)

    fun mangaDisplayMode() = preferenceStore.getLong("manga_display_mode", 0L)

    fun animeDisplayMode() = preferenceStore.getLong("anime_display_mode", 0L)

    fun mangaGridSize() = preferenceStore.getInt("manga_grid_size", 0)

    fun animeGridSize() = preferenceStore.getInt("anime_grid_size", 0)

    fun showMangaDownloadBadge() = preferenceStore.getBoolean("pref_show_manga_download_badge", false)

    fun showAnimeDownloadBadge() = preferenceStore.getBoolean("pref_show_anime_download_badge", false)

    fun showMangaUnreadBadge() = preferenceStore.getBoolean("pref_show_manga_unread_badge", true)

    fun showAnimeUnseenBadge() = preferenceStore.getBoolean("pref_show_anime_unseen_badge", true)

    fun showMangaLocalBadge() = preferenceStore.getBoolean("pref_show_manga_local_badge", true)

    fun showAnimeLocalBadge() = preferenceStore.getBoolean("pref_show_anime_local_badge", true)

    fun showMangaLanguageBadge() = preferenceStore.getBoolean("pref_show_manga_language_badge", false)

    fun showAnimeLanguageBadge() = preferenceStore.getBoolean("pref_show_anime_language_badge", false)

    fun showLatestEpisodeWhenViewInLibrary() = preferenceStore.getBoolean("pref_show_latest_episode_when_view_in_library", true)

    fun useHierarchicalSeasons() = preferenceStore.getBoolean("use_hierarchical_seasons", false)

    fun categoryTabs() = preferenceStore.getBoolean("pref_category_tabs", true)

    fun categoryNumberOfItems() = preferenceStore.getBoolean("pref_category_number_of_items", false)

    fun tappedManga() = preferenceStore.getLong("pref_tapped_manga", -1L)

    fun tappedAnime() = preferenceStore.getLong("pref_tapped_anime", -1L)

    fun showUpdatesCount() = preferenceStore.getBoolean("pref_show_updates_count", true)

    fun lastSelectedSeason(animeId: Long) = preferenceStore.getString("last_selected_season_$animeId", "")

    fun seasonGroupingMode() = preferenceStore.getEnum(
        "default_chapter_group_by_season_v2",
        SeasonGrouping.Tabs,
    )

    // AY -->
    val filterSeasonByDownload = preferenceStore.getLong("pref_filter_season_by_download_v2", Anime.SHOW_ALL)
    val filterSeasonByUnseen = preferenceStore.getLong("pref_filter_season_by_unseen_v2", Anime.SHOW_ALL)
    val filterSeasonByStarted = preferenceStore.getLong("pref_filter_season_by_started_v2", Anime.SHOW_ALL)
    val filterSeasonByCompleted = preferenceStore.getLong("pref_filter_season_by_completed_v2", Anime.SHOW_ALL)
    val filterSeasonByBookmarked = preferenceStore.getLong("pref_filter_season_by_bookmarked_v2", Anime.SHOW_ALL)
    val filterSeasonByFillermarked = preferenceStore.getLong("pref_filter_season_by_fillermarked_v2", Anime.SHOW_ALL)

    val sortSeasonBySourceOrNumber = preferenceStore.getLong("pref_sort_season_by_source_or_number_v2", Anime.SEASON_SORT_SEASON)
    val sortSeasonByAscendingOrDescending = preferenceStore.getLong("pref_sort_season_by_ascending_or_descending_v2", Anime.SEASON_SORT_ASC)

    val seasonDisplayGridMode = preferenceStore.getLong("pref_season_display_grid_mode_v2", 0L)
    val seasonDisplayGridSize = preferenceStore.getInt("pref_season_display_grid_size_v2", 0)

    val seasonDownloadOverlay = preferenceStore.getBoolean("pref_season_download_overlay_v2", false)
    val seasonUnseenOverlay = preferenceStore.getBoolean("pref_season_unseen_overlay_v2", true)
    val seasonLocalOverlay = preferenceStore.getBoolean("pref_season_local_overlay_v2", true)
    val seasonLangOverlay = preferenceStore.getBoolean("pref_season_lang_overlay_v2", false)
    val seasonContinueOverlay = preferenceStore.getBoolean("pref_season_continue_overlay_v2", true)

    val seasonDisplayMode = preferenceStore.getLong("pref_season_display_mode_v2", Anime.SEASON_DISPLAY_MODE_NUMBER)

    fun setSeasonSettingsDefault(anime: Anime) {
        filterSeasonByDownload.set(anime.seasonDownloadedFilterRaw)
        filterSeasonByUnseen.set(anime.seasonUnseenFilterRaw)
        filterSeasonByStarted.set(anime.seasonStartedFilterRaw)
        filterSeasonByCompleted.set(anime.seasonCompletedFilterRaw)
        filterSeasonByBookmarked.set(anime.seasonBookmarkedFilterRaw)
        filterSeasonByFillermarked.set(anime.seasonFillermarkedFilterRaw)
        sortSeasonBySourceOrNumber.set(anime.seasonSorting)
        sortSeasonByAscendingOrDescending.set(
            if (anime.seasonSortDescending()) Anime.SEASON_SORT_DESC else Anime.SEASON_SORT_ASC,
        )
        seasonDisplayGridMode.set(tachiyomi.domain.anime.model.SeasonDisplayMode.toLong(anime.seasonDisplayGridMode))
        seasonDisplayGridSize.set(anime.seasonDisplayGridSize)
        seasonDownloadOverlay.set(anime.seasonDownloadedOverlay)
        seasonUnseenOverlay.set(anime.seasonUnseenOverlay)
        seasonLocalOverlay.set(anime.seasonLocalOverlay)
        seasonLangOverlay.set(anime.seasonLangOverlay)
        seasonContinueOverlay.set(anime.seasonContinueOverlay)
        seasonDisplayMode.set(anime.seasonDisplayMode)
    }
    // <-- AY

    @Deprecated("Use seasonGroupingMode")
    fun groupEpisodeBySeason() = preferenceStore.getBoolean(
        "default_chapter_group_by_season",
        true,
    )

    fun swipeToStartAction() = preferenceStore.getEnum("pref_swipe_to_start_action", EpisodeSwipeAction.Disabled)

    fun swipeToEndAction() = preferenceStore.getEnum("pref_swipe_to_end_action", EpisodeSwipeAction.Disabled)

    enum class SeasonGrouping {
        Disabled,
        Headers,
        Tabs,
    }

    enum class EpisodeSwipeAction {
        Disabled,
        ToggleBookmark,
        ToggleSeen,
        Download,
    }

    companion object {
        const val DEVICE_ONLY_ON_WIFI = "wifi"
        const val DEVICE_CHARGING = "ac"
        const val DEVICE_BATTERY_NOT_LOW = "battery_not_low"

        const val MANGA_NON_COMPLETED = "manga_ongoing"
        const val MANGA_HAS_UNREAD = "manga_fully_read"
        const val MANGA_NON_READ = "manga_started"
        const val MANGA_OUTSIDE_RELEASE_PERIOD = "manga_outside_release_period"

        const val ANIME_NON_COMPLETED = "anime_ongoing"
        const val ANIME_HAS_UNSEEN = "anime_fully_seen"
        const val ANIME_NON_SEEN = "anime_started"
        const val ANIME_OUTSIDE_RELEASE_PERIOD = "anime_outside_release_period"
    }
}
