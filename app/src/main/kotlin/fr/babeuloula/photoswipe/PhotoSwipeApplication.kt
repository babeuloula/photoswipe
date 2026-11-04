package fr.babeuloula.photoswipe

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application Android : racine du graphe de dépendances Hilt (SPEC §5.5). */
@HiltAndroidApp
class PhotoSwipeApplication : Application()
