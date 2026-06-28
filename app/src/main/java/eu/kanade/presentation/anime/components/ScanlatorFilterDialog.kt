package eu.kanade.presentation.anime.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ScanlatorFilterDialog(
    availableScanlators: ImmutableList<String>,
    excludedScanlators: ImmutableSet<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val excluded = remember { mutableStateListOf(*excludedScanlators.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.scanlator)) },
        text = {
            LazyColumn {
                items(availableScanlators) { scanlator ->
                    CheckboxItem(
                        label = scanlator,
                        checked = !excluded.contains(scanlator),
                        onClick = {
                            if (excluded.contains(scanlator)) {
                                excluded.remove(scanlator)
                            } else {
                                excluded.add(scanlator)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(excluded.toSet())
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        }
    )
}
