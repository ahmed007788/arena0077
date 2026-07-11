package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to create a new evaluation (start a new chat).
 *
 * This is the body sent to POST /nextjs-api/stream/create-evaluation.
 * Extracted from the actual JS source code at:
 *   /_next/static/chunks/26145-7f05e3e8cc0adc58.js
 *
 * The actual fetch call in arena.ai's code is:
 *   fetch("/nextjs-api/stream/create-evaluation", {
 *     method: "POST",
 *     body: JSON.stringify({
 *       ...W,  // (the base payload below)
 *       ...(recaptchaV2Token ? {
 *         recaptchaV2Token,
 *         recaptchaV3Token: undefined
 *       } : {})
 *     })
 *   })
 *
 * The `W` payload contains:
 *   - modality: "chat" | "image" | "video" | "webdev"
 *   - mode: "battle" | "side" | "direct" | "agent"
 *   - prompt: string (user message)
 *   - title: string (conversation title)
 *   - modelAId / modelBId: optional (for direct/side mode)
 *   - files: array of attachment references
 *   - webhook: optional webhook configuration
 *   - secrets: optional secrets for A/B
 */
@Serializable
data class CreateEvaluationRequest(
    val modality: Modality,
    val mode: BattleMode,
    val prompt: String,
    val title: String? = null,
    @SerialName("modelAId") val modelAId: String? = null,
    @SerialName("modelBId") val modelBId: String? = null,
    val files: List<String> = emptyList(),
    @SerialName("recaptchaV2Token") val recaptchaV2Token: String? = null,
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    val webhook: WebhookConfig? = null,
    val secrets: SecretsConfig? = null
)

/**
 * Request to send a follow-up message to an existing conversation.
 * POST /nextjs-api/stream/post-to-evaluation/{evaluationSessionId}
 */
@Serializable
data class PostToEvaluationRequest(
    val prompt: String,
    val files: List<String> = emptyList(),
    @SerialName("recaptchaV2Token") val recaptchaV2Token: String? = null,
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    val mode: BattleMode? = null  // omitted for followups (set to undefined)
)

@Serializable
data class WebhookConfig(
    val url: String,
    val data: String? = null
)

@Serializable
data class SecretsConfig(
    val a: List<String>? = null,
    val b: List<String>? = null
)

/**
 * Vote request - POST /api/vote
 */
@Serializable
data class VoteRequest(
    val value: VoteValue,
    @SerialName("messageAId") val messageAId: String,
    @SerialName("messageBId") val messageBId: String,
    @SerialName("evaluationSessionId") val evaluationSessionId: String,
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    @SerialName("modelsRevealed") val modelsRevealed: Boolean? = null,
    @SerialName("didInspectBothMobileHorizontalResponses") val didInspectBothMobileHorizontalResponses: Boolean? = null
)

/**
 * Login request - POST /nextjs-api/sign-in/email
 */
@Serializable
data class SignInEmailRequest(
    val email: String,
    val password: String,
    @SerialName("recaptchaToken") val recaptchaToken: String? = null
)

/**
 * Sign-up request - POST /nextjs-api/sign-up
 * Creates an anonymous user (used before email login).
 */
@Serializable
data class SignUpRequest(
    @SerialName("recaptchaToken") val recaptchaToken: String,
    @SerialName("provisionalUserId") val provisionalUserId: String
)

/**
 * Stop streaming request - POST /nextjs-api/stream/stop/{id}/messages/{messageId}
 */
@Serializable
data class StopStreamingRequest(
    @SerialName("stoppedAt") val stoppedAt: String? = null
)

/**
 * Auto-modality detection - POST /nextjs-api/auto-modality
 * arena.ai uses this to auto-detect if a prompt is code, search, image, or text.
 */
@Serializable
data class AutoModalityRequest(
    val prompt: String
)

@Serializable
data class AutoModalityResponse(
    val modality: Modality,
    val confidence: Double? = null
)
