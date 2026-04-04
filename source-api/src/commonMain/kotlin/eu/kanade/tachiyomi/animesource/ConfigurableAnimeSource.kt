package eu.kanade.tachiyomi.animesource

import android.content.SharedPreferences
import eu.kanade.tachiyomi.animesource.utils.sourcePreferences

interface ConfigurableAnimeSource : AnimeSource {

    /**
     * Gets instance of [SharedPreferences] scoped to the specific source.
     *
     * @since extensions-lib 1.5
     */
    fun getSourcePreferences(): SharedPreferences = sourcePreferences()

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
