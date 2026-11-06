package fr.babeuloula.photoswipe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.deck.memoryLabel
import java.time.ZoneId

/**
 * Volet de détail du jour, à droite de l'accueil sur grand écran ou en paysage (ADR 0025) : la date,
 * le nombre à trier et les années, « Commencer le tri » (ou « Voir le résumé » si tout est trié),
 * puis la planche-contact du jour : une grande vignette par année, sous son repère « Il y a N ans ».
 */
@Composable
internal fun TodayDetailPane(
    today: ReviewDay,
    onOpenDay: () -> Unit,
    onSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dayLabel(today.date), style = MaterialTheme.typography.headlineMedium)
                val count = pluralStringResource(R.plurals.home_remaining, today.remainingCount, today.remainingCount)
                Text(
                    text =
                        today.years?.let { stringResource(R.string.home_detail_subtitle, count, yearsLabel(it)) }
                            ?: count,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (today.remainingCount > 0) {
                Button(onClick = onOpenDay) { Text(stringResource(R.string.home_start_sorting)) }
            } else {
                FilledTonalButton(onClick = onSummary) { Text(stringResource(R.string.home_view_summary)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            today.previews.forEach { media ->
                val takenOn = media.takenAt.atZone(zone).toLocalDate()
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilmEdgeLabel(memoryLabel(context, today.date.year - takenOn.year, takenOn))
                    Thumbnail(
                        media,
                        Modifier.fillMaxWidth().height(DETAIL_THUMBNAIL_HEIGHT).clip(MaterialTheme.shapes.medium),
                    )
                }
            }
        }
    }
}

/** Hauteur des vignettes du volet de détail. */
private val DETAIL_THUMBNAIL_HEIGHT = 240.dp
