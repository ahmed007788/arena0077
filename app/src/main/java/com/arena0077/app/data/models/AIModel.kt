package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI Model - extracted from arena.ai's leaderboard data.
 *
 * arena.ai ranks hundreds of models. Each has an id, name, organization,
 * and arena score. Users can pick specific models in Direct Chat mode.
 */
@Serializable
data class AIModel(
    val id: String,
    val name: String,
    val organization: String? = null,
    @SerialName("arena_score") val arenaScore: Double? = null,
    @SerialName("confidence_interval") val confidenceInterval: Double? = null,
    @SerialName("votes") val votes: Long? = null,
    val rank: Int? = null,
    @SerialName("license") val license: String? = null,
    @SerialName("knowledge_cutoff") val knowledgeCutoff: String? = null,
    val modalities: List<String> = emptyList(),
    @SerialName("is_open_source") val isOpenSource: Boolean? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
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
 * Leaderboard response.
 */
@Serializable
data class LeaderboardResponse(
    val category: LeaderboardCategory,
    val entries: List<LeaderboardEntry> = emptyList(),
    @SerialName("last_updated") val lastUpdated: String? = null
)

/**
 * Model vote - upvote / downvote / tie / both bad.
 * Used in Battle Mode to vote on the better model.
 */
@Serializable
enum class VoteValue(val apiValue: String, val displayName: String) {
    @SerialName("upvote") MODEL_A_UPVOTE("model_a", "A is better"),
    @SerialName("downvote") MODEL_B_UPVOTE("model_b", "B is better"),
    @SerialName("tie") TIE("tie", "Tie"),
    @SerialName("bothbad") BOTH_BAD("bothbad", "Both are bad");

    companion object {
        fun fromApi(value: String?): VoteValue =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: TIE
    }
}
