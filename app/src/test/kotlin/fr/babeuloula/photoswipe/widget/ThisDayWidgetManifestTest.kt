package fr.babeuloula.photoswipe.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import fr.babeuloula.photoswipe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Le widget « Ce jour-là » est déclaré dans le manifeste, avec son réglage (PS-170). */
@RunWith(RobolectricTestRunner::class)
class ThisDayWidgetManifestTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun `declares the widget receiver with its provider info`() {
        val receivers =
            application.packageManager.queryBroadcastReceivers(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setPackage(application.packageName),
                PackageManager.GET_META_DATA,
            )

        val widget = receivers.single { it.activityInfo.name == ThisDayWidgetReceiver::class.java.name }
        assertTrue(widget.activityInfo.exported)
        assertEquals(
            R.xml.this_day_widget_info,
            widget.activityInfo.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER),
        )
    }

    @Test
    fun `declares the settings activity opened when the widget is placed`() {
        val activities =
            application.packageManager.queryIntentActivities(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setPackage(application.packageName),
                0,
            )

        assertEquals(ThisDayWidgetConfigureActivity::class.java.name, activities.single().activityInfo.name)
    }
}
