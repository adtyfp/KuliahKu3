package com.kuliahku.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "kuliahku_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val LAST_ACTIVE_TIME = longPreferencesKey("last_active_time")

    private val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 hari

    suspend fun hasSeenWelcome(): Boolean =
        context.dataStore.data.first()[HAS_SEEN_WELCOME] ?: false

    suspend fun setHasSeenWelcome() {
        context.dataStore.edit { it[HAS_SEEN_WELCOME] = true }
    }

    suspend fun isLoggedIn(): Boolean {
        val prefs = context.dataStore.data.first()
        val loggedIn = prefs[IS_LOGGED_IN] ?: false
        if (!loggedIn) return false

        val lastActive = prefs[LAST_ACTIVE_TIME] ?: 0L
        val currentTime = System.currentTimeMillis()

        // Jika baru pertama kali (migrasi), set waktu aktif sekarang
        if (lastActive == 0L) {
            updateLastActiveTime()
            return true
        }

        // Cek apakah sesi sudah lebih dari 30 hari
        if (currentTime - lastActive > SESSION_DURATION_MS) {
            setLoggedIn(false)
            return false
        }
        
        // Perbarui waktu aktif terakhir
        updateLastActiveTime()
        return true
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { 
            it[IS_LOGGED_IN] = loggedIn
            if (loggedIn) {
                it[LAST_ACTIVE_TIME] = System.currentTimeMillis()
            } else {
                it[LAST_ACTIVE_TIME] = 0L
            }
        }
    }

    suspend fun updateLastActiveTime() {
        context.dataStore.edit { 
            it[LAST_ACTIVE_TIME] = System.currentTimeMillis()
        }
    }
}
