package fr.babeuloula.photoswipe.ui.tutorial

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.deck.LoadedDeck
import fr.babeuloula.photoswipe.ui.deck.MediaCardUiState
import java.time.LocalDate

/**
 * Dernière page du tutoriel : le mode entraînement (SPEC §4). Les cartes d'exemple de l'application
 * se trient avec les mêmes gestes, voiles, boutons et menu que la vraie pile (`LoadedDeck`), sans
 * jamais toucher aux photos de l'utilisateur ni enregistrer de décision ([PracticeState]).
 * Une fois les cartes triées, « Recommencer » les remet en place.
 */
@Composable
internal fun PracticePage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PracticeState(remaining = sampleCards(context))) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(TutorialPage.PRACTICE.title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(state.feedback?.text ?: TutorialPage.PRACTICE.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val deck = state.deck
            if (deck != null) {
                LoadedDeck(state = deck, onEvent = { state = state.after(it) })
            } else {
                PracticeDone(
                    onRestart = { state = PracticeState(remaining = sampleCards(context)) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** Toutes les cartes d'exemple sont triées : bravo, et « Recommencer ». */
@Composable
private fun PracticeDone(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.tutorial_practice_done),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRestart, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.tutorial_practice_restart))
        }
    }
}

/**
 * Cartes d'exemple intégrées à l'application, des illustrations vectorielles lues par leur URI de
 * ressource (`android.resource://`) : jamais un média de la galerie.
 */
internal fun sampleCards(
    context: Context,
    today: LocalDate = LocalDate.now(),
): List<MediaCardUiState> =
    SAMPLES.mapIndexed { index, (drawable, yearsAgo) ->
        MediaCardUiState(
            id = MediaId(index + 1L),
            uri = "android.resource://${context.packageName}/$drawable",
            type = MediaType.IMAGE,
            takenOn = today.minusYears(yearsAgo.toLong()),
            yearsAgo = yearsAgo,
            sizeBytes = SAMPLE_SIZE_BYTES,
        )
    }

/** Illustrations des cartes d'exemple et leur ancienneté en années. */
@Suppress("MagicNumber") // Données d'exemple, sans autre sens que leur valeur.
private val SAMPLES =
    listOf(
        R.drawable.tutorial_sample_mountains to 2,
        R.drawable.tutorial_sample_sea to 4,
        R.drawable.tutorial_sample_flowers to 7,
    )

/** Poids affiché sur les cartes d'exemple. */
private const val SAMPLE_SIZE_BYTES = 2_400_000L
