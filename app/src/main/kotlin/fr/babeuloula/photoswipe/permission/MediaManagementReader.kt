package fr.babeuloula.photoswipe.permission

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import javax.inject.Inject

/**
 * Dit si l'utilisateur a donné à PhotoSwipe l'accès « Gestion des médias » (`MANAGE_MEDIA`) :
 * avec lui, Android applique les demandes de corbeille et de favori sans afficher son dialogue
 * (SPEC §3.5, ADR 0023).
 */
fun interface MediaManagementReader {
    /** Vrai si l'accès est donné à cet instant ; il change depuis les réglages d'Android. */
    fun isGranted(): Boolean
}

/**
 * [MediaManagementReader] sur `MediaStore.canManageMedia` ; jamais accordé avant Android 12, où
 * l'accès n'existe pas (PS-181).
 */
class AndroidMediaManagementReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val features: PlatformFeatures,
    ) : MediaManagementReader {
        @SuppressLint("NewApi") // Appelé seulement quand l'accès existe (Android 12+).
        override fun isGranted(): Boolean = features.mediaManagement && MediaStore.canManageMedia(context)
    }
