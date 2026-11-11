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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Page « rattrapage et rappel » : la liste des jours telle que l'accueil la montre, à la verticale
 * (PS-137) : aujourd'hui en haut, puis les 6 jours précédents, chacun avec son nombre de souvenirs
 * à trier. Les jours se trient l'un après l'autre en partant d'aujourd'hui et leur compteur devient
 * une coche ; la notification du rappel, posée en haut comme une vraie, compte ce qui reste.
 * Statique ([animate] faux) : aujourd'hui et la veille triés.
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
    val dayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE d", locale) }
    val remaining = SAMPLE_COUNTS.filterIndexed { index, _ -> !isSorted(index, sortedDays) }.sum()
    Box(modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            // Du plus récent (aujourd'hui, en haut) au plus ancien, comme sur l'accueil.
            SAMPLE_COUNTS.indices.reversed().forEachIndexed { row, index ->
                val day = today.minusDays((SAMPLE_COUNTS.lastIndex - index).toLong())
                val isToday = index == SAMPLE_COUNTS.lastIndex
                DayRow(
                    label = if (isToday) stringResource(R.string.home_today) else day.format(dayFormat),
                    count = SAMPLE_COUNTS[index],
                    sorted = isSorted(index, sortedDays),
                    isToday = isToday,
                    shape =
                        if (isToday) {
                            MaterialTheme.shapes.medium
                        } else {
                            segmentShape(
                                row - 1,
                                SAMPLE_COUNTS.lastIndex,
                            )
                        },
                    modifier = Modifier.weight(1f).padding(bottom = if (isToday) 6.dp else 0.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = remaining > 0,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
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

/**
 * Une ligne de la liste des jours : son nom (« Aujourd'hui », « mar. 10 »), puis son compteur, une
 * coche une fois trié, un tiret sans souvenir.
 */
@Composable
private fun DayRow(
    label: String,
    count: Int,
    sorted: Boolean,
    isToday: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isToday) scheme.primaryContainer else scheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Crossfade(targetState = sorted || count == 0, label = "dayCounter") { done ->
            Box(Modifier.defaultMinSize(COUNTER_SIZE, COUNTER_SIZE), contentAlignment = Alignment.Center) {
                when {
                    count == 0 -> {
                        Text("–", color = scheme.outline, style = MaterialTheme.typography.labelMedium)
                    }

                    done -> {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.swipeColors.keep,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    else -> {
                        Box(
                            Modifier
                                .defaultMinSize(COUNTER_SIZE, COUNTER_SIZE)
                                .clip(CircleShape)
                                .background(scheme.primary)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$count", color = scheme.onPrimary, style = MaterialTheme.typography.labelMedium)
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
        )
    }
}

/** Souvenirs à trier des 7 jours d'exemple, du plus ancien à aujourd'hui (0 : aucun souvenir ce jour-là). */
@Suppress("MagicNumber") // Données d'exemple de l'illustration, sans autre sens que leur valeur.
internal val SAMPLE_COUNTS = listOf(2, 0, 5, 1, 0, 3, 4)

/** Jours triés dans la version statique : aujourd'hui et la veille. */
private const val STATIC_SORTED_DAYS = 2

/** Taille minimale d'un compteur : elle grandit avec la police. */
private val COUNTER_SIZE = 20.dp

/** Durée entre deux jours triés. */
internal const val CATCH_UP_STEP_MILLIS = 900L
