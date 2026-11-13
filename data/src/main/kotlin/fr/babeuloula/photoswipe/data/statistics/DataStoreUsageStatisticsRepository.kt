package fr.babeuloula.photoswipe.data.statistics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * [UsageStatisticsRepository] sur le DataStore des réglages (PS-122, ADR 0030) : totaux cumulés
 * (nombre, octets) et premier jour d'utilisation, en jours depuis l'epoch.
 *
 * Le premier jour est fixé à la première lecture : la date d'installation de l'application
 * ([firstInstallMillis]), ou aujourd'hui si elle est inconnue. Une installation déjà en service
 * avant ces statistiques garde ainsi son vrai premier jour, avec des totaux qui partent de zéro.
 *
 * @param firstInstallMillis date d'installation, en millisecondes depuis l'epoch (0 si inconnue).
 */
class DataStoreUsageStatisticsRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    private val firstInstallMillis: () -> Long,
) : UsageStatisticsRepository {
    /** Lit la date d'installation dans le gestionnaire de paquets d'Android. */
    @Inject
    constructor(
        dataStore: DataStore<Preferences>,
        clock: Clock,
        @ApplicationContext context: Context,
    ) : this(
        dataStore,
        clock,
        { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime },
    )

    override suspend fun statistics(): UsageStatistics {
        val preferences = preferences()
        val since =
            preferences[SINCE_EPOCH_DAY]?.let(LocalDate::ofEpochDay) ?: firstUseDay().also { day ->
                dataStore.edit { it[SINCE_EPOCH_DAY] = day.toEpochDay() }
            }
        return UsageStatistics(
            since = since,
            deletedCount = preferences[DELETED_COUNT] ?: 0,
            freedBytes = preferences[FREED_BYTES] ?: 0L,
        )
    }

    override suspend fun recordDeletion(
        count: Int,
        bytes: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[DELETED_COUNT] = (preferences[DELETED_COUNT] ?: 0) + count
            preferences[FREED_BYTES] = (preferences[FREED_BYTES] ?: 0L) + bytes
        }
    }

    override suspend fun removeDeletion(
        count: Int,
        bytes: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[DELETED_COUNT] = ((preferences[DELETED_COUNT] ?: 0) - count).coerceAtLeast(0)
            preferences[FREED_BYTES] = ((preferences[FREED_BYTES] ?: 0L) - bytes).coerceAtLeast(0)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(DELETED_COUNT)
            preferences.remove(FREED_BYTES)
        }
    }

    /** Jour d'installation, ou aujourd'hui s'il est inconnu. */
    private fun firstUseDay(): LocalDate {
        val installed = runCatching(firstInstallMillis).getOrDefault(0L)
        return if (installed > 0) {
            Instant.ofEpochMilli(installed).atZone(clock.zone).toLocalDate()
        } else {
            LocalDate.now(clock)
        }
    }

    /** Préférences enregistrées ; un fichier illisible se lit comme vide. */
    private suspend fun preferences(): Preferences =
        dataStore.data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }.first()

    private companion object {
        val SINCE_EPOCH_DAY = longPreferencesKey("statistics_since_epoch_day")
        val DELETED_COUNT = intPreferencesKey("statistics_deleted_count")
        val FREED_BYTES = longPreferencesKey("statistics_freed_bytes")
    }
}
