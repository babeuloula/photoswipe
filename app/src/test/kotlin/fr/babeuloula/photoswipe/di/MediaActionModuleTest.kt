package fr.babeuloula.photoswipe.di

import android.app.PendingIntent
import android.content.Intent
import fr.babeuloula.photoswipe.data.media.IntentSenderMediaActionRequest
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Pont entre les demandes système de `:data` et l'écran qui les lance (ADR 0018). */
@RunWith(RobolectricTestRunner::class)
class MediaActionModuleTest {
    private val intents = MediaActionModule.provideMediaActionIntents()

    @Test
    fun `gives the system dialog of a MediaStore request`() {
        val intentSender =
            PendingIntent
                .getActivity(
                    RuntimeEnvironment.getApplication(),
                    0,
                    Intent(),
                    PendingIntent.FLAG_IMMUTABLE,
                ).intentSender

        assertSame(intentSender, intents.intentSender(IntentSenderMediaActionRequest(setOf(MediaId(1)), intentSender)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a request that does not come from MediaStore`() {
        intents.intentSender(
            object : MediaActionRequest {
                override val mediaIds = setOf(MediaId(1))
            },
        )
    }
}
