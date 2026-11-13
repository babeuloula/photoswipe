package fr.babeuloula.photoswipe.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.settings.SectionTitle
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Section « Calendrier » de la page « Progression » (PS-157) : l'année de revue mois par mois, en
 * carte de chaleur. Chaque jour qui a des souvenirs porte son nombre à trier (fond `primaryContainer`,
 * `primary` à partir de [MANY_TO_SORT]) ou une coche s'il est tout trié : l'état ne repose jamais
 * sur la seule couleur. Un appui ouvre la vue d'ensemble du jour, y compris un jour à venir, que
 * l'on peut trier à l'avance. Aujourd'hui est entouré, et le calendrier s'ouvre sur son mois.
 * Tant que la galerie est lue, un indicateur de chargement tient la place.
 */
@Composable
internal fun CalendarSection(
    calendar: YearMemories?,
    today: LocalDate?,
    onOpenDay: (LocalDate) -> Unit,
) {
    // Moins de marge que les autres sections : sept cases de 48 dp tiennent dès 360 dp de large.
    Column {
        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle(stringResource(R.string.progress_calendar_section)) }
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .clip(segmentShape(0, 1))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 8.dp),
        ) {
            if (calendar == null) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
            } else {
                val firstMonth = if (today?.year == calendar.year) today.monthValue else 1
                var monthValue by rememberSaveable(calendar.year) { mutableIntStateOf(firstMonth) }
                val month = YearMonth.of(calendar.year, monthValue)
                MonthHeader(
                    month = month,
                    onPrevious = if (monthValue > 1) ({ monthValue -= 1 }) else null,
                    onNext = if (monthValue < MONTHS_IN_YEAR) ({ monthValue += 1 }) else null,
                )
                MonthGrid(month, calendar, today, onOpenDay)
                CalendarLegend()
                Text(
                    stringResource(R.string.progress_calendar_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Nom du mois et flèches vers le précédent et le suivant ; absentes aux bords de l'année. */
@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        MonthArrow(R.drawable.ic_chevron_left, R.string.progress_calendar_previous, onPrevious)
        Text(
            text =
                month
                    .format(
                        DateTimeFormatter.ofPattern("LLLL yyyy", locale),
                    ).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        MonthArrow(R.drawable.ic_chevron_right, R.string.progress_calendar_next, onNext)
    }
}

/** Flèche de changement de mois, remplacée par un vide de même taille quand il n'y a rien au-delà. */
@Composable
private fun MonthArrow(
    icon: Int,
    label: Int,
    onClick: (() -> Unit)?,
) {
    if (onClick == null) {
        Spacer(Modifier.size(48.dp))
    } else {
        IconButton(onClick = onClick) { Icon(painterResource(icon), contentDescription = stringResource(label)) }
    }
}

/** Jours du mois en semaines, à partir du premier jour de la semaine de la langue. */
@Composable
private fun MonthGrid(
    month: YearMonth,
    calendar: YearMemories,
    today: LocalDate?,
    onOpenDay: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val offset = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column {
        Row(Modifier.clearAndSetSemantics {}) {
            for (index in 0 until DAYS_IN_WEEK) {
                Text(
                    text = firstDayOfWeek.plus(index.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row {
                for (index in 0 until DAYS_IN_WEEK) {
                    val day = week.getOrNull(index)
                    Box(Modifier.weight(1f)) {
                        if (day != null) DayCell(day, calendar[day], day == today, locale, onOpenDay)
                    }
                }
            }
        }
    }
}

/** Case d'un jour : son numéro, puis son nombre à trier ou une coche ; touchable s'il a des souvenirs. */
@Composable
private fun DayCell(
    day: LocalDate,
    memories: DayMemories?,
    isToday: Boolean,
    locale: Locale,
    onOpenDay: (LocalDate) -> Unit,
) {
    val date = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    val dated = if (isToday) stringResource(R.string.progress_calendar_today, date) else date
    val description =
        when {
            memories == null -> {
                stringResource(R.string.progress_calendar_day_empty, dated)
            }

            memories.isSorted -> {
                stringResource(R.string.progress_calendar_day_sorted, dated)
            }

            else -> {
                pluralStringResource(
                    R.plurals.progress_calendar_day_remaining,
                    memories.remainingCount,
                    dated,
                    memories.remainingCount,
                )
            }
        }
    val (background, content) = dayColors(memories)
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                // Toute la case est touchable ; la couleur s'en écarte de 2 dp pour séparer les jours.
                .then(if (memories != null) Modifier.clickable(role = Role.Button) { onOpenDay(day) } else Modifier)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (memories != null) role = Role.Button
                }.padding(2.dp)
                .clip(shape)
                .background(background)
                .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                .padding(vertical = 4.dp),
    ) {
        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = content)
        when {
            memories == null -> {
                Unit
            }

            memories.isSorted -> {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
            }

            else -> {
                Text(memories.remainingCount.toString(), style = MaterialTheme.typography.labelSmall, color = content)
            }
        }
    }
}

/** Couleurs de fond et de texte d'un jour, selon ce qu'il reste à trier. */
@Composable
private fun dayColors(memories: DayMemories?): Pair<Color, Color> =
    when {
        memories == null -> {
            Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
        }

        memories.isSorted -> {
            MaterialTheme.swipeColors.keepContainer to MaterialTheme.swipeColors.onKeepContainer
        }

        memories.remainingCount >= MANY_TO_SORT -> {
            MaterialTheme.colorScheme.primary to
                MaterialTheme.colorScheme.onPrimary
        }

        else -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
    }

/** Légende des trois états colorés ; décorative pour TalkBack, qui lit l'état de chaque jour. */
@Composable
private fun CalendarLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clearAndSetSemantics {},
    ) {
        LegendItem(MaterialTheme.colorScheme.primaryContainer, stringResource(R.string.progress_calendar_to_sort))
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.progress_calendar_many_to_sort))
        LegendItem(MaterialTheme.swipeColors.keepContainer, stringResource(R.string.progress_calendar_sorted))
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** À partir de ce nombre de médias à trier, un jour prend la teinte la plus forte. */
private const val MANY_TO_SORT = 10
private const val DAYS_IN_WEEK = 7
private const val MONTHS_IN_YEAR = 12
