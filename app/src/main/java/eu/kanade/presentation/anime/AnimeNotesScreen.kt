package eu.kanade.presentation.anime

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.anime.notes.AnimeNotesScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun AnimeNotesScreen(
    state: AnimeNotesScreen.State,
    navigateUp: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = state.anime.title,
                subtitle = stringResource(MR.strings.action_edit_notes),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        OutlinedTextField(
            value = state.notes,
            onValueChange = onUpdate,
            modifier = Modifier
                .padding(contentPadding)
                .padding(MaterialTheme.padding.medium)
                .fillMaxSize(),
            label = { Text(text = stringResource(MR.strings.notes)) },
            placeholder = { Text(text = stringResource(MR.strings.notes_placeholder)) },
        )
    }
}
