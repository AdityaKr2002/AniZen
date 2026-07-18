package eu.kanade.presentation.anime.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.animesource.model.Credit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.util.clickableNoIndication
import java.net.URLEncoder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.util.lang.withIOContext

@Composable
fun CreditDetailsDialog(
    credit: Credit,
    onDismissRequest: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var biography by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val parsedDetails = remember(credit.url) {
        if (credit.url == null) return@remember null
        val uri = credit.url
        when {
            uri.contains("anilist.co/character/") -> {
                val id = uri.substringAfter("anilist.co/character/").substringBefore("/").toLongOrNull()
                id?.let { Pair("anilist-character", it) }
            }
            uri.contains("anilist.co/staff/") -> {
                val id = uri.substringAfter("anilist.co/staff/").substringBefore("/").toLongOrNull()
                id?.let { Pair("anilist-staff", it) }
            }
            uri.contains("themoviedb.org/person/") -> {
                val id = uri.substringAfter("themoviedb.org/person/").substringBefore("/").toLongOrNull()
                id?.let { Pair("tmdb-person", it) }
            }
            else -> null
        }
    }

    LaunchedEffect(parsedDetails) {
        if (parsedDetails == null) return@LaunchedEffect
        isLoading = true
        biography = null
        try {
            val trackerManager = Injekt.get<eu.kanade.tachiyomi.data.track.TrackerManager>()
            val result = withIOContext {
                when (parsedDetails.first) {
                    "anilist-character" -> trackerManager.aniList.api.getCharacterDescription(parsedDetails.second)
                    "anilist-staff" -> trackerManager.aniList.api.getStaffDescription(parsedDetails.second)
                    "tmdb-person" -> trackerManager.tmdb.api.getPersonBiography(parsedDetails.second)
                    else -> null
                }
            }
            biography = result
        } catch (_: Exception) {
            // Log/ignore
        } finally {
            isLoading = false
        }
    }

    val cleanBio = remember(biography) {
        biography?.let {
            it.replace(Regex("<br\\s*/?>"), "\n")
              .replace(Regex("<[^>]*>"), "")
              .trim()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissWithAnimation() {
        isVisible = false
        scope.launch {
            delay(250)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoIndication { dismissWithAnimation() }
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
                exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.8f),
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .clickableNoIndication {} // Prevent click propagation to background
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                ),
                            ),
                            shape = RoundedCornerShape(24.dp),
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Close button at top right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = { dismissWithAnimation() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }

                        // Profile Image
                        val ctx = LocalContext.current
                        val imageModifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                shape = CircleShape,
                            )

                        if (!credit.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(credit.image_url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = credit.name,
                                modifier = imageModifier,
                            )
                        } else {
                            Box(
                                modifier = imageModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Name
                        Text(
                            text = credit.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role / Character Info
                        val isCharacter = credit.character != null
                        val hasVA = !credit.role.isNullOrBlank()

                        if (isCharacter && hasVA && credit.role != credit.character) {
                            Text(
                                text = "Character",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = credit.character ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Played by / VA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = credit.role ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } else if (!credit.role.isNullOrBlank()) {
                            Text(
                                text = "Role",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = credit.role ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                val bioText = cleanBio
                                if (!bioText.isNullOrBlank()) {
                                    Text(
                                        text = bioText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "No description available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons
                        Button(
                            onClick = {
                                dismissWithAnimation()
                                onSearch(credit.name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Search in AniZen")
                        }

                        if (!credit.url.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val buttonText = remember(credit.url) {
                                when {
                                    credit.url.contains("anilist.co") -> "Open on AniList"
                                    credit.url.contains("themoviedb.org") -> "Open on TMDB"
                                    else -> "Open Source Page"
                                }
                            }

                            Button(
                                onClick = {
                                    uriHandler.openUri(credit.url)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = buttonText)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val escapedName = URLEncoder.encode(credit.name, "UTF-8")
                                uriHandler.openUri("https://www.google.com/search?q=$escapedName")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Search on Web (Google)")
                        }
                    }
                }
            }
        }
    }
}
