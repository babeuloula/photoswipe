package fr.babeuloula.photoswipe.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RoutesTest {
    @Test
    fun `carries the target day of a deck as an epoch day`() {
        val route = DeckRoute.of(LocalDate.of(2024, 2, 29))

        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), route.epochDay)
        assertEquals(LocalDate.of(2024, 2, 29), route.day)
    }
}
