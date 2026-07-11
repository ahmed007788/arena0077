package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI Model - EXACT shape from arena.ai leaderboard RSC payload.
 * Captured 2026-07-11 from https://arena.ai/leaderboard
 *
 * 785 models across 50 organizations.
 */
@Serializable
data class AIModel(
    val id: String,
    val organization: String? = null,
    val provider: String? = null,
    val publicName: String,
    val name: String,
    val displayName: String,
    val capabilities: ModelCapabilities? = null,
    @SerialName("userSelectable") val userSelectable: Boolean = true,
    val rank: Int? = null,
    @SerialName("rankByModality") val rankByModality: Map<String, Long> = emptyMap()
)

@Serializable
data class ModelCapabilities(
    @SerialName("inputCapabilities") val inputCapabilities: InputCapabilities? = null,
    @SerialName("outputCapabilities") val outputCapabilities: OutputCapabilities? = null
)

@Serializable
data class InputCapabilities(
    val text: Boolean = false,
    val image: ImageInputCapability? = null,
    val file: Boolean = false
)

@Serializable
data class ImageInputCapability(
    @SerialName("multipleImages") val multipleImages: Boolean = false
)

@Serializable
data class OutputCapabilities(
    val text: Boolean = false,
    val web: Boolean = false,
    val image: ImageOutputCapability? = null,
    val search: Boolean = false
)

@Serializable
data class ImageOutputCapability(
    @SerialName("aspectRatios") val aspectRatios: List<String> = emptyList()
)

/**
 * Leaderboard category - arena.ai ranks models in multiple categories.
 */
@Serializable
enum class LeaderboardCategory(val apiValue: String, val displayName: String) {
    OVERALL("overall", "Overall"),
    TEXT("text", "Text"),
    VISION("vision", "Vision"),
    CODING("coding", "Coding"),
    HARD("hard", "Hard Prompts"),
    AGENT("agent", "Agent");

    companion object {
        fun fromApi(value: String?): LeaderboardCategory =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: OVERALL
    }
}

/**
 * Leaderboard entry - a model with its ranking.
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val model: AIModel,
    @SerialName("arena_score") val arenaScore: Double,
    @SerialName("confidence_interval") val confidenceInterval: Double,
    val votes: Long,
    val organization: String? = null,
    val license: String? = null,
    @SerialName("knowledge_cutoff") val knowledgeCutoff: String? = null
)

/**
 * Model vote - used in Battle Mode to vote on the better model.
 * POST /api/vote
 */
@Serializable
enum class VoteValue(val apiValue: String, val displayName: String) {
    @SerialName("model_a") MODEL_A_UPVOTE("model_a", "A is better"),
    @SerialName("model_b") MODEL_B_UPVOTE("model_b", "B is better"),
    @SerialName("tie") TIE("tie", "Tie"),
    @SerialName("bothbad") BOTH_BAD("bothbad", "Both are bad");

    companion object {
        fun fromApi(value: String?): VoteValue =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: TIE
    }
}

/**
 * Arena.ai user - from GET /api/me
 * Captured 2026-07-11
 */
@Serializable
data class ArenaUser(
    val user: ArenaUserInfo
)

@Serializable
data class ArenaUserInfo(
    val id: String,
    @SerialName("supabaseUserId") val supabaseUserId: String,
    @SerialName("touConsentTimestamp") val touConsentTimestamp: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    val email: String,
    @SerialName("emailProvider") val emailProvider: String = "email",
    val username: String? = null,
    @SerialName("marketingSubscribed") val marketingSubscribed: Boolean = false
)
