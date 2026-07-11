package com.arena0077.app.data.auth

import com.arena0077.app.data.models.AuthSession
import com.arena0077.app.data.models.ArenaUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager - holds the current authentication session.
 *
 * arena.ai uses Supabase Auth (project: lmarena.supabase.co).
 * The auth state is persisted as an HTTP-only cookie named "arena-auth-prod-v1"
 * which contains a base64-encoded JSON with:
 *   - access_token  (JWT)
 *   - refresh_token
 *   - expires_at    (unix seconds)
 *   - token_type    ("bearer")
 *   - user          (full Supabase user object)
 *
 * In native code, we mirror this state in an encrypted DataStore.
 * In WebView, the cookie is set automatically by arena.ai when the user logs in.
 *
 * The two stores are kept in sync: when the WebView emits an auth-state-changed
 * event through the JsBridge, we update our native AuthSession here.
 */
@Singleton
class AuthManager @Inject constructor(
    private val authStorage: AuthStorage
) {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun init() {
        _session.value = authStorage.loadSession()
        _isLoading.value = false
    }

    suspend fun setSession(session: AuthSession) {
        authStorage.saveSession(session)
        _session.value = session
    }

    suspend fun clearSession() {
        authStorage.clearSession()
        _session.value = null
    }

    val isLoggedIn: Boolean
        get() = _session.value?.isValid == true

    val currentUser: ArenaUser?
        get() = _session.value?.user

    val accessToken: String?
        get() = _session.value?.accessToken

    val cookieHeader: String
        get() {
            val s = _session.value ?: return ""
            val json = kotlinx.serialization.json.Json { encodeDefaults = true }
            val jsonStr = json.encodeToString(com.arena0077.app.data.models.AuthSession.serializer(), s)
            return "arena-auth-prod-v1.0=base64-${java.util.Base64.getEncoder().encodeToString(jsonStr.encodeToByteArray())}"
        }
}
