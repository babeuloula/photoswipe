package fr.babeuloula.photoswipe.ui.navigation

import fr.babeuloula.photoswipe.domain.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoutesTest {
    @Test
    fun `carries the target day of a deck as an epoch day`() {
        val route = DeckRoute.of(LocalDate.of(2024, 2, 29))

        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), route.epochDay)
        assertEquals(LocalDate.of(2024, 2, 29), route.day)
    }

    @Test
    fun `opens the marked deck from the media tapped in the summary`() {
        val route = DeckRoute.marked(MediaId(42))

        assertTrue(route.isMarked)
        assertNull(route.day)
        assertEquals(42L, route.firstMarkedMediaId)
        assertFalse(DeckRoute.of(LocalDate.of(2026, 7, 15)).isMarked)
    }
}
