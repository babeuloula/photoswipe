package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ReminderSettingsTest {
    @Test
    fun `reminds at 18 00 by default`() {
        assertEquals(LocalTime.of(18, 0), ReminderSettings.DEFAULT.time)
    }

    @Test
    fun `is disabled by default until the user turns it on`() {
        assertFalse(ReminderSettings.DEFAULT.enabled)
    }
}
