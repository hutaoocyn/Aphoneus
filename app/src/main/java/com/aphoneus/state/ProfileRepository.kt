package com.aphoneus.state

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aphoneus.model.CustomProfile
import com.aphoneus.model.PrimaryMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "aphoneus_preferences")

class ProfileRepository(private val context: Context) {

    private val activeModeKey = stringPreferencesKey("active_mode")
    private val customProfilesKey = stringPreferencesKey("custom_profiles")
    private val bootCrashCountKey = intPreferencesKey("boot_crash_count")
    private val thermalCeilingKey = intPreferencesKey("thermal_ceiling_celsius")

    val activeModeFlow: Flow<PrimaryMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[activeModeKey] ?: PrimaryMode.BALANCED.name
        runCatching { PrimaryMode.valueOf(modeStr) }.getOrDefault(PrimaryMode.BALANCED)
    }

    val customProfilesFlow: Flow<List<CustomProfile>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[customProfilesKey] ?: "[]"
        runCatching { Json.decodeFromString<List<CustomProfile>>(jsonStr) }.getOrDefault(emptyList())
    }

    val bootCrashCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[bootCrashCountKey] ?: 0
    }

    val thermalCeilingFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[thermalCeilingKey] ?: 85
    }

    suspend fun setActiveMode(mode: PrimaryMode) {
        context.dataStore.edit { prefs ->
            prefs[activeModeKey] = mode.name
        }
    }

    suspend fun saveCustomProfile(profile: CustomProfile) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[customProfilesKey] ?: "[]"
            val list = runCatching {
                Json.decodeFromString<List<CustomProfile>>(existingJson).toMutableList()
            }.getOrDefault(mutableListOf())

            list.removeAll { it.id == profile.id }
            list.add(profile)
            prefs[customProfilesKey] = Json.encodeToString(list)
        }
    }

    suspend fun deleteCustomProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[customProfilesKey] ?: "[]"
            val list = runCatching {
                Json.decodeFromString<List<CustomProfile>>(existingJson).toMutableList()
            }.getOrDefault(mutableListOf())

            list.removeAll { it.id == profileId }
            prefs[customProfilesKey] = Json.encodeToString(list)
        }
    }

    suspend fun incrementCrashCount(): Int {
        var count = 0
        context.dataStore.edit { prefs ->
            val cur = prefs[bootCrashCountKey] ?: 0
            count = cur + 1
            prefs[bootCrashCountKey] = count
        }
        return count
    }

    suspend fun resetCrashCount() {
        context.dataStore.edit { prefs ->
            prefs[bootCrashCountKey] = 0
        }
    }

    suspend fun setThermalCeiling(celsius: Int) {
        context.dataStore.edit { prefs ->
            prefs[thermalCeilingKey] = celsius
        }
    }

    fun exportProfileJson(profile: CustomProfile): String {
        return Json.encodeToString(profile)
    }

    fun importProfileJson(jsonStr: String): CustomProfile? {
        return runCatching { Json.decodeFromString<CustomProfile>(jsonStr) }.getOrNull()
    }
}
