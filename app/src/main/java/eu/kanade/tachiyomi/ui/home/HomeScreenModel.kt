package eu.kanade.tachiyomi.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.ui.UiPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import android.content.Context

class HomeScreenModel(
    context: Context,
    private val uiPreferences: UiPreferences = Injekt.get()
) : ScreenModel {

    val adaptiveEngine = NavAdaptiveEngine(context, screenModelScope)

    init {
        screenModelScope.launch {
            while (true) {
                adaptiveEngine.evaluateRules()
                delay(60000) // Every minute
            }
        }
    }
}
