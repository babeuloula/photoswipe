package fr.babeuloula.photoswipe.data.activity

import fr.babeuloula.photoswipe.data.database.DailyActivityDao
import fr.babeuloula.photoswipe.data.database.DailyActivityEntity
import fr.babeuloula.photoswipe.domain.model.DailyActivity
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import java.time.LocalDate
import javax.inject.Inject

/**
 * [ActivityHistory] sur la base Room, table `daily_activity` (ADR 0037). Les jours sont stockés en
 * jours depuis l'epoch.
 */
class RoomActivityHistory
    @Inject
    constructor(
        private val dao: DailyActivityDao,
    ) : ActivityHistory {
        override suspend fun recordTrashed(
            day: LocalDate,
            count: Int,
            bytes: Long,
        ) {
            dao.addTrashed(day.toEpochDay(), count, bytes)
        }

        override suspend fun removeTrashed(
            day: LocalDate,
            count: Int,
            bytes: Long,
        ) {
            dao.removeTrashed(day.toEpochDay(), count, bytes)
        }

        override suspend fun since(day: LocalDate): List<DailyActivity> =
            dao.since(day.toEpochDay()).map { it.toDomain() }

        override suspend fun firstDay(): LocalDate? = dao.firstDay()?.let(LocalDate::ofEpochDay)

        private fun DailyActivityEntity.toDomain(): DailyActivity =
            DailyActivity(LocalDate.ofEpochDay(day), keptCount, favoriteCount, trashedCount, freedBytes)
    }
