package com.arena0077.app.webview

import android.util.Log
import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JsBridge - the @JavascriptInterface exposed to arena.ai's page loaded inside our WebView.
 *
 * arena.ai's own JavaScript calls these methods via `window.AndroidBridge.<method>(...)`.
 * We use this to:
 *   1. Receive events (stream chunks, history, votes) from the page.
 *   2. Receive auth state changes.
 *   3. Trigger haptic feedback / notifications.
 *
 * SECURITY: Every method MUST be on the main thread (annotated or default).
 * Never expose sensitive methods that the page could abuse.
 *
 * IMPORTANT: Methods are called from the WebView's JS thread.
 * Use a coroutine to forward to the rest of the app.
 */
@Singleton
class JsBridge @Inject constructor(
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    companion object {
        private const val TAG = "JsBridge"
        const val BRIDGE_NAME = "AndroidBridge"
    }

    /**
     * Called by the injected JS whenever the auth state changes.
     * The page detects auth state via Supabase's onAuthStateChange listener.
     */
    @JavascriptInterface
    fun onAuthStateChanged(payload: String) {
        Log.d(TAG, "onAuthStateChanged: $payload")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.AuthStateChanged.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse AuthStateChanged", it) }
        }
    }

    /**
     * Called by the injected JS after creating a new conversation.
     */
    @JavascriptInterface
    fun onConversationCreated(payload: String) {
        Log.d(TAG, "onConversationCreated: $payload")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.ConversationCreated.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse ConversationCreated", it) }
        }
    }

    /**
     * Called by the injected JS after loading the conversation history list.
     */
    @JavascriptInterface
    fun onHistoryLoaded(payload: String) {
        Log.d(TAG, "onHistoryLoaded (${payload.length} chars)")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.HistoryLoaded.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse HistoryLoaded", it) }
        }
    }

    /**
     * Called by the injected JS after loading messages for a conversation.
     */
    @JavascriptInterface
    fun onMessagesLoaded(payload: String) {
        Log.d(TAG, "onMessagesLoaded (${payload.length} chars)")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.MessagesLoaded.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse MessagesLoaded", it) }
        }
    }

    /**
     * Called by the injected JS when a stream starts.
     */
    @JavascriptInterface
    fun onStreamStarted(payload: String) {
        Log.d(TAG, "onStreamStarted: $payload")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.StreamStarted.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse StreamStarted", it) }
        }
    }

    /**
     * Called by the injected JS for each streamed text chunk.
     * High frequency - keep this fast.
     */
    @JavascriptInterface
    fun onStreamChunk(payload: String) {
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.StreamChunk.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse StreamChunk", it) }
        }
    }

    /**
     * Called by the injected JS when a stream completes.
     */
    @JavascriptInterface
    fun onStreamCompleted(payload: String) {
        Log.d(TAG, "onStreamCompleted: $payload")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.StreamCompleted.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse StreamCompleted", it) }
        }
    }

    /**
     * Called by the injected JS on stream error.
     * Special case: reCAPTCHA errors are flagged so the UI can show the
     * reCAPTCHA V2 widget.
     */
    @JavascriptInterface
    fun onStreamError(payload: String) {
        Log.e(TAG, "onStreamError: $payload")
        scope.launch {
            runCatching {
                val event = json.decodeFromString(ChatEvent.StreamError.serializer(), payload)
                _events.tryEmit(event)
            }.onFailure { Log.e(TAG, "Failed to parse StreamError", it) }
        }
    }

    @JavascriptInterface
    fun onVoteRegistered(payload: String) {
        Log.d(TAG, "onVoteRegistered: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.VoteRegistered.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse VoteRegistered", it) }
        }
    }

    @JavascriptInterface
    fun onModelsRevealed(payload: String) {
        Log.d(TAG, "onModelsRevealed: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.ModelsRevealed.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse ModelsRevealed", it) }
        }
    }

    @JavascriptInterface
    fun onImageGenerated(payload: String) {
        Log.d(TAG, "onImageGenerated: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.ImageGenerated.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse ImageGenerated", it) }
        }
    }

    @JavascriptInterface
    fun onVideoGenerated(payload: String) {
        Log.d(TAG, "onVideoGenerated: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.VideoGenerated.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse VideoGenerated", it) }
        }
    }

    @JavascriptInterface
    fun onWebDevPreview(payload: String) {
        Log.d(TAG, "onWebDevPreview: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.WebDevPreview.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse WebDevPreview", it) }
        }
    }

    @JavascriptInterface
    fun onAgentStep(payload: String) {
        Log.d(TAG, "onAgentStep: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.AgentStep.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse AgentStep", it) }
        }
    }

    @JavascriptInterface
    fun onRecaptchaChallenge(payload: String) {
        Log.w(TAG, "onRecaptchaChallenge: $payload")
        scope.launch {
            runCatching {
                _events.tryEmit(json.decodeFromString(ChatEvent.RecaptchaChallenge.serializer(), payload))
            }.onFailure { Log.e(TAG, "Failed to parse RecaptchaChallenge", it) }
        }
    }

    /**
     * General-purpose log channel from the injected JS.
     * Useful for debugging.
     */
    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "[JS] $message")
    }

    /**
     * Emit a raw event that doesn't fit any of the typed methods above.
     * The caller is responsible for parsing.
     */
    @JavascriptInterface
    fun emitEvent(eventType: String, payload: String) {
        Log.d(TAG, "emitEvent($eventType): ${payload.take(200)}")
    }
}
