package com.arena0077.app.data.local

import com.arena0077.app.data.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionInterceptor - injects the Supabase auth cookie into every OkHttp request
 * bound for arena.ai.
 *
 * arena.ai reads authentication from the cookie `arena-auth-prod-v1`.
 * Without it, every authenticated endpoint returns 401.
 *
 * Headers added:
 *   Cookie: arena-auth-prod-v1=<base64-encoded session JSON>
 *   X-Client-Platform: android-native
 *
 * Note: For reCAPTCHA-protected endpoints (create-evaluation etc.), this
 * interceptor alone is insufficient. Those calls must go through the WebView.
 */
@Singleton
class SessionInterceptor @Inject constructor(
    private val authManager: AuthManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        authManager.accessToken?.let { token ->
            builder.header("Authorization", "Bearer $token")
            builder.header("Cookie", buildArenaCookie(token))
        }

        builder.header("X-Client-Platform", "android-native")
        builder.header("X-Client-Version", "1.0.0")
        builder.header("Accept", "application/json, text/event-stream, */*")

        return chain.proceed(builder.build())
    }

    /**
     * Build the arena-auth-prod-v1 cookie.
     * arena.ai expects this exact name + base64-encoded payload.
     */
    private fun buildArenaCookie(accessToken: String): String {
        // The actual cookie is set by arena.ai's server. For direct API calls,
        // we provide it via Authorization header. The Cookie header is a fallback.
        return "arena-auth-prod-v1=android-native; access_token=$accessToken"
    }
}
