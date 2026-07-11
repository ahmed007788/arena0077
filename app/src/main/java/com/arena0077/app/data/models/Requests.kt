package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to create a new evaluation (start a new chat).
 *
 * EXACT shape captured from arena.ai production traffic on 2026-07-11.
 * This is the body sent to POST /nextjs-api/stream/create-evaluation.
 *
 * The client generates ALL UUIDs locally before sending - this is critical.
 * The server uses these IDs to track the conversation and messages.
 */
@Serializable
data class CreateEvaluationRequest(
    val id: String,                              // conversation UUID (client-generated)
    val mode: String,                            // battle|side|direct|direct-battle|agent
    @SerialName("userMessageId") val userMessageId: String,
    @SerialName("modelAMessageId") val modelAMessageId: String,
    @SerialName("modelBMessageId") val modelBMessageId: String,
    val userMessage: UserMessagePayload,
    val modality: String,                        // chat|image|video|webdev
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    @SerialName("recaptchaV2Token") val recaptchaV2Token: String? = null
)

@Serializable
data class UserMessagePayload(
    val content: String,
    @SerialName("experimental_attachments") val experimentalAttachments: List<Attachment> = emptyList(),
    val metadata: AutoModalityMetadata? = null
)

@Serializable
data class AutoModalityMetadata(
    @SerialName("autoModalityMetadata") val autoModalityMetadata: AutoModalityResult? = null
)

@Serializable
data class AutoModalityResult(
    val modality: String,
    @SerialName("shouldShowModalitySuggestion") val shouldShowModalitySuggestion: Boolean = false,
    @SerialName("suggestedModalities") val suggestedModalities: List<String> = emptyList(),
    val confidence: ModalityConfidence? = null,
    @SerialName("disabledModalities") val disabledModalities: List<String> = emptyList(),
    @SerialName("latencyMs") val latencyMs: Long = 0,
    val thresholds: ModalityThresholds? = null,
    @SerialName("wasAutoSelected") val wasAutoSelected: Boolean = true,
    @SerialName("treatmentVariant") val treatmentVariant: String = "treatment-3"
)

@Serializable
data class ModalityConfidence(
    val image: Double = 0.0,
    val search: Double = 0.0,
    val text: Double = 1.0,
    val video: Double = 0.0,
    val code: Double = 0.0
)

@Serializable
data class ModalityThresholds(
    val IMAGE: Double = 0.8,
    val VIDEO: Double = 0.95,
    val SEARCH: Double = 0.8,
    val CODE: Double = 0.25
)

/**
 * Request to send a follow-up message to an existing conversation.
 * POST /nextjs-api/stream/post-to-evaluation/{evaluationSessionId}
 */
@Serializable
data class PostToEvaluationRequest(
    val mode: String,
    @SerialName("userMessageId") val userMessageId: String,
    @SerialName("modelAMessageId") val modelAMessageId: String,
    @SerialName("modelBMessageId") val modelBMessageId: String,
    val userMessage: UserMessagePayload,
    val modality: String,
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    @SerialName("recaptchaV2Token") val recaptchaV2Token: String? = null
)

/**
 * Vote request - POST /api/vote
 */
@Serializable
data class VoteRequest(
    val value: String,                           // model_a|model_b|tie|bothbad
    @SerialName("messageAId") val messageAId: String,
    @SerialName("messageBId") val messageBId: String,
    @SerialName("evaluationSessionId") val evaluationSessionId: String,
    @SerialName("recaptchaV3Token") val recaptchaV3Token: String? = null,
    @SerialName("modelsRevealed") val modelsRevealed: Boolean? = null,
    @SerialName("didInspectBothMobileHorizontalResponses") val didInspectBothMobileHorizontalResponses: Boolean? = null
)

/**
 * Login request - POST /nextjs-api/sign-in/email
 * Captured 2026-07-11: includes shouldLinkHistory flag
 */
@Serializable
data class SignInEmailRequest(
    val email: String,
    val password: String,
    @SerialName("shouldLinkHistory") val shouldLinkHistory: Boolean = true
)

/**
 * Sign-up request - POST /nextjs-api/sign-up
 */
@Serializable
data class SignUpRequest(
    @SerialName("recaptchaToken") val recaptchaToken: String,
    @SerialName("provisionalUserId") val provisionalUserId: String
)

/**
 * Auto-modality detection - POST /nextjs-api/auto-modality
 * Captured 2026-07-11: uses user_prompt (not prompt) and has_image flag
 */
@Serializable
data class AutoModalityRequest(
    @SerialName("user_prompt") val userPrompt: String,
    @SerialName("has_image") val hasImage: Boolean = false
)

@Serializable
data class AutoModalityResponse(
    val modality: String,
    @SerialName("shouldShowModalitySuggestion") val shouldShowModalitySuggestion: Boolean = false,
    @SerialName("suggestedModalities") val suggestedModalities: List<String> = emptyList(),
    val confidence: ModalityConfidence = ModalityConfidence(),
    @SerialName("disabledModalities") val disabledModalities: List<String> = emptyList(),
    @SerialName("latencyMs") val latencyMs: Long = 0,
    val thresholds: ModalityThresholds = ModalityThresholds(),
    @SerialName("wasAutoSelected") val wasAutoSelected: Boolean = true,
    @SerialName("treatmentVariant") val treatmentVariant: String = "treatment-3"
)
