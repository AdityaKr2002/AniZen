package eu.kanade.tachiyomi.ui.home

import android.content.Context
import eu.kanade.domain.base.BasePreferences
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
    val onApply: (() -> Unit)? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class NavAdaptiveEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get()
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
        if (uiPreferences.adaptiveConnectivityRule().get() && !context.isOnline() && !basePreferences.downloadedOnly().get()) {
            _currentDecision.value = AdaptiveDecision(
                reason = "Offline mode detected. Switch to Downloaded Only?",
                priority = 10,
                suggestedConfig = NavPresets.MINIMAL,
                onApply = { basePreferences.downloadedOnly().set(true) }
            )
            return
        }

        // Rule 2: Choice Fatigue (Lower Priority)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val startHour = uiPreferences.adaptiveTimeRuleStart().get()
        val endHour = uiPreferences.adaptiveTimeRuleEnd().get()
        
        val isLateNight = if (startHour <= endHour) {
            hour in startHour..endHour
        } else {
            hour >= startHour || hour <= endHour
        }

        if (uiPreferences.adaptiveTimeRule().get() && isLateNight) {
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
        }
        decision.onApply?.invoke()
        lastShiftTimestamp = System.currentTimeMillis()
        _currentDecision.value = null
    }

    fun dismissDecision() {
        _currentDecision.value = null
        // Mark this timestamp to prevent immediate re-suggestion
        lastShiftTimestamp = System.currentTimeMillis()
    }
}
