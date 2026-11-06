package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R

/**
 * Illustration d'une page du tutoriel, faite de formes vectorielles et des couleurs du thème, sans
 * aucune vraie photo (SPEC §4).
 *
 * @param animate faux pour la version statique (réglage système de suppression des animations).
 */
@Composable
internal fun TutorialIllustration(
    page: TutorialPage,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    when (page) {
        TutorialPage.SAME_DAY -> {
            SameDayIllustration(animate = animate, modifier = modifier)
        }

        TutorialPage.SWIPE_UP -> {
            SwipeUpIllustration(animate = animate, modifier = modifier)
        }

        TutorialPage.CATCH_UP -> {
            CatchUpIllustration(animate = animate, modifier = modifier)
        }

        else -> {
            Box(modifier, contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(page.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}

/**
 * Tirage d'exemple : carte arrondie et bordée, de la couleur [color], avec l'icône d'une image
 * ([showIcon] faux pour un tirage de derrière).
 * Il remplace les photos dans les illustrations du tutoriel.
 */
@Composable
internal fun SamplePrint(
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = PRINT_WIDTH,
    height: Dp = PRINT_HEIGHT,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showIcon: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .size(width = width, height = height)
                .clip(MaterialTheme.shapes.large)
                .background(color)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        if (showIcon) {
            Icon(
                painterResource(R.drawable.ic_photo_library),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** Largeur d'un tirage d'exemple. */
internal val PRINT_WIDTH = 112.dp

/** Hauteur d'un tirage d'exemple, au format d'une photo en portrait. */
internal val PRINT_HEIGHT = 148.dp
