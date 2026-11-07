package fr.babeuloula.photoswipe.permission

import android.app.AlarmManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Dit si PhotoSwipe peut programmer des alarmes exactes (« Alarmes et rappels », Android 14+) :
 * avec elles, le rappel arrive à la minute choisie (PS-125, ADR 0031).
 */
fun interface ExactAlarmPermissionReader {
    /** Vrai si l'autorisation est donnée à cet instant ; elle change depuis les réglages d'Android. */
    fun isGranted(): Boolean
}

/** [ExactAlarmPermissionReader] sur `AlarmManager.canScheduleExactAlarms`. */
class AndroidExactAlarmPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ExactAlarmPermissionReader {
        override fun isGranted(): Boolean = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
