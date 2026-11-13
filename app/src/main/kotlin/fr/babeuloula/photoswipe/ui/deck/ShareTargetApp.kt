package fr.babeuloula.photoswipe.ui.deck

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
 * Application à qui renvoyer un média (PS-163).
 *
 * @property label nom de l'application, tel qu'Android l'affiche.
 * @property icon son icône.
 */
internal class ShareTargetApp(
    val label: String,
    val icon: ImageBitmap,
)

/**
 * Nom et icône de l'application [component] (`paquet/classe`), lus dans le gestionnaire de
 * paquets ; `null` si elle a été désinstallée ou ne reçoit plus de partage.
 */
@Composable
internal fun rememberShareTargetApp(component: String?): ShareTargetApp? {
    val packageManager = LocalContext.current.packageManager
    return remember(component, packageManager) {
        val name = component?.let(ComponentName::unflattenFromString) ?: return@remember null
        try {
            val activity = packageManager.getActivityInfo(name, 0)
            ShareTargetApp(
                label = activity.loadLabel(packageManager).toString(),
                icon = activity.loadIcon(packageManager).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap(),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/** Taille de l'icône lue, en pixels : assez pour 24 dp sur les écrans les plus denses. */
private const val ICON_SIZE_PX = 96
