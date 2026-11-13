package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val selected = (state as? DayOverviewUiState.Loaded)?.selected.orEmpty()
    BackHandler(enabled = selected.isNotEmpty()) { onEvent(DayOverviewEvent.ClearSelection) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Les vignettes du jour, sur l'accueil, s'agrandissent jusqu'à cet écran (PS-141).
        modifier = modifier.sharedScreen(SharedScreenKey.Overview(day), RectangleShape),
        topBar = {
            OverviewTopBar(day, selected.size, onEvent)
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

/**
 * Barre du haut : la date du jour et le retour ; pendant une sélection (PS-159), le nombre de
 * souvenirs sélectionnés et une croix qui la quitte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTopBar(
    day: LocalDate,
    selectedCount: Int,
    onEvent: (DayOverviewEvent) -> Unit,
) {
    val selecting = selectedCount > 0
    TopAppBar(
        title = {
            Text(
                if (selecting) {
                    pluralStringResource(R.plurals.day_overview_selected, selectedCount, selectedCount)
                } else {
                    stringResource(R.string.deck_title, dayAndMonth(LocalContext.current, day))
                },
                maxLines = 1,
            )
        },
        navigationIcon = {
            IconButton(onClick = { onEvent(DayOverviewEvent.Back) }) {
                Icon(
                    painterResource(if (selecting) R.drawable.ic_close else R.drawable.ic_arrow_back),
                    contentDescription =
                        stringResource(
                            if (selecting) R.string.day_overview_clear_selection else R.string.navigate_back,
                        ),
                )
            }
        },
    )
}

/**
 * Grille des souvenirs et, s'il en reste à trier, le bouton qui ouvre la pile ; pendant une
 * sélection, les boutons « Garder » et « Supprimer » du lot (PS-159).
 */
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
                item(key = "year-${year.year}", span = { GridItemSpan(maxLineSpan) }) {
                    YearHeader(
                        day = day,
                        year = year,
                        selection =
                            if (state.selecting) {
                                YearSelection(year.memories.all { it.media.id in state.selected }) {
                                    onEvent(DayOverviewEvent.ToggleYear(year.year))
                                }
                            } else {
                                null
                            },
                    )
                }
                items(year.memories, key = { it.media.id.value }) { memory ->
                    MemoryTile(
                        memory = memory,
                        selection = TileSelection(state.selecting, memory.media.id in state.selected),
                        onOpen = { onEvent(DayOverviewEvent.Open(memory.media.id)) },
                        onToggle = { onEvent(DayOverviewEvent.ToggleSelection(memory.media.id)) },
                        qualityHint = state.quality[memory.media.id],
                    )
                }
            }
        }
        if (state.selecting) {
            BatchButtons(onEvent)
        } else if (state.remainingCount > 0) {
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

/** « Garder » et « Supprimer » : décident pour tous les souvenirs sélectionnés. */
@Composable
private fun BatchButtons(onEvent: (DayOverviewEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val colors = MaterialTheme.swipeColors
        Button(
            onClick = { onEvent(DayOverviewEvent.MarkSelected) },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.deleteContainer,
                    contentColor = colors.onDeleteContainer,
                ),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.day_overview_mark_selected), modifier = Modifier.padding(start = 8.dp))
        }
        Button(
            onClick = { onEvent(DayOverviewEvent.KeepSelected) },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.keepContainer,
                    contentColor = colors.onKeepContainer,
                ),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.day_overview_keep_selected), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * Sélection d'une année entière, pendant une sélection.
 *
 * @property all vrai si tous ses souvenirs sont sélectionnés.
 * @property toggle coche ou décoche toute l'année.
 */
private class YearSelection(
    val all: Boolean,
    val toggle: () -> Unit,
)

/**
 * « 2023 · Il y a 3 ans · 5 souvenirs », annoncé comme un titre par TalkBack. Pendant une
 * sélection ([selection]), un appui coche ou décoche toute l'année.
 */
@Composable
private fun YearHeader(
    day: LocalDate,
    year: OverviewYear,
    selection: YearSelection? = null,
) {
    val yearsAgo = day.year - year.year
    val count = year.memories.size
    Row(verticalAlignment = Alignment.CenterVertically) {
        YearTitle(year, yearsAgo, count, Modifier.weight(1f))
        if (selection != null) YearToggle(year, selection)
    }
}

/** Titre de l'année. */
@Composable
private fun YearTitle(
    year: OverviewYear,
    yearsAgo: Int,
    count: Int,
    modifier: Modifier,
) {
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
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp).semantics { heading() },
    )
}

/** « Sélectionner » ou « Désélectionner » toute l'année [year]. */
@Composable
private fun YearToggle(
    year: OverviewYear,
    selection: YearSelection,
) {
    run {
        val label =
            stringResource(
                if (selection.all) R.string.day_overview_unselect_year else R.string.day_overview_select_year,
                year.year,
            )
        TextButton(onClick = selection.toggle, modifier = Modifier.semantics { contentDescription = label }) {
            Text(stringResource(if (selection.all) R.string.day_overview_unselect else R.string.day_overview_select))
        }
    }
}

/** Repère de test de la grille. */
internal const val DAY_OVERVIEW_GRID_TAG = "day_overview_grid"

/** Préfixe du repère de test d'un souvenir, suivi de son identifiant. */
internal const val MEMORY_TILE_TAG = "day_overview_memory_"

/** Largeur minimale d'une case : 3 colonnes sur un téléphone, plus sur un grand écran. */
private val TILE_MIN_SIZE = 96.dp

/**
 * État d'une case pendant une sélection.
 *
 * @property active vrai pendant une sélection : un appui coche ou décoche.
 * @property selected vrai si la case est cochée.
 */
internal data class TileSelection(
    val active: Boolean,
    val selected: Boolean,
)
