package fr.babeuloula.photoswipe.ui.tutorial

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.theme.motion
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.delay

/**
 * Page « favori ou partage » : devant deux tirages inclinés (ceux des pages précédentes), le tirage glisse vers le haut sous le voile bleu du favori, puis la
 * feuille « Ajouter aux favoris » / « Partager » de la pile monte du bas. Statique ([animate] faux) :
 * tirage relevé et feuille ouverte. Sans favoris de la galerie (Android 10), voile neutre et
 * feuille « Partager » / « Ouvrir avec » (PS-183).
 */
@Composable
internal fun SwipeUpIllustration(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val lift = remember { Animatable(if (animate) 0f else 1f) }
    val sheet = remember { Animatable(if (animate) 0f else 1f) }
    val motion = MaterialTheme.motion
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(PAUSE_MILLIS)
                lift.animateTo(1f, tween(LIFT_MILLIS))
                sheet.animateTo(1f, motion.defaultSpatial())
                delay(HOLD_MILLIS)
                sheet.animateTo(0f, motion.fastExit())
                lift.animateTo(0f, motion.defaultSpatial())
            }
        }
    }
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.TopCenter) {
        val liftPx = with(LocalDensity.current) { LIFT_DISTANCE.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        // Les tirages de derrière, comme aux pages précédentes : ils restent en place quand la carte monte.
        val scheme = MaterialTheme.colorScheme
        Box(Modifier.padding(top = PRINT_TOP), contentAlignment = Alignment.Center) {
            SamplePrint(
                scheme.surfaceContainerHighest,
                Modifier.offset(x = (-44).dp, y = 12.dp).rotate(-BACK_TILT),
                showIcon = false,
            )
            SamplePrint(
                scheme.surfaceContainerHigh,
                Modifier.offset(x = 44.dp, y = 8.dp).rotate(BACK_TILT),
                showIcon = false,
            )
        }
        val favorites = LocalPlatformFeatures.current.galleryFavorites
        LiftedPrint(lift = { lift.value }, liftPx = liftPx, favorites = favorites)
        // La feuille n'existe qu'une fois sortie : fermée, elle n'est ni dessinée ni lue.
        if (sheet.value > 0f) {
            SampleMenuSheet(
                favorites = favorites,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { translationY = heightPx * (1f - sheet.value) },
            )
        }
    }
}

/**
 * Tirage relevé de [lift] (0 à 1) fois [liftPx], sous le voile bleu et le cœur du favori, ou sous un
 * voile neutre et « Plus d'actions » sans [favorites].
 */
@Composable
private fun LiftedPrint(
    lift: () -> Float,
    liftPx: Float,
    favorites: Boolean,
) {
    val colors = MaterialTheme.swipeColors
    val scheme = MaterialTheme.colorScheme
    val veil = if (favorites) colors.favorite else scheme.inverseSurface
    val container = if (favorites) colors.favoriteContainer else scheme.secondaryContainer
    val onContainer = if (favorites) colors.onFavoriteContainer else scheme.onSecondaryContainer
    Box(
        modifier =
            Modifier
                .padding(top = PRINT_TOP)
                .graphicsLayer { translationY = -liftPx * lift() },
        contentAlignment = Alignment.Center,
    ) {
        SamplePrint(MaterialTheme.colorScheme.primaryContainer, iconTint = MaterialTheme.colorScheme.onPrimaryContainer)
        Box(
            Modifier
                .size(PRINT_WIDTH, PRINT_HEIGHT)
                .graphicsLayer { alpha = lift() * VEIL_ALPHA }
                .clip(MaterialTheme.shapes.large)
                .background(veil),
        )
        Box(
            modifier =
                Modifier
                    .graphicsLayer { alpha = lift() }
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (favorites) R.drawable.ic_favorite_filled else R.drawable.ic_more_horiz),
                null,
                tint = onContainer,
            )
        }
    }
}

/** Feuille « Plus d'actions » d'exemple, comme celle de la pile : favori et partage, ou partage et « Ouvrir avec ». */
@Composable
private fun SampleMenuSheet(
    favorites: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        if (favorites) SheetRow(R.drawable.ic_favorite, stringResource(R.string.deck_favorite))
        SheetRow(R.drawable.ic_share, stringResource(R.string.deck_share))
        if (!favorites) SheetRow(R.drawable.ic_open_in_new, stringResource(R.string.deck_open_with))
    }
}

/** Ligne de la feuille d'exemple : icône et libellé de l'action. */
@Composable
private fun SheetRow(
    @DrawableRes icon: Int,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Marge au-dessus du tirage. */
private val PRINT_TOP = 32.dp

/** Inclinaison des tirages de derrière, en degrés, comme aux pages précédentes. */
private const val BACK_TILT = 8f

/** Distance dont le tirage monte. */
private val LIFT_DISTANCE = 24.dp

/** Opacité du voile bleu, tirage relevé. */
private const val VEIL_ALPHA = 0.45f

/** Pause avant chaque glissement. */
private const val PAUSE_MILLIS = 700L

/** Durée du glissement vers le haut. */
private const val LIFT_MILLIS = 600

/** Temps pendant lequel la feuille reste ouverte. */
internal const val HOLD_MILLIS = 1_800L
