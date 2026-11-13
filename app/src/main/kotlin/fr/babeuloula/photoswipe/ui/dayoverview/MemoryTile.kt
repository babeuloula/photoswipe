package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.OverviewMemory
import fr.babeuloula.photoswipe.domain.model.OverviewYear
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.ui.common.SharedScreenKey
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.common.sharedScreen
import fr.babeuloula.photoswipe.ui.deck.PhotoViewer
import fr.babeuloula.photoswipe.ui.deck.dayAndMonth
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.LocalDate

/**
 * Un souvenir : sa vignette carrée, une pastille de son état en bas à droite et ▶ pour une vidéo.
 * Un média marqué pour suppression est estompé. TalkBack lit « Photo, gardée, en favori ».
 *
 * Un appui long le sélectionne pour un tri par lot (PS-159) ; TalkBack propose la même chose par
 * l'action « Sélectionner ». Pendant une sélection, un appui coche ou décoche, et la case cochée
 * est entourée et marquée d'une coche.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MemoryTile(
    memory: OverviewMemory,
    selection: TileSelection,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val description =
        listOfNotNull(
            memoryDescription(memory),
            stringResource(R.string.day_overview_selected_state).takeIf { selection.selected },
        ).joinToString(", ")
    val openable = selection.active || memory.decision == null || memory.media.type == MediaType.IMAGE
    val toggleLabel =
        stringResource(if (selection.selected) R.string.day_overview_unselect else R.string.day_overview_select)
    val selectedBorder =
        if (selection.selected) {
            Modifier.border(
                3.dp,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.shapes.small,
            )
        } else {
            Modifier
        }
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .then(selectedBorder)
                .combinedClickable(enabled = true, onClick = { if (openable) onOpen() }, onLongClick = onToggle)
                .clearAndSetSemantics {
                    contentDescription = description
                    testTag = "$MEMORY_TILE_TAG${memory.media.id.value}"
                    role = Role.Image
                    if (selection.active) selected = selection.selected
                    if (openable) {
                        onClick {
                            onOpen()
                            true
                        }
                    }
                    customActions =
                        listOf(
                            CustomAccessibilityAction(toggleLabel) {
                                onToggle()
                                true
                            },
                        )
                },
    ) {
        MemoryTileContent(memory, selection.selected)
    }
}

/** Vignette d'un souvenir, estompée s'il est marqué, avec ses pastilles et sa coche de sélection. */
@Composable
private fun BoxScope.MemoryTileContent(
    memory: OverviewMemory,
    selected: Boolean,
) {
    Thumbnail(
        memory.media,
        Modifier.fillMaxSize().alpha(
            if (memory.decision ==
                ReviewDecision.MARKED_FOR_DELETION
            ) {
                MARKED_ALPHA
            } else {
                1f
            },
        ),
    )
    memory.similar?.let { SimilarBadge(it, Modifier.align(Alignment.TopEnd)) }
    if (memory.media.type == MediaType.VIDEO) {
        StateBadge(
            R.drawable.ic_play_arrow_filled,
            MaterialTheme.colorScheme.primaryFixed,
            Modifier.align(Alignment.TopStart),
        )
    }
    if (selected) {
        Icon(
            painterResource(R.drawable.ic_check_circle_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(SELECTED_MARK_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
        )
    }
    Row(Modifier.align(Alignment.BottomEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val colors = MaterialTheme.swipeColors
        if (memory.favorite) StateBadge(R.drawable.ic_favorite_filled, colors.favoriteContainer)
        when (memory.decision) {
            ReviewDecision.KEPT -> StateBadge(R.drawable.ic_check, colors.keepContainer)
            ReviewDecision.MARKED_FOR_DELETION -> StateBadge(R.drawable.ic_delete, colors.deleteContainer)
            ReviewDecision.TRASHED, null -> Unit
        }
    }
}

/** « ⧉ 2/3 » : la photo a des voisines prises à la suite (PS-140). */
@Composable
private fun SimilarBadge(
    similar: SimilarGroup,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryFixed)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_burst_mode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primaryFixed,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.deck_similar, similar.position, similar.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primaryFixed,
        )
    }
}

/** Pastille ronde d'un état, sur [container]. */
@Composable
private fun StateBadge(
    @DrawableRes icon: Int,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(2.dp)
                .size(BADGE_SIZE)
                .clip(CircleShape)
                .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

/** « Photo, à trier », « Vidéo, gardée, en favori », « Photo, marquée pour suppression ». */
@Composable
private fun memoryDescription(memory: OverviewMemory): String {
    val type =
        stringResource(
            if (memory.media.type ==
                MediaType.VIDEO
            ) {
                R.string.day_overview_video
            } else {
                R.string.day_overview_photo
            },
        )
    val decision =
        stringResource(
            when (memory.decision) {
                ReviewDecision.KEPT -> R.string.day_overview_kept
                ReviewDecision.MARKED_FOR_DELETION, ReviewDecision.TRASHED -> R.string.day_overview_marked
                null -> R.string.day_overview_to_sort
            },
        )
    val similar =
        memory.similar?.let { stringResource(R.string.day_overview_similar, it.position, it.size) }
    val parts =
        listOfNotNull(
            type,
            decision,
            stringResource(R.string.day_overview_favorite).takeIf { memory.favorite },
            similar,
        )
    return parts.joinToString(", ")
}

/** Taille de la coche d'une case sélectionnée. */
private val SELECTED_MARK_SIZE = 32.dp

/** Taille d'une pastille d'état. */
private val BADGE_SIZE = 22.dp

/** Opacité d'un souvenir marqué pour suppression. */
private const val MARKED_ALPHA = 0.5f
