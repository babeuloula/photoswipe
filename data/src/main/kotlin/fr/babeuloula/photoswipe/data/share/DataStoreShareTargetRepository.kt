package fr.babeuloula.photoswipe.data.share

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.babeuloula.photoswipe.domain.port.ShareTargetRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

/** [ShareTargetRepository] sur le DataStore des réglages (PS-163). */
class DataStoreShareTargetRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ShareTargetRepository {
        override suspend fun lastTarget(): String? =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .first()[LAST_SHARE_TARGET]

        override suspend fun saveLastTarget(component: String) {
            dataStore.edit { it[LAST_SHARE_TARGET] = component }
        }

        private companion object {
            val LAST_SHARE_TARGET = stringPreferencesKey("last_share_target")
        }
    }
