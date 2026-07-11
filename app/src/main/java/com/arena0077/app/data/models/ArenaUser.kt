package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User model - matches arena.ai's Supabase auth user structure.
 * Extracted from the actual JWT payload and /api/me response.
 */
@Serializable
data class ArenaUser(
    val id: String,
    val aud: String = "authenticated",
    val role: String = "authenticated",
    val email: String,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
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
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("identity_data") val identityData: IdentityData,
    val provider: String = "email",
    @SerialName("last_sign_in_at") val lastSignInAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class IdentityData(
    @SerialName("domain_url") val domainUrl: String? = null,
    val email: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("full_name") val fullName: String? = null,
    val id: String? = null
)

/**
 * Auth session persisted across app launches.
 * Stored in encrypted DataStore (auth_state.xml).
 */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val tokenType: String = "bearer",
    val user: ArenaUser
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() / 1000 >= expiresAt - 60

    val isValid: Boolean
        get() = accessToken.isNotBlank() && !isExpired
}
