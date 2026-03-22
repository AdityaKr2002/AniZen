package eu.kanade.tachiyomi.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.ui.UiPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import android.content.Context
import eu.kanade.tachiyomi.util.system.networkStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import eu.kanade.core.preference.asState

class HomeScreenModel(
    context: Context,
    private val uiPreferences: UiPreferences = Injekt.get()
) : ScreenModel {

    val adaptiveEngine = NavAdaptiveEngine(context, screenModelScope)

    init {
        // Polling evaluation (cooldown enforced)
        screenModelScope.launch {
            while (true) {
                adaptiveEngine.evaluateRules()
                delay(60000) // Every minute
            }
        }

        // Real-time triggers (force bypasses cooldown for state changes)
        screenModelScope.launch {
            merge(
                context.networkStateFlow(),
                uiPreferences.adaptiveNavEnabled().changes(),
                uiPreferences.adaptiveConnectivityRule().changes(),
                uiPreferences.adaptiveTimeRule().changes()
            ).collectLatest {
                adaptiveEngine.evaluateRules(force = true)
            }
        }
    }
}
