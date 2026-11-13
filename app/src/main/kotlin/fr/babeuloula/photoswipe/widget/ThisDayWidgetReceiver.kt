package fr.babeuloula.photoswipe.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.EntryPointAccessors

/**
 * Récepteur du widget « Ce jour-là » (PS-170), déclaré dans le manifeste : il le rattache à
 * [ThisDayWidget], planifie sa mise à jour quotidienne à la pose du premier widget et l'annule au
 * retrait du dernier.
 */
class ThisDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThisDayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updates(context).scheduleDaily()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        updates(context).cancel()
    }

    private fun updates(context: Context): ThisDayWidgetUpdates =
        EntryPointAccessors.fromApplication<ThisDayWidgetEntryPoint>(context.applicationContext).widgetUpdates()
}
