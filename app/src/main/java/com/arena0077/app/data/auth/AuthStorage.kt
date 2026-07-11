package com.arena0077.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arena0077.app.data.models.AuthSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_state")

/**
 * AuthStorage - persists the Supabase auth session to an (encrypted) DataStore.
 *
 * File: /data/data/com.arena0077.app/files/datastore/auth_state.preferences_pb
 *
 * For production, this should use EncryptedSharedPreferences or Tink-backed
 * DataStore. For now we store the JSON directly.
 */
@Singleton
class AuthStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val sessionKey = stringPreferencesKey("session_json")

    suspend fun saveSession(session: AuthSession) {
        context.authDataStore.edit { prefs ->
            prefs[sessionKey] = json.encodeToString(AuthSession.serializer(), session)
        }
    }

    suspend fun loadSession(): AuthSession? {
        val prefs = context.authDataStore.data.first()
        val jsonStr = prefs[sessionKey] ?: return null
        return runCatching {
            json.decodeFromString(AuthSession.serializer(), jsonStr)
        }.getOrNull()
    }

    suspend fun clearSession() {
        context.authDataStore.edit { it.remove(sessionKey) }
    }
}
