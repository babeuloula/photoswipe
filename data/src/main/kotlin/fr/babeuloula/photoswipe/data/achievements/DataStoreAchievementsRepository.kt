package fr.babeuloula.photoswipe.data.achievements

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.port.AchievementsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

/**
 * [AchievementsRepository] sur le DataStore des réglages (PS-158) : les badges obtenus par leur
 * nom (un nom inconnu est ignoré), l'objectif par ses deux nombres d'octets.
 */
class DataStoreAchievementsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : AchievementsRepository {
        override suspend fun earnedBadges(): Set<Badge> =
            preferences()[EARNED_BADGES]
                .orEmpty()
                .mapNotNullTo(HashSet()) { name -> Badge.entries.firstOrNull { it.name == name } }

        override suspend fun saveEarnedBadges(badges: Set<Badge>) {
            dataStore.edit { it[EARNED_BADGES] = badges.mapTo(HashSet()) { badge -> badge.name } }
        }

        override suspend fun freeSpaceGoal(): FreeSpaceGoal? {
            val preferences = preferences()
            val target = preferences[GOAL_TARGET_BYTES] ?: return null
            return FreeSpaceGoal(target, preferences[GOAL_START_BYTES] ?: 0)
        }

        override suspend fun saveFreeSpaceGoal(goal: FreeSpaceGoal?) {
            dataStore.edit { preferences ->
                if (goal == null) {
                    preferences.remove(GOAL_TARGET_BYTES)
                    preferences.remove(GOAL_START_BYTES)
                } else {
                    preferences[GOAL_TARGET_BYTES] = goal.targetBytes
                    preferences[GOAL_START_BYTES] = goal.startFreedBytes
                }
            }
        }

        /** Préférences enregistrées ; un fichier illisible se lit comme vide. */
        private suspend fun preferences(): Preferences =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.first()

        private companion object {
            val EARNED_BADGES = stringSetPreferencesKey("earned_badges")
            val GOAL_TARGET_BYTES = longPreferencesKey("goal_target_bytes")
            val GOAL_START_BYTES = longPreferencesKey("goal_start_bytes")
        }
    }
