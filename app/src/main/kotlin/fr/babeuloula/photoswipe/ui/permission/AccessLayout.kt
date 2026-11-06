package fr.babeuloula.photoswipe.ui.permission

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.deck.memoryLabel
import java.time.LocalDate

/**
 * Mise en page des écrans d'accès aux photos (ADR 0025) : une pile de tirages en tête, le titre et
 * son contenu, puis les actions en bas. Défilable pour une grande police.
 */
@Composable
internal fun AccessLayout(
    title: String,
    blankPrints: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PrintStack(blank = blankPrints, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                content()
            }
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = actions,
            )
        }
    }
}

/**
 * Pile de tirages, l'illustration de l'accès aux photos : deux tirages inclinés derrière un troisième,
 * faits des formes et des couleurs du thème (les photos des maquettes ne sont pas dans l'APK), et
 * dessous le repère « Il y a 3 ans · … ». [blank] : sans accès, des tirages vides et une image barrée.
 * Décorative : TalkBack ne la lit pas.
 */
@Composable
private fun PrintStack(
    blank: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.height(PRINT_HEIGHT + 24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Print(
                Modifier.offset(x = (-56).dp, y = 12.dp).rotate(-BACK_PRINT_TILT),
                if (blank) scheme.surfaceContainer else scheme.secondaryContainer,
            )
            Print(
                Modifier.offset(x = 56.dp, y = 8.dp).rotate(SIDE_PRINT_TILT),
                if (blank) scheme.surfaceContainer else scheme.tertiaryContainer,
            )
            Print(
                modifier = Modifier,
                color = if (blank) scheme.surfaceContainerHighest else scheme.primaryContainer,
                icon = if (blank) R.drawable.ic_hide_image else R.drawable.ic_photo_library,
                iconTint = if (blank) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
            )
        }
        if (!blank) {
            val today = LocalDate.now()
            FilmEdgeLabel(memoryLabel(LocalContext.current, YEARS_AGO, today.minusYears(YEARS_AGO.toLong())))
        }
    }
}

/** Un tirage : carte à coins de 16 dp, bordée, avec une icône s'il est au premier plan. */
@Composable
private fun Print(
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color,
    @DrawableRes icon: Int? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier =
            modifier
                .size(width = PRINT_WIDTH, height = PRINT_HEIGHT)
                .clip(MaterialTheme.shapes.large)
                .background(color)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        if (icon !=
            null
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(48.dp))
        }
    }
}

/** Ligne d'explication : une icône, un titre et son texte. */
@Composable
internal fun AccessPoint(
    @DrawableRes icon: Int,
    title: String,
    body: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Largeur d'un tirage. */
private val PRINT_WIDTH = 156.dp

/** Hauteur d'un tirage, au format d'une photo en portrait. */
private val PRINT_HEIGHT = 208.dp

/** Ancienneté du souvenir du repère d'exemple. */
private const val YEARS_AGO = 3

/** Inclinaison du tirage de gauche, en degrés. */
private const val BACK_PRINT_TILT = 9f

/** Inclinaison du tirage de droite, en degrés. */
private const val SIDE_PRINT_TILT = 7f
