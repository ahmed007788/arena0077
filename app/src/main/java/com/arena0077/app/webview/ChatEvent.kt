package com.arena0077.app.webview

import com.arena0077.app.data.models.Attachment
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Conversation
import com.arena0077.app.data.models.Message
import com.arena0077.app.data.models.Modality
import kotlinx.serialization.Serializable

/**
 * ChatEvent - sealed hierarchy of events streamed from the WebView back to native.
 *
 * The WebView posts these events via JsBridge.emitEvent(jsonString).
 * Native parses them and updates the ChatViewModel state.
 */
@Serializable
sealed class ChatEvent {
    abstract val conversationId: String?

    @Serializable
    data class AuthStateChanged(
        val isLoggedIn: Boolean,
        val email: String? = null,
        val userId: String? = null
    ) : ChatEvent() {
        override val conversationId: String? = null
    }

    @Serializable
    data class ConversationCreated(
        override val conversationId: String,
        val modality: Modality,
        val mode: BattleMode,
        val title: String? = null
    ) : ChatEvent()

    @Serializable
    data class HistoryLoaded(
        val items: List<HistoryItemDto> = emptyList()
    ) : ChatEvent() {
        override val conversationId: String? = null
    }

    @Serializable
    data class MessagesLoaded(
        override val conversationId: String,
        val messages: List<MessageDto> = emptyList()
    ) : ChatEvent()

    @Serializable
    data class StreamStarted(
        override val conversationId: String,
        val messageId: String,
        val modelLabel: String  // "A", "B", or model name in direct mode
    ) : ChatEvent()

    @Serializable
    data class StreamChunk(
        override val conversationId: String,
        val messageId: String,
        val delta: String,
        val modelLabel: String
    ) : ChatEvent()

    @Serializable
    data class StreamCompleted(
        override val conversationId: String,
        val messageId: String,
        val finalContent: String,
        val modelLabel: String
    ) : ChatEvent()

    @Serializable
    data class StreamError(
        override val conversationId: String,
        val messageId: String? = null,
        val message: String,
        val isRecaptchaError: Boolean = false
    ) : ChatEvent()

    @Serializable
    data class VoteRegistered(
        override val conversationId: String,
        val voteValue: String
    ) : ChatEvent()

    @Serializable
    data class ModelsRevealed(
        override val conversationId: String,
        val modelAName: String,
        val modelAOrganization: String? = null,
        val modelBName: String,
        val modelBOrganization: String? = null
    ) : ChatEvent()

    @Serializable
    data class ImageGenerated(
        override val conversationId: String,
        val messageId: String,
        val imageUrl: String,
        val modelLabel: String
    ) : ChatEvent()

    @Serializable
    data class VideoGenerated(
        override val conversationId: String,
        val messageId: String,
        val videoUrl: String,
        val thumbnailUrl: String? = null,
        val modelLabel: String
    ) : ChatEvent()

    @Serializable
    data class WebDevPreview(
        override val conversationId: String,
        val messageId: String,
        val previewUrl: String,
        val modelLabel: String
    ) : ChatEvent()

    @Serializable
    data class AgentStep(
        override val conversationId: String,
        val messageId: String,
        val stepNumber: Int,
        val action: String,
        val result: String? = null
    ) : ChatEvent()

    @Serializable
    data class RecaptchaChallenge(
        override val conversationId: String?,
        val reason: String,
        val sitekey: String
    ) : ChatEvent()
}

// DTOs for serialization across the JS bridge
@Serializable
data class HistoryItemDto(
    val id: String,
    val title: String,
    val modality: String = "chat",
    val mode: String = "battle",
    val createdAt: String,
    val updatedAt: String? = null,
    val isArchived: Boolean = false
)

@Serializable
data class MessageDto(
    val id: String,
    val role: String,
    val content: String,
    val modelId: String? = null,
    val modelName: String? = null,
    val modelOrganization: String? = null,
    val modelLabel: String? = null,
    val createdAt: String? = null,
    val isError: Boolean = false,
    val attachments: List<AttachmentDto> = emptyList()
)

@Serializable
data class AttachmentDto(
    val id: String,
    val type: String,
    val url: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val thumbnailUrl: String? = null
)
