package fr.babeuloula.photoswipe.domain.fixtures

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Horloge de test : elle reste figée sur un instant, que le test déplace à la main.
 *
 * Contrairement à `Clock.fixed`, l'instant peut avancer ([advanceBy]) ou être changé ([setTo]) :
 * un même service, qui lit l'horloge à chaque appel, peut ainsi être testé de part et d'autre
 * de minuit. Elle n'est pas partagée entre threads : chaque test crée la sienne.
 *
 * @param instant instant de départ.
 * @param zone fuseau de l'horloge, celui de l'« appareil » simulé.
 */
class FixedClock(
    private var instant: Instant,
    private val zone: ZoneId = PARIS,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun instant(): Instant = instant

    /** Nouvelle horloge, indépendante de celle-ci, figée sur le même instant dans le fuseau [zone]. */
    override fun withZone(zone: ZoneId): FixedClock = FixedClock(instant, zone)

    /** Avance l'horloge de [duration] (négative pour reculer). */
    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration)
    }

    /** Place l'horloge sur [instant]. */
    fun setTo(instant: Instant) {
        this.instant = instant
    }

    /** Place l'horloge sur la date et l'heure locales [dateTime], lues dans son fuseau. */
    fun setTo(dateTime: LocalDateTime) {
        instant = dateTime.atZone(zone).toInstant()
    }

    override fun toString(): String = "FixedClock[$instant,$zone]"

    companion object {
        /** Fuseau par défaut des tests : celui de l'utilisateur principal de l'application. */
        val PARIS: ZoneId = ZoneId.of("Europe/Paris")

        /** Horloge figée sur la date et l'heure locales [dateTime], lues dans le fuseau [zone]. */
        fun at(
            dateTime: LocalDateTime,
            zone: ZoneId = PARIS,
        ): FixedClock = FixedClock(dateTime.atZone(zone).toInstant(), zone)
    }
}
