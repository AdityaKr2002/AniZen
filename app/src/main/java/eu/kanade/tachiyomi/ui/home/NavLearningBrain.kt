package eu.kanade.tachiyomi.ui.home

import android.content.Context
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavConfig
import eu.kanade.domain.ui.model.NavItem
import eu.kanade.domain.ui.model.NavPresets
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object NavLearningBrain {

    fun recommendLayout(context: Context): NavConfig {
        val history = NavActionExecutor.getHistory()
        if (history.isEmpty()) return NavPresets.DEFAULT

        // Calculate scores for each tab
        val scores = mutableMapOf<String, Float>()
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        history.forEach { trace ->
            val tabId = trace.tabId ?: return@forEach
            val ageDays = (now - trace.timestamp).coerceAtLeast(0L).toFloat() / dayMs
            
            // Recency weighting: actions today are worth 1.0, actions 7 days ago are worth 0.3
            val weight = (1.0f / (1.0f + ageDays)).coerceAtLeast(0.1f)
            
            scores[tabId] = (scores[tabId] ?: 0f) + weight
        }

        // Required tabs
        val visible = mutableListOf(NavItem.LIBRARY.id)
        val hidden = mutableListOf<String>()

        // Potential tabs to rank
        val candidateIds = listOf(
            NavItem.FEED.id,
            NavItem.UPDATES.id,
            NavItem.HISTORY.id,
            NavItem.BROWSE.id
        )

        // Sort candidates by score
        val rankedCandidates = candidateIds.sortedByDescending { scores[it] ?: 0f }

        // Take top 3 most used to keep it balanced (Max 5 tabs total with Library + More)
        visible.addAll(rankedCandidates.take(3))
        
        // Ensure "More" is always at the end
        visible.add(NavItem.MORE.id)

        // Everything else goes to hidden
        val allIds = NavItem.entries.map { it.id }.toSet()
        hidden.addAll(allIds.filter { it !in visible })

        return NavConfig(
            visibleTabs = visible.distinct().toImmutableList(),
            hiddenTabs = hidden.distinct().toImmutableList(),
            behaviorMap = Injekt.get<UiPreferences>().bottomNavBehaviors().get()
        )
    }

    fun hasEnoughData(context: Context): Boolean {
        return NavActionExecutor.getHistory().size >= 10
    }
}
