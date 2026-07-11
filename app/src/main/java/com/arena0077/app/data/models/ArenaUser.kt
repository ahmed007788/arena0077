package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Auth session - matches the EXACT decoded shape of the arena-auth-prod-v1 cookie.
 *
 * The cookie is SPLIT into v1.0 and v1.1 because it exceeds the 4KB cookie limit.
 * Concatenate v1.0 + v1.1, strip the "base64-" prefix, base64-decode, then JSON-parse.
 *
 * Captured 2026-07-11 from production.
 */
@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("refresh_token") val refreshToken: String,
    val user: SupabaseUser,
    @SerialName("weak_password") val weakPassword: Boolean? = null
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() / 1000 >= expiresAt - 60

    val isValid: Boolean
        get() = accessToken.isNotBlank() && !isExpired
}

/**
 * Supabase user - the full user object inside the auth cookie.
 */
@Serializable
data class SupabaseUser(
    val id: String,
    val aud: String = "authenticated",
    val role: String = "authenticated",
    val email: String,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    val phone: String = "",
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("last_sign_in_at") val lastSignInAt: String? = null,
    @SerialName("app_metadata") val appMetadata: AppMetadata = AppMetadata(),
    @SerialName("user_metadata") val userMetadata: UserMetadata = UserMetadata(),
    val identities: List<Identity> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false
)

@Serializable
data class AppMetadata(
    val provider: String = "email",
    val providers: List<String> = listOf("email")
)

@Serializable
data class UserMetadata(
    @SerialName("domain_url") val domainUrl: String? = "https://lmarena.ai",
    val email: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("full_name") val fullName: String? = null,
    val id: String? = null,
    @SerialName("last_linked_supabase_user_id") val lastLinkedSupabaseUserId: String? = null,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("should_link_history") val shouldLinkHistory: Boolean = true,
    @SerialName("signup_intent_id") val signupIntentId: String? = null,
    val sub: String? = null
)

@Serializable
data class Identity(
    @SerialName("identity_id") val identityId: String,
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("identity_data") val identityData: IdentityData,
    val provider: String = "email",
    @SerialName("last_sign_in_at") val lastSignInAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val email: String? = null
)

@Serializable
data class IdentityData(
    @SerialName("domain_url") val domainUrl: String? = null,
    val email: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("full_name") val fullName: String? = null,
    val id: String? = null,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("should_link_history") val shouldLinkHistory: Boolean = true,
    @SerialName("signup_intent_id") val signupIntentId: String? = null,
    val sub: String? = null
)

/**
 * Decoded JWT payload from the access_token.
 * Algorithm: ES256 (ECDSA with P-256 and SHA-256)
 * Issuer: https://huogzoeqzcrdvkwtvodi.supabase.co/auth/v1
 */
@Serializable
data class JwtPayload(
    val iss: String,
    val sub: String,
    val aud: String,
    val exp: Long,
    val iat: Long,
    val email: String = "",
    val phone: String = "",
    @SerialName("app_metadata") val appMetadata: AppMetadata = AppMetadata(),
    @SerialName("user_metadata") val userMetadata: UserMetadata = UserMetadata(),
    val role: String = "authenticated",
    val aal: String = "aal1",
    val amr: List<AmrEntry> = emptyList(),
    @SerialName("session_id") val sessionId: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false
)

@Serializable
data class AmrEntry(
    val method: String,  // password|anonymous|oauth
    val timestamp: Long
)
