package fr.babeuloula.photoswipe.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R

/**
 * Bannières en tête de l'accueil : le conseil « Gestion des médias », puis les suppressions en
 * attente, chacun seulement s'il a lieu d'être.
 */
@Composable
internal fun HomeBanners(
    state: HomeUiState.Loaded,
    onEvent: (HomeEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.showManageMediaTip) {
            ManageMediaTipCard(
                onOpenSettings = { onEvent(HomeEvent.OpenManageMediaSettings) },
                onDismiss = { onEvent(HomeEvent.DismissManageMediaTip) },
            )
        }
        if (state.pendingDeletionCount > 0) {
            PendingDeletionsBanner(state.pendingDeletionCount, onClick = { onEvent(HomeEvent.OpenPendingDeletions) })
        }
    }
}

/**
 * Bannière des suppressions en attente (SPEC §3.4) : des médias sont marqués mais pas encore
 * envoyés à la corbeille. Un tap ouvre le résumé pour les confirmer.
 */
@Composable
private fun PendingDeletionsBanner(
    count: Int,
    onClick: () -> Unit,
) {
    // Calme (ADR 0025) : une surface, pas un conteneur coloré, pour ne pas écraser la carte du jour.
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(R.plurals.home_pending_deletions, count, count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) { Text(stringResource(R.string.home_pending_deletions_review)) }
        }
    }
}

/**
 * Conseil de l'accueil (SPEC §3.5, ADR 0023) : après un premier dialogue d'Android validé, il
 * propose l'accès « Gestion des médias ». Le mot « paramètres » ouvre le réglage d'Android ; la
 * croix ferme le conseil pour de bon.
 */
@Composable
internal fun ManageMediaTipCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val link = stringResource(R.string.home_manage_media_tip_link)
    val message = stringResource(R.string.home_manage_media_tip, link)
    val linkColor = MaterialTheme.colorScheme.primary
    val text =
        buildAnnotatedString {
            val start = message.indexOf(link)
            append(message.substring(0, start))
            withLink(
                LinkAnnotation.Clickable(
                    tag = "manage_media_settings",
                    styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ) { onOpenSettings() },
            ) { append(link) }
            append(message.substring(start + link.length))
        }
    // Simple contour (ADR 0025) : le conseil reste en retrait de la carte du jour et de la bannière.
    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_lightbulb),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.home_manage_media_tip_dismiss),
                )
            }
        }
    }
}
