package eu.kanade.presentation.anime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.library.service.LibraryPreferences
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationEdge

@Composable
fun PrequelSequelBox(
    relations: List<ALRelationEdge>,
    onRelationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (relations.isEmpty()) return

    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val preferredLanguage by libraryPreferences.relationTitleLanguage().collectAsState()

    val prequel = relations.find { it.relationType == "PREQUEL" }
    val sequel = relations.find { it.relationType == "SEQUEL" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (prequel != null) {
            RelationBanner(
                relationName = stringResource(MR.strings.relation_prequel),
                edge = prequel,
                preferredLanguage = preferredLanguage,
                onRelationClick = onRelationClick,
                modifier = Modifier.weight(1f)
            )
        }
        if (sequel != null) {
            RelationBanner(
                relationName = stringResource(MR.strings.relation_sequel),
                edge = sequel,
                preferredLanguage = preferredLanguage,
                onRelationClick = onRelationClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RelationBanner(
    relationName: String,
    edge: ALRelationEdge,
    preferredLanguage: String,
    onRelationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = MaterialTheme.colorScheme.primary
    
    val title = when (preferredLanguage) {
        "english" -> edge.node.title.english ?: edge.node.title.romaji ?: edge.node.title.userPreferred
        "native" -> edge.node.title.native ?: edge.node.title.romaji ?: edge.node.title.userPreferred
        else -> edge.node.title.romaji ?: edge.node.title.userPreferred
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onRelationClick(title) }
            .border(
                width = 2.dp,
                color = themeColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        // Background Image
        AsyncImage(
            model = edge.node.coverImage?.large,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Text Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = relationName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Box(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .height(2.dp)
                    .width(48.dp)
                    .background(themeColor)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PrequelSequelBoxPreview() {
    val dummyRelations = listOf(
        eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationEdge(
            relationType = "PREQUEL",
            node = eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationNode(
                id = 1,
                title = eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationTitle(userPreferred = "Dr. STONE"),
                coverImage = null
            )
        ),
        eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationEdge(
            relationType = "SEQUEL",
            node = eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationNode(
                id = 2,
                title = eu.kanade.tachiyomi.data.track.anilist.dto.ALRelationTitle(userPreferred = "Dr. STONE: New World"),
                coverImage = null
            )
        )
    )

    MaterialTheme {
        PrequelSequelBox(
            relations = dummyRelations,
            onRelationClick = {}
        )
    }
}
