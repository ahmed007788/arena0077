package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Conversation / Evaluation Session - matches the EXACT shape returned by
 * GET /api/evaluation/{conversationId} on arena.ai.
 *
 * Captured 2026-07-11 from production traffic.
 */
@Serializable
data class Conversation(
    val id: String,
    @SerialName("userId") val userId: String? = null,
    val title: String? = null,
    val mode: String = "battle",  // battle|side|direct|direct-battle|agent
    val visibility: String = "public",
    @SerialName("lastMessageIds") val lastMessageIds: List<String> = emptyList(),
    @SerialName("archivedAt") val archivedAt: String? = null,
    @SerialName("deletedAt") val deletedAt: String? = null,
    @SerialName("deletionPendingProcessing") val deletionPendingProcessing: Boolean = false,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val messages: List<Message> = emptyList()
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "New Chat"

    val battleMode: BattleMode
        get() = when (mode) {
            "battle" -> BattleMode.BATTLE
            "side" -> BattleMode.SIDE
            "direct", "direct-battle" -> BattleMode.DIRECT
            "agent" -> BattleMode.AGENT
            else -> BattleMode.BATTLE
        }
}

/**
 * Chat message - matches the EXACT shape from GET /api/evaluation/{id}.messages[]
 */
@Serializable
data class Message(
    val id: String,
    @SerialName("evaluationSessionId") val evaluationSessionId: String? = null,
    @SerialName("evaluationId") val evaluationId: String? = null,
    val role: String,  // user|assistant
    @SerialName("parentMessageIds") val parentMessageIds: List<String> = emptyList(),
    val content: String,
    @SerialName("experimental_attachments") val experimentalAttachments: List<Attachment> = emptyList(),
    @SerialName("modelId") val modelId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("participantPosition") val participantPosition: String? = null,  // a|b
    val status: String = "success",  // success|error|streaming
    @SerialName("failureReason") val failureReason: String? = null,
    val metadata: JsonElement? = null,
    @SerialName("is_streaming") val isStreaming: Boolean = false,
    @SerialName("is_error") val isError: Boolean = false,
    val error: String? = null
) {
    val isUser: Boolean get() = role == "user"
    val isModelA: Boolean get() = participantPosition == "a" && role != "user"
    val isModelB: Boolean get() = participantPosition == "b" && role != "user"
    val modelLabel: String get() = when {
        isUser -> "You"
        isModelA -> "Model A"
        isModelB -> "Model B"
        else -> "Assistant"
    }
}

/**
 * File attachment - matches experimental_attachments[] shape
 */
@Serializable
data class Attachment(
    val id: String = "",
    val type: String = "file",
    val url: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

/**
 * Conversation history item (sidebar list entry) - matches /api/history/unified entries[]
 */
@Serializable
data class HistoryItem(
    val id: String,
    val title: String,
    val modality: String = "chat",
    val mode: String = "battle",
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("archivedAt") val archivedAt: String? = null
) {
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() } ?: "New Chat"

    val modalityEnum: Modality
        get() = Modality.fromApi(modality)

    val battleModeEnum: BattleMode
        get() = when (mode) {
            "battle" -> BattleMode.BATTLE
            "side" -> BattleMode.SIDE
            "direct", "direct-battle" -> BattleMode.DIRECT
            "agent" -> BattleMode.AGENT
            else -> BattleMode.BATTLE
        }
}

/**
 * Unified history response from GET /api/history/unified
 */
@Serializable
data class HistoryResponse(
    val entries: List<HistoryItem> = emptyList(),
    val pagination: Pagination = Pagination()
)

@Serializable
data class Pagination(
    val hasMore: Boolean = false,
    val cursor: String? = null,
    val limit: Int = 20
)
