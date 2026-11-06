package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.deck.memoryLabel
import fr.babeuloula.photoswipe.ui.theme.motion
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Page « souvenirs du même jour » : des tirages de plusieurs années défilent l'un après l'autre,
 * chacun avec son repère « Il y a 1 an · … », « Il y a 3 ans · … », à la date d'aujourd'hui.
 * Statique ([animate] faux) : le tirage d'il y a 3 ans, devant les autres. Avec une très grande
 * police, le repère ne garde que l'ancienneté, pour tenir sans être coupé.
 */
@Composable
internal fun SameDayIllustration(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    var index by remember { mutableIntStateOf(if (animate) 0 else STATIC_INDEX) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(SAME_DAY_STEP_MILLIS)
                index = (index + 1) % SAME_DAY_YEARS.size
            }
        }
    }
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer)
    val motion = MaterialTheme.motion
    // Avec une très grande police, le repère complet ne tiendrait pas : l'ancienneté seule.
    val withDate = LocalDensity.current.fontScale < LARGE_FONT_SCALE
    Box(modifier, contentAlignment = Alignment.Center) {
        // Les tirages des autres années, inclinés derrière celui de l'année affichée.
        SamplePrint(
            scheme.surfaceContainerHighest,
            Modifier.offset(x = (-44).dp, y = (-12).dp).rotate(-BACK_TILT),
            showIcon = false,
        )
        SamplePrint(
            scheme.surfaceContainerHigh,
            Modifier.offset(x = 44.dp, y = (-16).dp).rotate(BACK_TILT),
            showIcon = false,
        )
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInHorizontally(motion.defaultSpatial()) { it } + fadeIn(motion.defaultEffects()))
                    .togetherWith(
                        slideOutHorizontally(motion.defaultSpatial()) { -it } + fadeOut(motion.defaultEffects()),
                    ).using(SizeTransform(clip = false))
            },
            label = "sameDayPrint",
        ) { shown ->
            val yearsAgo = SAME_DAY_YEARS[shown]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SamplePrint(colors[shown % colors.size], iconTint = scheme.onPrimaryContainer)
                val label =
                    if (withDate) {
                        memoryLabel(context, yearsAgo, today.minusYears(yearsAgo.toLong()))
                    } else {
                        context.resources.getQuantityString(R.plurals.deck_years_ago, yearsAgo, yearsAgo)
                    }
                FilmEdgeLabel(label)
            }
        }
    }
}

/** Années d'exemple qui défilent, en années avant aujourd'hui. */
@Suppress("MagicNumber") // Données d'exemple de l'illustration, sans autre sens que leur valeur.
internal val SAME_DAY_YEARS = listOf(1, 3, 5, 8)

/** Année montrée par la version statique : il y a 3 ans. */
internal const val STATIC_INDEX = 1

/** Durée d'affichage d'un tirage avant le suivant. */
internal const val SAME_DAY_STEP_MILLIS = 1_800L

/** Échelle de police à partir de laquelle le repère perd sa date. */
private const val LARGE_FONT_SCALE = 1.5f

/** Inclinaison des tirages de derrière, en degrés. */
private const val BACK_TILT = 8f
