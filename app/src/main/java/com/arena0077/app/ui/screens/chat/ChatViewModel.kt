package com.arena0077.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.chat.ChatRepository
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Conversation
import com.arena0077.app.data.models.HistoryItem
import com.arena0077.app.data.models.Message
import com.arena0077.app.data.models.MessageRole
import com.arena0077.app.data.models.Modality
import com.arena0077.app.data.models.QuickAction
import com.arena0077.app.webview.ChatEvent
import com.arena0077.app.webview.ChatWebViewEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ChatViewModel - the bridge between the ChatWebViewEngine and Compose UI.
 *
 * Responsibilities:
 *   1. Hold chat UI state (conversation list, current messages, modality, mode).
 *   2. Collect ChatEvent from the WebView engine and update state.
 *   3. Expose commands: sendMessage, stop, vote, openConversation, newChat.
 *   4. Manage loading / error / streaming flags.
 *
 * The ViewModel survives configuration changes and is scoped to the Activity.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val engine: ChatWebViewEngine,
    private val chatRepo: ChatRepository,
    val authManager: AuthManager
) : ViewModel() {

    // ---------------- Public UI state ----------------

    val isLoggedIn: StateFlow<Boolean> = authManager.session
        .let { session ->
            MutableStateFlow(session.value?.isValid == true)
                .apply {
                    viewModelScope.launch {
                        session.collect { s ->
                            value = s?.isValid == true
                        }
                    }
                }
        }.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _modality = MutableStateFlow(Modality.CHAT)
    val modality: StateFlow<Modality> = _modality.asStateFlow()

    private val _battleMode = MutableStateFlow(BattleMode.BATTLE)
    val battleMode: StateFlow<BattleMode> = _battleMode.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // Streaming message id tracking (for delta updates)
    private val streamingMessages = mutableMapOf<String, Message>()  // key: modelLabel
    private var eventsCollector: Job? = null

    init {
        startCollectingEvents()
    }

    private fun startCollectingEvents() {
        eventsCollector?.cancel()
        eventsCollector = viewModelScope.launch {
            engine.events.collect { event -> handleEvent(event) }
        }
    }

    // ---------------- Event handling ----------------

    private fun handleEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.AuthStateChanged -> {
                // Auth state changed in the WebView
            }

            is ChatEvent.ConversationCreated -> {
                _currentConversation.value = Conversation(
                    id = event.conversationId,
                    title = event.title,
                    modality = event.modality,
                    mode = event.mode,
                    createdAt = System.currentTimeMillis().toString()
                )
                _isSending.value = false
            }

            is ChatEvent.HistoryLoaded -> {
                _history.value = event.items.map { dto ->
                    HistoryItem(
                        id = dto.id,
                        title = dto.title,
                        modality = com.arena0077.app.data.models.Modality.fromApi(dto.modality),
                        mode = com.arena0077.app.data.models.BattleMode.fromApi(dto.mode),
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isArchived = dto.isArchived
                    )
                }
            }

            is ChatEvent.MessagesLoaded -> {
                _messages.value = event.messages.map { it.toMessage() }
            }

            is ChatEvent.StreamStarted -> {
                _isStreaming.value = true
                val key = event.messageId
                val newMsg = Message(
                    id = event.messageId,
                    role = if (event.modelLabel.equals("A", true) || event.modelLabel.equals("B", true))
                        MessageRole.MODEL_A else MessageRole.ASSISTANT,
                    content = "",
                    createdAt = System.currentTimeMillis().toString(),
                    isStreaming = true
                )
                streamingMessages[key] = newMsg
                _messages.value = _messages.value + newMsg
            }

            is ChatEvent.StreamChunk -> {
                val key = event.messageId
                val existing = streamingMessages[key] ?: return
                val updated = existing.copy(content = existing.content + event.delta)
                streamingMessages[key] = updated
                _messages.value = _messages.value.map { if (it.id == key) updated else it }
            }

            is ChatEvent.StreamCompleted -> {
                val key = event.messageId
                val existing = streamingMessages.remove(key)
                val finalContent = if (event.finalContent.isNotEmpty()) event.finalContent
                                   else existing?.content ?: ""
                val finalMsg = (existing ?: Message(
                    id = key,
                    role = MessageRole.ASSISTANT,
                    content = finalContent
                )).copy(content = finalContent, isStreaming = false)
                _messages.value = _messages.value.map { if (it.id == key) finalMsg else it }
                if (streamingMessages.isEmpty()) _isStreaming.value = false
            }

            is ChatEvent.StreamError -> {
                _isStreaming.value = false
                _isSending.value = false
                streamingMessages.clear()
                _error.value = if (event.isRecaptchaError) {
                    "reCAPTCHA verification required. Please complete the challenge."
                } else {
                    event.message
                }
            }

            is ChatEvent.ImageGenerated -> {
                val key = event.messageId
                val msg = Message(
                    id = key,
                    role = MessageRole.ASSISTANT,
                    content = "![Generated Image](${event.imageUrl})",
                    modelName = event.modelLabel,
                    createdAt = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.VideoGenerated -> {
                val key = event.messageId
                val msg = Message(
                    id = key,
                    role = MessageRole.ASSISTANT,
                    content = "[Video](${event.videoUrl})",
                    modelName = event.modelLabel,
                    createdAt = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.WebDevPreview -> {
                val key = event.messageId
                val msg = Message(
                    id = key,
                    role = MessageRole.ASSISTANT,
                    content = "Preview: ${event.previewUrl}",
                    modelName = event.modelLabel,
                    createdAt = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.AgentStep -> {
                val key = "${event.messageId}-step-${event.stepNumber}"
                val msg = Message(
                    id = key,
                    role = MessageRole.ASSISTANT,
                    content = "Step ${event.stepNumber}: ${event.action}" +
                              (event.result?.let { "\n→ $it" } ?: ""),
                    modelName = "Agent",
                    createdAt = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.VoteRegistered -> { /* UI updates via state */ }
            is ChatEvent.ModelsRevealed -> { /* Update messages with model names */ }
            is ChatEvent.RecaptchaChallenge -> {
                _error.value = "reCAPTCHA challenge required: ${event.reason}"
            }
        }
    }

    // ---------------- Public commands ----------------

    fun ensureEngineInitialized() {
        // Must be called from the main thread (Composable)
        engine.ensureInitialized()
    }

    fun setModality(m: Modality) { _modality.value = m }
    fun setBattleMode(m: BattleMode) { _battleMode.value = m }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        _error.value = null
        _isSending.value = true

        // Add user message to UI immediately
        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = prompt,
            createdAt = System.currentTimeMillis().toString()
        )
        _messages.value = _messages.value + userMsg

        // Send via WebView (handles reCAPTCHA automatically)
        engine.sendMessage(
            prompt = prompt,
            modality = _modality.value,
            mode = _battleMode.value,
            conversationId = _currentConversation.value?.id
        )
    }

    fun applyQuickAction(action: QuickAction) {
        setModality(action.modality)
        sendMessage(action.prompt)
    }

    fun stop() {
        engine.stop(
            conversationId = _currentConversation.value?.id,
            messageId = streamingMessages.keys.firstOrNull()
        )
        _isStreaming.value = false
        _isSending.value = false
    }

    fun vote(value: String) {
        val convId = _currentConversation.value?.id ?: return
        engine.vote(convId, value)
    }

    fun openConversation(id: String) {
        engine.openConversation(id)
        _messages.value = emptyList()
    }

    fun newChat() {
        engine.newChat()
        _currentConversation.value = null
        _messages.value = emptyList()
        _error.value = null
    }

    fun refreshHistory() {
        engine.refreshHistory()
    }

    fun login(email: String, password: String) {
        _isAuthenticating.value = true
        _error.value = null
        engine.login(email, password)
        // The AuthStateChanged event will fire when login completes
        viewModelScope.launch {
            // Wait a bit for login to process
            kotlinx.coroutines.delay(2000)
            _isAuthenticating.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            engine.signOut()
            chatRepo.signOut()
            _currentConversation.value = null
            _messages.value = emptyList()
            _history.value = emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        eventsCollector?.cancel()
        // Don't destroy the engine here - it's a singleton
    }
}

// Extension to convert MessageDto to Message
private fun com.arena0077.app.webview.MessageDto.toMessage(): Message = Message(
    id = id,
    role = when (role.lowercase()) {
        "user" -> MessageRole.USER
        "assistant" -> MessageRole.ASSISTANT
        "system" -> MessageRole.SYSTEM
        "model_a" -> MessageRole.MODEL_A
        "model_b" -> MessageRole.MODEL_B
        else -> MessageRole.ASSISTANT
    },
    content = content,
    modelId = modelId,
    modelName = modelName,
    modelOrganization = modelOrganization,
    createdAt = createdAt,
    isError = isError
)
