package fr.babeuloula.photoswipe.link

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DeepLinksTest {
    @Test
    fun `opens the deck of today`() {
        assertEquals(LaunchRequest.TodayDeck, DeepLinks.parse("photoswipe://today"))
        assertEquals(LaunchRequest.TodayDeck, DeepLinks.parse("photoswipe://today/"))
    }

    @Test
    fun `opens the summary`() {
        assertEquals(LaunchRequest.Summary, DeepLinks.parse("photoswipe://summary"))
    }

    @Test
    fun `opens the overview of a date`() {
        assertEquals(
            LaunchRequest.DayOverview(LocalDate.of(2026, 7, 15)),
            DeepLinks.parse("photoswipe://day/2026-07-15"),
        )
    }

    @Test
    fun `ignores the case of the scheme and of the screen`() {
        assertEquals(LaunchRequest.Summary, DeepLinks.parse("PhotoSwipe://Summary"))
    }

    @Test
    fun `builds links that parse back to their screen`() {
        val day = LocalDate.of(2024, 2, 29)

        assertEquals(LaunchRequest.TodayDeck, DeepLinks.parse(DeepLinks.TODAY))
        assertEquals(LaunchRequest.Summary, DeepLinks.parse(DeepLinks.SUMMARY))
        assertEquals(LaunchRequest.DayOverview(day), DeepLinks.parse(DeepLinks.day(day)))
    }

    @Test
    fun `opens the home screen for an invalid link`() {
        listOf(
            "photoswipe://",
            "photoswipe://settings",
            "photoswipe://today/extra",
            "photoswipe://summary/2026-07-15",
            "photoswipe://day",
            "photoswipe://day/",
            "photoswipe://day/2026-02-30",
            "photoswipe://day/15-07-2026",
            "photoswipe://day/2026-07-15/more",
            "photoswipe:today",
            "https://today",
            "not a link",
            "",
        ).forEach { link -> assertEquals(link, LaunchRequest.Home, DeepLinks.parse(link)) }
    }
}
