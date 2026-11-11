package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
 * Vue d'ensemble d'un jour (PS-139) : tous ses souvenirs en grille, rangés par année de la plus
 * récente à la plus ancienne, chacun avec son état (à trier, gardé, marqué, favori). Elle aide à se
 * remémorer la journée et à voir les photos proches avant de décider. Toucher un souvenir à trier
 * ouvre la pile sur lui ; une photo déjà triée s'ouvre en grand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOverviewScreen(
    day: LocalDate,
    state: DayOverviewUiState,
    onEvent: (DayOverviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // Les vignettes du jour, sur l'accueil, s'agrandissent jusqu'à cet écran (PS-141).
        modifier = modifier.sharedScreen(SharedScreenKey.Overview(day), RectangleShape),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.deck_title, dayAndMonth(LocalContext.current, day)), maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(DayOverviewEvent.Back) }) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                DayOverviewUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                DayOverviewUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.day_overview_error), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { onEvent(DayOverviewEvent.Retry) }) {
                            Text(stringResource(R.string.summary_retry))
                        }
                    }
                }

                is DayOverviewUiState.Loaded -> {
                    LoadedOverview(day, state, onEvent)
                }
            }
        }
    }
}

/** Grille des souvenirs et, s'il en reste à trier, le bouton qui ouvre la pile. */
@Composable
private fun LoadedOverview(
    day: LocalDate,
    state: DayOverviewUiState.Loaded,
    onEvent: (DayOverviewEvent) -> Unit,
) {
    if (state.overview.memoryCount == 0) {
        Text(
            text = stringResource(R.string.day_overview_empty),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(TILE_MIN_SIZE),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).testTag(DAY_OVERVIEW_GRID_TAG),
        ) {
            state.overview.years.forEach { year ->
                item(key = "year-${year.year}", span = { GridItemSpan(maxLineSpan) }) { YearHeader(day, year) }
                items(year.memories, key = { it.media.id.value }) { memory ->
                    MemoryTile(memory, onOpen = { onEvent(DayOverviewEvent.Open(memory.media.id)) })
                }
            }
        }
        if (state.remainingCount > 0) {
            Button(
                onClick = { onEvent(DayOverviewEvent.StartSorting) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).heightIn(min = 56.dp),
            ) {
                Text(pluralStringResource(R.plurals.day_overview_sort, state.remainingCount, state.remainingCount))
            }
        }
    }
    state.openPhoto?.let { PhotoViewer(it, onClose = { onEvent(DayOverviewEvent.ClosePhoto) }) }
}

/** « 2023 · Il y a 3 ans · 5 souvenirs », annoncé comme un titre par TalkBack. */
@Composable
private fun YearHeader(
    day: LocalDate,
    year: OverviewYear,
) {
    val yearsAgo = day.year - year.year
    val count = year.memories.size
    Text(
        text =
            stringResource(
                R.string.day_overview_year,
                year.year,
                pluralStringResource(R.plurals.deck_years_ago, yearsAgo, yearsAgo),
                pluralStringResource(R.plurals.day_overview_memories, count, count),
            ),
        style = MaterialTheme.typography.monoLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp).semantics { heading() },
    )
}

/**
 * Un souvenir : sa vignette carrée, une pastille de son état en bas à droite et ▶ pour une vidéo.
 * Un média marqué pour suppression est estompé. TalkBack lit « Photo, gardée, en favori ».
 */
@Composable
private fun MemoryTile(
    memory: OverviewMemory,
    onOpen: () -> Unit,
) {
    val description = memoryDescription(memory)
    val openable = memory.decision == null || memory.media.type == MediaType.IMAGE
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = openable, onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = description
                    testTag = "$MEMORY_TILE_TAG${memory.media.id.value}"
                    role = Role.Image
                    if (openable) {
                        onClick {
                            onOpen()
                            true
                        }
                    }
                },
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

/** Repère de test de la grille. */
internal const val DAY_OVERVIEW_GRID_TAG = "day_overview_grid"

/** Préfixe du repère de test d'un souvenir, suivi de son identifiant. */
internal const val MEMORY_TILE_TAG = "day_overview_memory_"

/** Largeur minimale d'une case : 3 colonnes sur un téléphone, plus sur un grand écran. */
private val TILE_MIN_SIZE = 96.dp

/** Taille d'une pastille d'état. */
private val BADGE_SIZE = 22.dp

/** Opacité d'un souvenir marqué pour suppression. */
private const val MARKED_ALPHA = 0.5f
