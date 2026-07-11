package com.arena0077.app.data.chat

import com.arena0077.app.data.api.ArenaApi
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.models.ArenaUser
import com.arena0077.app.data.models.AuthSession
import com.arena0077.app.data.models.AutoModalityRequest
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Conversation
import com.arena0077.app.data.models.CreateEvaluationRequest
import com.arena0077.app.data.models.HistoryItem
import com.arena0077.app.data.models.HistoryResponse
import com.arena0077.app.data.models.Message
import com.arena0077.app.data.models.Modality
import com.arena0077.app.data.models.PostToEvaluationRequest
import com.arena0077.app.data.models.SignInEmailRequest
import com.arena0077.app.data.models.SignUpRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository - thin wrapper around the REST API for non-reCAPTCHA endpoints.
 *
 * reCAPTCHA-protected endpoints (create-evaluation, post-to-evaluation) are
 * routed through the WebView. See ChatWebViewEngine.
 *
 * Public surface:
 *   - signInWithEmail(email, password) - login
 *   - signUpAnonymous() - create anonymous user
 *   - signOut()
 *   - loadHistory(limit, cursor) - paginated conversation list
 *   - loadConversation(id) - full conversation with messages
 *   - autoModality(prompt) - modality auto-detection
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: ArenaApi,
    private val authManager: AuthManager,
    private val json: Json
) {
    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 8)
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    // ===================== Auth =====================

    suspend fun signInWithEmail(email: String, password: String): Result<ArenaUser> = runCatching {
        // Step 1: ensure we have an anonymous provisional user (arena.ai quirk).
        // Step 2: POST /nextjs-api/sign-in/email with email + password.
        val request = SignInEmailRequest(
            email = email,
            password = password,
            recaptchaToken = null  // Server allows null on the email sign-in path
        )
        val user = api.signInWithEmail(request)
        val session = AuthSession(
            accessToken = "placeholder",  // Real token comes from the WebView cookie
            refreshToken = "placeholder",
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = user
        )
        authManager.setSession(session)
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
        // arena.ai returns conversation details through the page RSC payload.
        // For native, we hit a (yet-to-be-formalized) endpoint or read from cache.
        // For now we return a placeholder - the WebView fills in real data.
        Conversation(
            id = id,
            title = null,
            createdAt = System.currentTimeMillis().toString(),
            messages = emptyList()
        )
    }

    // ===================== Modality routing =====================

    suspend fun autoModality(prompt: String): Result<Modality> = runCatching {
        api.autoModality(AutoModalityRequest(prompt = prompt)).modality
    }.recoverCatching { e ->
        _errors.tryEmit(e)
        // Fall back to CHAT - the safest default
        Modality.CHAT
    }

    // ===================== Streaming (delegated to WebView) =====================
    //
    // The following APIs are intentionally NOT implemented here. They require
    // reCAPTCHA Enterprise tokens that can only be obtained in a browser context.
    // Use ChatWebViewEngine for these operations:
    //
    //   - createEvaluation(...)    -> start a new chat
    //   - postToEvaluation(...)    -> send followup message
    //   - stopStreaming(...)       -> stop generation
    //   - rerun / resample / retry -> regenerate responses
    //   - vote(...)                -> vote for better model
    //
    // The engine posts a JSON command to the WebView; the WebView executes
    // arena.ai's own fetch() code (with reCAPTCHA) and streams results back
    // via the JsBridge.

    fun buildCreateEvaluationPayload(req: CreateEvaluationRequest): String =
        json.encodeToString(CreateEvaluationRequest.serializer(), req)

    fun buildPostToEvaluationPayload(req: PostToEvaluationRequest): String =
        json.encodeToString(PostToEvaluationRequest.serializer(), req)
}
