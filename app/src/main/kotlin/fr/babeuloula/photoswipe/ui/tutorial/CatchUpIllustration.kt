package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Page « rattrapage et rappel » : les 7 derniers jours, chacun avec son nombre de souvenirs à
 * trier ; les jours se trient l'un après l'autre en partant d'aujourd'hui et leur compteur devient
 * une coche. Dessous, la notification du rappel compte ce qui reste. Statique ([animate] faux) :
 * aujourd'hui et la veille triés.
 */
@Composable
internal fun CatchUpIllustration(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    var sortedDays by remember { mutableIntStateOf(if (animate) 0 else STATIC_SORTED_DAYS) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(CATCH_UP_STEP_MILLIS)
                sortedDays = if (sortedDays == SAMPLE_COUNTS.size) 0 else sortedDays + 1
            }
        }
    }
    val today = remember { LocalDate.now() }
    val locale = LocalConfiguration.current.locales[0]
    val remaining = SAMPLE_COUNTS.filterIndexed { index, _ -> !isSorted(index, sortedDays) }.sum()
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SAMPLE_COUNTS.forEachIndexed { index, count ->
                val day = today.minusDays((SAMPLE_COUNTS.lastIndex - index).toLong())
                DayTile(
                    weekday = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    count = count,
                    sorted = isSorted(index, sortedDays),
                    isToday = index == SAMPLE_COUNTS.lastIndex,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AnimatedVisibility(
            visible = remaining > 0,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ReminderBubble(remaining.coerceAtLeast(1))
        }
    }
}

/** Vrai si le jour [index] (0 = il y a 6 jours, 6 = aujourd'hui) est trié quand [sortedDays] jours le sont. */
private fun isSorted(
    index: Int,
    sortedDays: Int,
): Boolean = SAMPLE_COUNTS.lastIndex - index < sortedDays

/** Un jour : initiale du jour de la semaine, puis son compteur, une coche une fois trié, rien sans souvenir. */
@Composable
private fun DayTile(
    weekday: String,
    count: Int,
    sorted: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(if (isToday) scheme.primaryContainer else scheme.surfaceContainerHigh)
                .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(weekday, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Crossfade(targetState = sorted || count == 0, label = "dayCounter") { done ->
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                when {
                    count == 0 -> {
                        Text("–", color = scheme.outline, style = MaterialTheme.typography.labelLarge)
                    }

                    done -> {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.swipeColors.keep,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    else -> {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(scheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$count", color = scheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/** Notification du rappel d'exemple : « 7 souvenirs à trier aujourd'hui ». */
@Composable
private fun ReminderBubble(remaining: Int) {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_notification),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            pluralStringResource(R.plurals.reminder_title, remaining, remaining),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Souvenirs à trier des 7 jours d'exemple, du plus ancien à aujourd'hui (0 : aucun souvenir ce jour-là). */
@Suppress("MagicNumber") // Données d'exemple de l'illustration, sans autre sens que leur valeur.
internal val SAMPLE_COUNTS = listOf(2, 0, 5, 1, 0, 3, 4)

/** Jours triés dans la version statique : aujourd'hui et la veille. */
private const val STATIC_SORTED_DAYS = 2

/** Durée entre deux jours triés. */
internal const val CATCH_UP_STEP_MILLIS = 900L
