package com.arena0077.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.chat.ChatRepository
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Conversation
import com.arena0077.app.data.models.HistoryItem
import com.arena0077.app.data.models.Message
import com.arena0077.app.data.models.Modality
import com.arena0077.app.data.models.QuickAction
import com.arena0077.app.webview.ChatEvent
import com.arena0077.app.webview.ChatWebViewEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val engine: ChatWebViewEngine,
    private val chatRepo: ChatRepository,
    val authManager: AuthManager
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

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

    private val streamingMessages = mutableMapOf<String, Message>()
    private var eventsCollector: Job? = null

    init {
        startCollectingEvents()
        // Observe auth state
        viewModelScope.launch {
            authManager.session.collect { s ->
                _isLoggedIn.value = s?.isValid == true
            }
        }
    }

    private fun startCollectingEvents() {
        eventsCollector?.cancel()
        eventsCollector = viewModelScope.launch {
            engine.events.collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.AuthStateChanged -> {
                _isLoggedIn.value = event.isLoggedIn
            }

            is ChatEvent.ConversationCreated -> {
                _currentConversation.value = Conversation(
                    id = event.conversationId,
                    title = event.title,
                    mode = event.mode.apiValue,
                    createdAt = System.currentTimeMillis().toString()
                )
                _isSending.value = false
            }

            is ChatEvent.HistoryLoaded -> {
                _history.value = event.items.map { dto ->
                    HistoryItem(
                        id = dto.id,
                        title = dto.title,
                        modality = dto.modality,
                        mode = dto.mode,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        archivedAt = null
                    )
                }
            }

            is ChatEvent.MessagesLoaded -> {
                _messages.value = event.messages.map { dto ->
                    Message(
                        id = dto.id,
                        role = dto.role,
                        content = dto.content,
                        modelId = dto.modelId,
                        createdAt = dto.createdAt,
                        participantPosition = dto.modelLabel?.lowercase(),
                        status = if (dto.isError) "error" else "success",
                        isError = dto.isError
                    )
                }
            }

            is ChatEvent.StreamStarted -> {
                _isStreaming.value = true
                val key = event.messageId
                val newMsg = Message(
                    id = event.messageId,
                    role = "assistant",
                    content = "",
                    createdAt = System.currentTimeMillis().toString(),
                    participantPosition = event.modelLabel.lowercase(),
                    status = "streaming",
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
                    role = "assistant",
                    content = finalContent
                )).copy(content = finalContent, isStreaming = false, status = "success")
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
                val msg = Message(
                    id = event.messageId,
                    role = "assistant",
                    content = "![Generated Image](${event.imageUrl})",
                    createdAt = System.currentTimeMillis().toString(),
                    participantPosition = event.modelLabel.lowercase()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.VideoGenerated -> {
                val msg = Message(
                    id = event.messageId,
                    role = "assistant",
                    content = "[Video](${event.videoUrl})",
                    createdAt = System.currentTimeMillis().toString(),
                    participantPosition = event.modelLabel.lowercase()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.WebDevPreview -> {
                val msg = Message(
                    id = event.messageId,
                    role = "assistant",
                    content = "Preview: ${event.previewUrl}",
                    createdAt = System.currentTimeMillis().toString(),
                    participantPosition = event.modelLabel.lowercase()
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.AgentStep -> {
                val msg = Message(
                    id = "${event.messageId}-step-${event.stepNumber}",
                    role = "assistant",
                    content = "Step ${event.stepNumber}: ${event.action}" +
                              (event.result?.let { "\n→ $it" } ?: ""),
                    createdAt = System.currentTimeMillis().toString(),
                    participantPosition = "agent"
                )
                _messages.value = _messages.value + msg
            }

            is ChatEvent.VoteRegistered -> { }
            is ChatEvent.ModelsRevealed -> { }
            is ChatEvent.RecaptchaChallenge -> {
                _error.value = "reCAPTCHA challenge required: ${event.reason}"
            }
        }
    }

    fun ensureEngineInitialized() {
        engine.ensureInitialized()
    }

    fun setModality(m: Modality) { _modality.value = m }
    fun setBattleMode(m: BattleMode) { _battleMode.value = m }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        _error.value = null
        _isSending.value = true

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = prompt,
            createdAt = System.currentTimeMillis().toString()
        )
        _messages.value = _messages.value + userMsg

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
        viewModelScope.launch {
            chatRepo.loadConversation(id).onSuccess { conv ->
                _currentConversation.value = conv
                _messages.value = conv.messages
            }
        }
    }

    fun newChat() {
        engine.newChat()
        _currentConversation.value = null
        _messages.value = emptyList()
        _error.value = null
    }

    fun refreshHistory() {
        engine.refreshHistory()
        viewModelScope.launch {
            chatRepo.loadHistory().onSuccess { resp ->
                _history.value = resp.entries
            }
        }
    }

    fun login(email: String, password: String) {
        _isAuthenticating.value = true
        _error.value = null
        engine.login(email, password)
        viewModelScope.launch {
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
            _isLoggedIn.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        eventsCollector?.cancel()
    }
}
