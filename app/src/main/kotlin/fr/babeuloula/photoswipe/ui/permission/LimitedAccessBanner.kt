package fr.babeuloula.photoswipe.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Bannière de l'accès partiel (Android 14+, SPEC §4.1) : les souvenirs peuvent être incomplets, et
 * [onManageAccess] relance la demande système pour étendre la sélection ou tout autoriser.
 */
@Composable
fun LimitedAccessBanner(
    onManageAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Carte arrondie et calme (ADR 0025), en tête de tous les écrans tant que l'accès est partiel.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.limited_access_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onManageAccess) {
                Text(stringResource(R.string.limited_access_manage))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LimitedAccessBannerPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        LimitedAccessBanner(onManageAccess = {})
    }
}
