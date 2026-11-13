package fr.babeuloula.photoswipe.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.VisibleForTesting
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.link.DeepLinks

/**
 * Tuile « PhotoSwipe » des réglages rapides (PS-172, ADR 0038) : un appui ferme le volet et ouvre
 * la pile du jour par le lien `photoswipe://today`, ou l'accueil s'il n'y reste rien à trier.
 * Téléphone verrouillé, Android demande d'abord le déverrouillage (`unlockAndRun`).
 *
 * La tuile n'affiche aucun compteur : elle ne lit ni la galerie ni la base, son sous-titre est fixe.
 */
open class SortTodayTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            // Une action, pas un interrupteur : la tuile reste inactive.
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.tile_subtitle)
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openTodayDeck) else openTodayDeck()
    }

    private fun openTodayDeck() {
        collapseAndStart(todayDeckIntent(this))
    }

    /**
     * Ferme le volet et lance [intent]. Séparé pour les tests : Robolectric ne relie pas la tuile au
     * service système qui lance l'activité. La version dépréciée à `Intent` ne sert que sous
     * Android 14, où elle ne lève pas d'exception (d'où la suppression de l'alerte de Lint).
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @VisibleForTesting
    internal open fun collapseAndStart(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pending)
        } else {
            // Android 13 : seule la version à Intent existe.
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Intent qui ouvre la pile du jour par son lien, dans la tâche de l'application. */
        fun todayDeckIntent(context: Context): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.TODAY))
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
