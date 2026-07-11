package com.arena0077.app.data.chat

import com.arena0077.app.data.api.ArenaApi
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.models.ArenaUser
import com.arena0077.app.data.models.AuthSession
import com.arena0077.app.data.models.AutoModalityRequest
import com.arena0077.app.data.models.Conversation
import com.arena0077.app.data.models.HistoryResponse
import com.arena0077.app.data.models.Modality
import com.arena0077.app.data.models.SignInEmailRequest
import com.arena0077.app.data.models.SignUpRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository - thin wrapper around the REST API for non-reCAPTCHA endpoints.
 *
 * reCAPTCHA-protected endpoints (create-evaluation, post-to-evaluation) are
 * routed through the WebView. See ChatWebViewEngine.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: ArenaApi,
    private val authManager: AuthManager
) {
    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 8)
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    // ===================== Auth =====================

    suspend fun signInWithEmail(email: String, password: String): Result<ArenaUser> = runCatching {
        val request = SignInEmailRequest(
            email = email,
            password = password,
            shouldLinkHistory = true
        )
        val user = api.signInWithEmail(request)
        // Session is set via WebView cookie - placeholder until then
        user
    }.recoverCatching { e ->
        _errors.tryEmit(e)
        throw e
    }

    suspend fun signUpAnonymous(provisionalUserId: String = UUID.randomUUID().toString()): Result<ArenaUser> = runCatching {
        api.signUp(SignUpRequest(recaptchaToken = "", provisionalUserId = provisionalUserId))
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        runCatching { api.signOut() }
        authManager.clearSession()
    }

    // ===================== User =====================

    suspend fun getMe(): Result<ArenaUser> = runCatching {
        api.getMe()
    }

    // ===================== History =====================

    suspend fun loadHistory(
        limit: Int = 20,
        includeArchived: Boolean = false,
        cursor: String? = null
    ): Result<HistoryResponse> = runCatching {
        api.getHistory(limit = limit, includeArchived = includeArchived, cursor = cursor)
    }.recoverCatching { e ->
        _errors.tryEmit(e)
        throw e
    }

    // ===================== Conversation =====================

    suspend fun loadConversation(id: String): Result<Conversation> = runCatching {
        api.getEvaluation(id)
    }.recoverCatching { e ->
        _errors.tryEmit(e)
        throw e
    }

    // ===================== Modality routing =====================

    suspend fun autoModality(prompt: String, hasImage: Boolean = false): Result<Modality> = runCatching {
        val req = AutoModalityRequest(userPrompt = prompt, hasImage = hasImage)
        val resp = api.autoModality(req)
        Modality.fromApi(resp.modality)
    }.recoverCatching { e ->
        _errors.tryEmit(e)
        Modality.CHAT
    }
}
