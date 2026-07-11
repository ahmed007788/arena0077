package com.arena0077.app.data.api

import com.arena0077.app.data.models.AutoModalityRequest
import com.arena0077.app.data.models.AutoModalityResponse
import com.arena0077.app.data.models.HistoryResponse
import com.arena0077.app.data.models.SignInEmailRequest
import com.arena0077.app.data.models.SignUpRequest
import com.arena0077.app.data.models.ArenaUser
import com.arena0077.app.data.models.VoteRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Arena.ai REST API surface.
 *
 * Every endpoint here was extracted from arena.ai's production JS bundles:
 *   /_next/static/chunks/26145-7f05e3e8cc0adc58.js  (chat streaming logic)
 *   /_next/static/chunks/1352-a493db734fba80d6.js    (Zod schemas / request shapes)
 *   /_next/static/chunks/1780-37d607c4c0817365.js    (sign-up / recaptcha)
 *
 * IMPORTANT: Endpoints that require reCAPTCHA Enterprise tokens
 * (create-evaluation, post-to-evaluation, vote) are NOT safe to call
 * directly from native code. They must be called through the WebView
 * so reCAPTCHA V2/V3 tokens can be obtained. See ArenaWebViewClient.
 */
interface ArenaApi {

    // ===================== Auth =====================

    /** POST /nextjs-api/sign-up - create anonymous user before login. */
    @POST("nextjs-api/sign-up")
    suspend fun signUp(@Body body: SignUpRequest): ArenaUser

    /** POST /nextjs-api/sign-in/email - email + password login. */
    @POST("nextjs-api/sign-in/email")
    suspend fun signInWithEmail(@Body body: SignInEmailRequest): ArenaUser

    /** POST /nextjs-api/sign-out - logout. */
    @POST("nextjs-api/sign-out")
    suspend fun signOut()

    /** POST /nextjs-api/reset-password/request - request password reset. */
    @POST("nextjs-api/reset-password/request")
    suspend fun requestPasswordReset(@Body body: Map<String, String>)

    /** GET /api/me - current authenticated user. */
    @GET("api/me")
    suspend fun getMe(): ArenaUser

    // ===================== History =====================

    /** GET /api/history/unified - list recent conversations. */
    @GET("api/history/unified")
    suspend fun getHistory(
        @Query("limit") limit: Int = 20,
        @Query("includeArchived") includeArchived: Boolean = false,
        @Query("cursor") cursor: String? = null
    ): HistoryResponse

    // ===================== Modality routing =====================

    /** POST /nextjs-api/auto-modality - auto-detect prompt modality. */
    @POST("nextjs-api/auto-modality")
    suspend fun autoModality(@Body body: AutoModalityRequest): AutoModalityResponse

    // ===================== Chat streaming (reCAPTCHA-protected) =====================
    // These endpoints are NOT called directly. The WebView handles them.
    // The signatures are kept here for documentation and future use.

    /** POST /nextjs-api/stream/create-evaluation - start a new chat (battle/side/direct). */
    @POST("nextjs-api/stream/create-evaluation")
    suspend fun createEvaluation(@Body body: RequestBody): ResponseBody

    /** POST /nextjs-api/stream/post-to-evaluation/{id} - send followup message. */
    @POST("nextjs-api/stream/post-to-evaluation/{id}")
    suspend fun postToEvaluation(
        @Path("id") evaluationSessionId: String,
        @Body body: RequestBody
    ): ResponseBody

    /** POST /nextjs-api/stream/stop/{id}/messages/{messageId} - stop streaming. */
    @POST("nextjs-api/stream/stop/{id}/messages/{messageId}")
    suspend fun stopStreaming(
        @Path("id") evaluationSessionId: String,
        @Path("messageId") messageId: String,
        @Body body: RequestBody
    ): ResponseBody

    /** POST /nextjs-api/stream/rerun/{id} - rerun a message. */
    @POST("nextjs-api/stream/rerun/{id}")
    suspend fun rerun(@Path("id") messageId: String, @Body body: RequestBody): ResponseBody

    /** POST /nextjs-api/stream/resample/{id} - resample a response. */
    @POST("nextjs-api/stream/resample/{id}")
    suspend fun resample(@Path("id") evaluationSessionId: String, @Body body: RequestBody): ResponseBody

    /** POST /nextjs-api/stream/skip-direct-battle/{id} - skip direct battle. */
    @POST("nextjs-api/stream/skip-direct-battle/{id}")
    suspend fun skipDirectBattle(@Path("id") evaluationSessionId: String): ResponseBody

    // ===================== Voting =====================

    /** POST /api/vote - vote for the better model in battle mode. */
    @POST("api/vote")
    suspend fun vote(@Body body: VoteRequest): ResponseBody

    // ===================== Media =====================

    /** GET /nextjs-api/proxy/media?url={url} - proxy external media through arena.ai. */
    @GET("nextjs-api/proxy/media")
    suspend fun proxyMedia(@Query("url") url: String): ResponseBody

    // ===================== File upload =====================

    /** POST /api/files - upload a file attachment. */
    @Multipart
    @POST("api/files")
    suspend fun uploadFile(@Part part: MultipartBody.Part): ResponseBody

    // ===================== Web dev (Design to Code, etc.) =====================

    /** GET /api/evaluation/webdev/{id}/stream-credentials - web dev streaming credentials. */
    @GET("api/evaluation/webdev/{id}/stream-credentials")
    suspend fun getWebdevStreamCredentials(@Path("id") id: String): ResponseBody

    /** POST /nextjs-api/stream/resume-webdev/{id} - resume web dev session. */
    @POST("nextjs-api/stream/resume-webdev/{id}")
    suspend fun resumeWebdev(@Path("id") id: String, @Body body: RequestBody): ResponseBody

    /** POST /nextjs-api/stream/resume-video-workflow/{id} - resume video workflow. */
    @POST("nextjs-api/stream/resume-video-workflow/{id}")
    suspend fun resumeVideoWorkflow(@Path("id") id: String, @Body body: RequestBody): ResponseBody

    /** POST /nextjs-api/stream/retry-evaluation-session-message/{id}/messages/{msgId} - retry. */
    @POST("nextjs-api/stream/retry-evaluation-session-message/{id}/messages/{msgId}")
    suspend fun retryMessage(
        @Path("id") evaluationSessionId: String,
        @Path("msgId") messageId: String,
        @Body body: RequestBody
    ): ResponseBody
}
