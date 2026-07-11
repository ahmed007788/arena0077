package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Conversation / Evaluation Session.
 *
 * In arena.ai's data model, every chat is an "evaluation session".
 * The URL pattern is /c/{evaluationSessionId}.
 *
 * Extracted from the /api/history/unified response shape.
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String? = null,
    val modality: Modality = Modality.CHAT,
    val mode: BattleMode = BattleMode.BATTLE,
    @SerialName("model_a_id") val modelAId: String? = null,
    @SerialName("model_b_id") val modelBId: String? = null,
    @SerialName("model_a_organization") val modelAOrganization: String? = null,
    @SerialName("model_b_organization") val modelBOrganization: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("last_message_ids") val lastMessageIds: List<String> = emptyList(),
    @SerialName("is_archived") val isArchived: Boolean = false,
    val messages: List<Message> = emptyList()
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "New Chat"
}

/**
 * Chat message - both user prompts and model responses.
 */
@Serializable
data class Message(
    val id: String,
    val role: MessageRole,
    val content: String,
    @SerialName("model_id") val modelId: String? = null,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("model_organization") val modelOrganization: String? = null,
    @SerialName("evaluation_session_id") val evaluationSessionId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val metadata: JsonElement? = null,
    @SerialName("is_streaming") val isStreaming: Boolean = false,
    @SerialName("is_error") val isError: Boolean = false,
    val error: String? = null
)

@Serializable
enum class MessageRole {
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
    @SerialName("system") SYSTEM,
    @SerialName("model_a") MODEL_A,
    @SerialName("model_b") MODEL_B
}

/**
 * File attachment - images, PDFs, code files attached to a message.
 */
@Serializable
data class Attachment(
    val id: String,
    val type: AttachmentType,
    val url: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

@Serializable
enum class AttachmentType {
    @SerialName("image") IMAGE,
    @SerialName("file") FILE,
    @SerialName("code") CODE
}

/**
 * Conversation history item (sidebar list entry).
 */
@Serializable
data class HistoryItem(
    val id: String,
    val title: String,
    val modality: Modality = Modality.CHAT,
    val mode: BattleMode = BattleMode.BATTLE,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false
)

/**
 * Unified history response from /api/history/unified.
 */
@Serializable
data class HistoryResponse(
    val items: List<HistoryItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)
