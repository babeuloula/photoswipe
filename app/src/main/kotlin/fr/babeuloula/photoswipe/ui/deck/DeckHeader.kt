package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Progression dans la pile du jour, sous la barre du haut : une barre et « 4 sur 23 » en chiffres
 * tabulaires. TalkBack la lit comme une barre de progression.
 */
@Composable
internal fun DeckProgress(
    position: Int,
    total: Int,
) {
    val label = stringResource(R.string.deck_progress, position, total)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else (position - 1).toFloat() / total },
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.monoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Jour et mois de [date] dans la langue de [context], sans l'année : « 6 novembre », « November 6 ». */
internal fun dayAndMonth(
    context: Context,
    date: LocalDate,
): String {
    val locale = context.resources.configuration.locales[0]
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale))
}
