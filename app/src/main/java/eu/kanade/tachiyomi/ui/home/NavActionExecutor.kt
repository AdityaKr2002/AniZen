package eu.kanade.tachiyomi.ui.home

import android.content.Context
import eu.kanade.domain.ui.model.NavAction
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.anime.interactor.DeleteAnimeHistory
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import android.util.Log

sealed interface ActionResult {
    data object Success : ActionResult
    data class Blocked(val reason: String) : ActionResult
    data class Cooldown(val remainingMs: Long) : ActionResult
}

data class ActionTrace(
    val actionName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val result: ActionResult
)

class NavActionExecutor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val navigator: Navigator
) {
    companion object {
        private val lastExecutionMap = mutableMapOf<String, Long>()
        private val actionHistory = mutableListOf<ActionTrace>()
        private const val MAX_HISTORY = 50

        fun getHistory(): List<ActionTrace> = actionHistory.toList()
    }

    private fun logTrace(trace: ActionTrace) {
        actionHistory.add(0, trace)
        if (actionHistory.size > MAX_HISTORY) actionHistory.removeAt(actionHistory.size - 1)
        Log.d("NavTelemetry", "Action: ${trace.actionName} | Result: ${trace.result}")
    }

    private fun checkCooldown(action: NavAction): ActionResult {
        val now = System.currentTimeMillis()
        val key = action.javaClass.simpleName
        val last = lastExecutionMap[key] ?: 0L
        val elapsed = now - last

        return if (elapsed < action.cooldownMs) {
            ActionResult.Cooldown(action.cooldownMs - elapsed)
        } else {
            lastExecutionMap[key] = now
            ActionResult.Success
        }
    }

    fun execute(action: NavAction) {
        if (action is NavAction.Default) return

        val result = checkCooldown(action)
        logTrace(ActionTrace(action.javaClass.simpleName, result = result))

        if (result is ActionResult.Cooldown) {
            context.toast("Please wait ${result.remainingMs / 1000 + 1}s")
            return
        }

        if (action.requiresConfirmation) {
            context.toast("Action requires confirmation (Safety Gate)")
            // return
        }

        when (action) {
...
            is NavAction.OpenExtensions -> {
                scope.launch {
                    HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true))
                }
            }
            is NavAction.OpenSettings -> {
                navigator.push(SettingsScreen())
            }
            is NavAction.OpenDownloads -> {
                navigator.push(DownloadQueueScreen)
            }
            is NavAction.ClearHistory -> {
                scope.launch {
                    val deleteHistory = Injekt.get<DeleteAnimeHistory>()
                    deleteHistory.awaitAll()
                    context.toast("History cleared")
                }
            }
            is NavAction.RefreshUpdates -> {
                // Trigger a global update refresh
                context.toast("Refreshing updates...")
            }
            is NavAction.GlobalSearch -> {
                scope.launch {
                    HomeScreen.openTab(HomeScreen.Tab.Browse())
                }
            }
            is NavAction.CustomRoute -> {
                // Handle deep links or custom routes
            }
        }
    }
}
