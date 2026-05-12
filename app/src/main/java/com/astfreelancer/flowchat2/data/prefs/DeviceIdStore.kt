package com.astfreelancer.flowchat2.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

val Context.dataStore by preferencesDataStore(name = "settings")
class DeviceIdStore(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    suspend fun getOrCreate(): String {
        val prefs = dataStore.data.first()
        prefs[Keys.DEVICE_ID]?.let { return it }

        val fresh = UUID.randomUUID().toString()
        dataStore.edit { it[Keys.DEVICE_ID] = fresh }
        return fresh
    }

    suspend fun clear() {
        dataStore.edit { it.remove(Keys.DEVICE_ID) }
    }
}
