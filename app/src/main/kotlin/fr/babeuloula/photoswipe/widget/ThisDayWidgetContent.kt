package fr.babeuloula.photoswipe.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.TodayWidget
import fr.babeuloula.photoswipe.tile.SortTodayTileService.Companion.todayDeckIntent
import fr.babeuloula.photoswipe.ui.theme.LightColorScheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Arrondi du widget, celui des widgets d'Android 12+. */
private val WidgetCorner: Dp = 24.dp

/** Marge intérieure du widget. */
private val WidgetPadding: Dp = 12.dp

/** Repère posé sur la photo : rôles fixes, identiques en clair et en sombre (ADR 0025). */
private val OnPhotoContainer = ColorProvider(LightColorScheme.primaryFixed)
private val OnPhoto = ColorProvider(LightColorScheme.onPrimaryFixed)

/**
 * Contenu du widget « Ce jour-là » (PS-170) : les souvenirs du jour à trier, avec ou sans photo,
 * ou l'état qui en tient lieu. Un appui ouvre la pile du jour s'il reste à trier (lien
 * `photoswipe://today`, ADR 0038), sinon l'accueil.
 */
@Composable
fun ThisDayWidgetContent(data: ThisDayWidgetData) {
    val context = LocalContext.current
    val state = data.state
    val open =
        if (state is TodayWidget.ToSort) {
            // Lien profond `photoswipe://today` (PS-171) : le jour est relu à l'ouverture.
            todayDeckIntent(context)
        } else {
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val photo = data.photo
    val background =
        if (state is TodayWidget.ToSort &&
            photo == null
        ) {
            GlanceTheme.colors.primaryContainer
        } else {
            GlanceTheme.colors.surface
        }
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(WidgetCorner)
                .background(background)
                .clickable(actionStartActivity(open)),
    ) {
        when (state) {
            is TodayWidget.ToSort -> {
                if (photo != null) ToSortWithPhoto(state, photo) else ToSortText(state)
            }

            is TodayWidget.AllSorted -> {
                Message(
                    R.drawable.ic_check_circle_filled,
                    context.getString(R.string.widget_all_sorted),
                    state.date,
                )
            }

            is TodayWidget.NoMemories -> {
                Message(
                    R.drawable.ic_calendar_today,
                    context.getString(R.string.widget_no_memories),
                    state.date,
                )
            }

            TodayWidget.NoAccess -> {
                Message(
                    R.drawable.ic_photo_library,
                    context.getString(R.string.widget_no_access),
                    date = null,
                )
            }
        }
    }
}

/** Le widget est-il assez large pour la mise en page moyenne (photo à gauche, texte à droite) ? */
@Composable
private fun isMedium(): Boolean = LocalSize.current.width >= ThisDayWidget.MEDIUM.width

/** Souvenirs à trier avec leur photo : posée en fond (petit) ou à gauche du texte (moyen). */
@Composable
private fun ToSortWithPhoto(
    state: TodayWidget.ToSort,
    photo: Bitmap,
) {
    val context = LocalContext.current
    val image = ImageProvider(photo)
    val description = context.getString(R.string.widget_photo_description)
    if (isMedium()) {
        val side = LocalSize.current.height - WidgetPadding * 2
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(WidgetPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = image,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.size(side).cornerRadius(16.dp),
            )
            Spacer(GlanceModifier.width(WidgetPadding))
            ToSortLines(state, modifier = GlanceModifier.defaultWeight())
        }
    } else {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            Image(
                provider = image,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Column(modifier = GlanceModifier.padding(10.dp)) {
                OnPhotoChip(formatDate(context, state.date, long = false))
                Spacer(GlanceModifier.height(4.dp))
                OnPhotoChip(toSort(context, state.remainingCount), bold = true)
            }
        }
    }
}

/** Repère posé sur la photo, sur les rôles fixes du thème. */
@Composable
private fun OnPhotoChip(
    text: String,
    bold: Boolean = false,
) {
    Text(
        text = text,
        maxLines = 1,
        style =
            TextStyle(
                color = OnPhoto,
                fontSize = 12.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            ),
        modifier =
            GlanceModifier
                .background(OnPhotoContainer)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Souvenirs à trier sans photo, sur le conteneur ambre du thème. */
@Composable
private fun ToSortText(state: TodayWidget.ToSort) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(WidgetPadding + 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        ToSortLines(state, onContainer = true)
    }
}

/** Lignes du texte : « Ce jour-là », la date, « 23 à trier » et les années (moyen seulement). */
@Composable
private fun ToSortLines(
    state: TodayWidget.ToSort,
    modifier: GlanceModifier = GlanceModifier,
    onContainer: Boolean = false,
) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val accent = if (onContainer) colors.onPrimaryContainer else colors.primary
    val text = if (onContainer) colors.onPrimaryContainer else colors.onSurface
    val medium = isMedium()
    Column(modifier = modifier) {
        Text(
            text = context.getString(R.string.widget_name),
            maxLines = 1,
            style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Text(
            text = formatDate(context, state.date, long = medium),
            maxLines = 1,
            style = TextStyle(color = text, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = toSort(context, state.remainingCount),
            maxLines = 1,
            style = TextStyle(color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
        val years = state.years
        if (medium && years != null && years.first != years.last) {
            Text(
                text = context.getString(R.string.widget_years, years.first, years.last),
                maxLines = 1,
                style = TextStyle(color = text, fontSize = 12.sp),
            )
        }
    }
}

/** État sans souvenir à trier : une icône, un message et, s'il y a lieu, la date. */
@Composable
private fun Message(
    @DrawableRes icon: Int,
    message: String,
    date: LocalDate?,
) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(WidgetPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.primary),
            modifier = GlanceModifier.size(28.dp),
        )
        Spacer(GlanceModifier.height(6.dp))
        if (date != null) {
            Text(
                text = formatDate(context, date, long = isMedium()),
                maxLines = 1,
                style = TextStyle(color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
        }
        Text(
            text = message,
            maxLines = 3,
            style =
                TextStyle(
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

/** « 23 à trier ». */
private fun toSort(
    context: Context,
    count: Int,
): String = context.resources.getQuantityString(R.plurals.widget_to_sort, count, count)

/** Date sans l'année : « 13 novembre », ou abrégée, « 13 nov. » ([long] faux). */
private fun formatDate(
    context: Context,
    date: LocalDate,
    long: Boolean,
): String {
    val locale = context.resources.configuration.locales[0]
    val skeleton = if (long) "dMMMM" else "dMMM"
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}
