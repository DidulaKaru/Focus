package com.example.currencyconverter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AmbientSound {
    OFF,
    RAIN,
    WHITE_NOISE,
    FOREST
}

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val WORK_DURATION_KEY = intPreferencesKey("work_duration_minutes")
        private val BREAK_DURATION_KEY = intPreferencesKey("break_duration_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
    }

    val workDurationFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_DURATION_KEY] ?: 25
    }

    val breakDurationFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BREAK_DURATION_KEY] ?: 5
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val rawMode = preferences[THEME_MODE]
        runCatching { rawMode?.let { ThemeMode.valueOf(it) } }.getOrNull() ?: ThemeMode.SYSTEM
    }

    val ambientSoundFlow: Flow<AmbientSound> = context.dataStore.data.map { preferences ->
        val rawSound = preferences[AMBIENT_SOUND]
        runCatching { rawSound?.let { AmbientSound.valueOf(it) } }.getOrNull() ?: AmbientSound.OFF
    }

    suspend fun setWorkDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[WORK_DURATION_KEY] = minutes
        }
    }

    suspend fun setBreakDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[BREAK_DURATION_KEY] = minutes
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setAmbientSound(sound: AmbientSound) {
        context.dataStore.edit { preferences ->
            preferences[AMBIENT_SOUND] = sound.name
        }
    }
}
