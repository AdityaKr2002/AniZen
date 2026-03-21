package eu.kanade.tachiyomi.ui.home

import android.content.Context
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavPresets
import eu.kanade.tachiyomi.util.system.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar

data class AdaptiveDecision(
    val reason: String,
    val priority: Int,
    val suggestedConfig: eu.kanade.domain.ui.model.NavConfig? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class NavAdaptiveEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val uiPreferences: UiPreferences = Injekt.get()
) {
    private val _currentDecision = MutableStateFlow<AdaptiveDecision?>(null)
    val currentDecision = _currentDecision.asStateFlow()

    companion object {
        private var lastShiftTimestamp = 0L
        private const val SHIFT_COOLDOWN = 3600000L // 1 hour stability lock
    }

    fun evaluateRules() {
        if (!uiPreferences.adaptiveNavEnabled().get()) return

        val now = System.currentTimeMillis()
        if (now - lastShiftTimestamp < SHIFT_COOLDOWN) return

        // Rule 1: Connectivity (High Priority)
        if (uiPreferences.adaptiveConnectivityRule().get() && !context.isOnline()) {
            _currentDecision.value = AdaptiveDecision(
                reason = "Offline mode detected",
                priority = 10,
                suggestedConfig = NavPresets.MINIMAL // Or a custom offline layout
            )
            return
        }

        // Rule 2: Choice Fatigue (Lower Priority)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (uiPreferences.adaptiveTimeRule().get() && hour in 1..5) {
            _currentDecision.value = AdaptiveDecision(
                reason = "Late night: suggesting minimal layout",
                priority = 5,
                suggestedConfig = NavPresets.MINIMAL
            )
            return
        }

        _currentDecision.value = null
    }

    fun applyDecision(decision: AdaptiveDecision) {
        decision.suggestedConfig?.let {
            uiPreferences.updateNavConfig(it)
            lastShiftTimestamp = System.currentTimeMillis()
            _currentDecision.value = null
        }
    }

    fun dismissDecision() {
        _currentDecision.value = null
        // Mark this timestamp to prevent immediate re-suggestion
        lastShiftTimestamp = System.currentTimeMillis()
    }
}
