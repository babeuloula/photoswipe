package fr.babeuloula.photoswipe.permission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import javax.inject.Inject

/**
 * Dit si PhotoSwipe peut programmer des alarmes exactes (« Alarmes et rappels » à partir d'Android 12,
 * permises d'office avant) : avec elles, le rappel arrive à la minute choisie (PS-125, ADR 0031).
 */
fun interface ExactAlarmPermissionReader {
    /** Vrai si l'autorisation est donnée à cet instant ; elle change depuis les réglages d'Android. */
    fun isGranted(): Boolean
}

/**
 * [ExactAlarmPermissionReader] sur `AlarmManager.canScheduleExactAlarms` à partir d'Android 12 ;
 * avant, les alarmes exactes sont permises d'office (PS-181).
 */
class AndroidExactAlarmPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val features: PlatformFeatures,
    ) : ExactAlarmPermissionReader {
        @SuppressLint("NewApi") // Lue seulement quand l'autorisation existe (Android 12+).
        override fun isGranted(): Boolean =
            !features.exactAlarmPermission ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
