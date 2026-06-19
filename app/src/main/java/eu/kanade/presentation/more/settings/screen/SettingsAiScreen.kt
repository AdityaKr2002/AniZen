package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.ai.AiAssistantScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsAiScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_ai

    @Composable
    override fun getPreferences(): List<Preference> {
        val aiPreferences = remember { Injekt.get<AiPreferences>() }
        val navigator = LocalNavigator.currentOrThrow
        val enableAi by aiPreferences.enableAi().collectAsState()

        return if (enableAi) {
            listOf(
                getMainGroup(aiPreferences, navigator),
                getAssistantGroup(aiPreferences),
                getStatisticsGroup(aiPreferences),
            )
        } else {
            listOf(
                getMainGroup(aiPreferences, navigator),
            )
        }
    }

    @Composable
    private fun getMainGroup(aiPreferences: AiPreferences, navigator: cafe.adriel.voyager.navigator.Navigator): Preference.PreferenceGroup {
        val enableAiPref = aiPreferences.enableAi()
        val enableAi by enableAiPref.collectAsState()
        val aiEngine by aiPreferences.aiEngine().collectAsState()

        return Preference.PreferenceGroup(
            title = "AI Configuration",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAiPref,
                    title = "Enable AI Features",
                    subtitle = "Allows smart search, summaries, and diagnostic help",
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = aiPreferences.aiEngine(),
                    title = "AI Model Provider",
                    subtitle = "Choose the AI service provider",
                    entries = persistentMapOf(
                        "gemini" to "Google Gemini",
                        "groq" to "Groq",
                    ),
                    enabled = enableAi,
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.geminiApiKey(),
                    title = stringResource(MR.strings.pref_ai_gemini_api_key),
                    subtitle = stringResource(MR.strings.pref_ai_gemini_api_key_summary),
                    enabled = enableAi && aiEngine == "gemini",
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.groqApiKey(),
                    title = "Groq API Key",
                    subtitle = "Used for high-speed inference",
                    enabled = enableAi && aiEngine == "groq",
                )
            ),
        )
    }

    @Composable
    private fun getAssistantGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "Diagnostic Assistant",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.enableAiAssistant(),
                    title = "Enable Assistant",
                    subtitle = "Enables conversational diagnostics",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.aiAssistantLogs(),
                    title = "Ingest Error Logs",
                    subtitle = "Allows the assistant to analyze stack traces",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.aiAssistantLibrary(),
                    title = "Ingest Library Context",
                    subtitle = "Allows the assistant to analyze your collection",
                ),
                Preference.PreferenceItem.MultiLineEditTextPreference(
                    pref = aiPreferences.aiSystemPrompt(),
                    title = "Custom System Prompt",
                    subtitle = "Override the default behavioral instructions",
                    canBeBlank = true,
                ),
            ),
        )
    }

    @Composable
    private fun getStatisticsGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "Advanced Analytics",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.enableAiStatistics(),
                    title = "Data Summarization",
                    subtitle = "Generates technical summaries in the Statistics module",
                ),
            ),
        )
    }
}
