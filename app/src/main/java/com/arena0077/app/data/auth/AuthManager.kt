package com.arena0077.app.data.auth

import com.arena0077.app.data.models.AuthSession
import com.arena0077.app.data.models.SupabaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager - holds the current authentication session.
 *
 * arena.ai uses Supabase Auth (project: huogzoeqzcrdvkwtvodi.supabase.co).
 * The auth state is persisted as HTTP-only cookies named:
 *   - arena-auth-prod-v1.0  (first 4KB of the base64 payload)
 *   - arena-auth-prod-v1.1  (remaining bytes of the base64 payload)
 *
 * To reconstruct the session:
 *   1. Concatenate v1.0 + v1.1 (without the cookie names)
 *   2. Strip the "base64-" prefix from v1.0
 *   3. Base64-decode the result
 *   4. JSON-parse to get {access_token, refresh_token, expires_at, user, ...}
 *
 * The access_token is a JWT signed with ES256 algorithm.
 *
 * In native code, we mirror this state in an encrypted DataStore.
 * In WebView, the cookie is set automatically by arena.ai when the user logs in.
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

    val currentUser: SupabaseUser?
        get() = _session.value?.user

    val accessToken: String?
        get() = _session.value?.accessToken

    val refreshToken: String?
        get() = _session.value?.refreshToken

    /**
     * Build the arena-auth-prod-v1 cookie value for WebView injection.
     * The cookie must be split into v1.0 and v1.1 to fit the 4KB cookie limit.
     */
    fun buildCookieHeaders(): Map<String, String> {
        val s = _session.value ?: return emptyMap()
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val jsonStr = json.encodeToString(AuthSession.serializer(), s)
        val b64 = java.util.Base64.getEncoder().encodeToString(jsonStr.encodeToByteArray())
        val fullValue = "base64-$b64"

        // Split at ~4000 chars to stay under 4KB per cookie
        val splitPoint = 4000
        return if (fullValue.length > splitPoint) {
            mapOf(
                "arena-auth-prod-v1.0" to fullValue.substring(0, splitPoint),
                "arena-auth-prod-v1.1" to fullValue.substring(splitPoint)
            )
        } else {
            mapOf("arena-auth-prod-v1.0" to fullValue)
        }
    }
}
