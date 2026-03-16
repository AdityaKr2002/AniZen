package eu.kanade.tachiyomi.ui.anime.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.anime.AnimeNotesScreen
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.CustomAnimeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeNotesScreen(
    private val anime: Anime,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { Model(anime) }
        val state by screenModel.state.collectAsState()

        AnimeNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = screenModel::updateNotes,
        )
    }

    private class Model(
        private val anime: Anime,
        private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    ) : StateScreenModel<State>(State(anime, anime.note.orEmpty())) {

        fun updateNotes(content: String) {
            if (content == state.value.notes) return

            mutableState.update {
                it.copy(notes = content)
            }

            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            tachiyomi.core.common.util.lang.launchIO {
                setCustomAnimeInfo.set(
                    CustomAnimeInfo(
                        id = anime.id,
                        title = null,
                        note = content.trim().ifBlank { null },
                    ),
                )
            }
        }
    }

    @Immutable
    data class State(
        val anime: Anime,
        val notes: String,
    )
}
