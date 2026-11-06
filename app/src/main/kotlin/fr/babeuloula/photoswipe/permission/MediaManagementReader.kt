package fr.babeuloula.photoswipe.permission

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
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

/** [MediaManagementReader] sur `MediaStore.canManageMedia` (Android 12+, sous notre `minSdk`). */
class AndroidMediaManagementReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MediaManagementReader {
        override fun isGranted(): Boolean = MediaStore.canManageMedia(context)
    }
