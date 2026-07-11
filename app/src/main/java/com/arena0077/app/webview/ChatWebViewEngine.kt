package com.arena0077.app.webview

import android.content.Context
import android.view.View
import android.webkit.WebView
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Modality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatWebViewEngine - the high-level interface for chat operations.
 *
 * Wraps a hidden WebView that loads arena.ai and exposes a clean API:
 *
 *   - sendMessage(prompt, modality, mode, conversationId)
 *   - stop(conversationId, messageId)
 *   - vote(conversationId, value)
 *   - openConversation(id)
 *   - newChat()
 *   - refreshHistory()
 *   - login(email, password)
 *   - signOut()
 *
 * Events flow back via [events] as a SharedFlow<ChatEvent>.
 *
 * The WebView is lazily created on first use and kept alive for the
 * lifetime of the singleton. This is critical because:
 *   1. Re-creating the WebView loses arena.ai's auth cookies.
 *   2. Re-loading the page is slow (reCAPTCHA Enterprise bootstrap).
 *   3. We want continuous streaming across UI recompositions.
 *
 * Threading: WebView must be created and called on the main thread.
 */
@Singleton
class ChatWebViewEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webClient: ArenaWebViewClient
) {
    private var webView: WebView? = null

    /**
     * Stream of events from the WebView. Collected by the ChatViewModel.
     */
    val events: SharedFlow<ChatEvent> = webClient.events

    /**
     * Lazily initialize the WebView on first use.
     * MUST be called on the main thread.
     */
    fun ensureInitialized(): WebView {
        webView?.let { return it }
        return synchronized(this) {
            webView?.let { return it }
            val wv = WebView(context).apply {
                // Hidden - we use this purely as a chat engine.
                // We still keep it at 1x1 px so JS runs and timers fire.
                layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
                visibility = View.VISIBLE  // Must be VISIBLE for JS to run
            }
            webClient.configure(wv)
            webClient.loadArena(wv)
            webView = wv
            wv
        }
    }

    fun sendMessage(
        prompt: String,
        modality: Modality,
        mode: BattleMode,
        conversationId: String? = null
    ) {
        val wv = webView ?: return
        webClient.sendMessage(wv, prompt, modality, mode, conversationId)
    }

    fun stop(conversationId: String? = null, messageId: String? = null) {
        val wv = webView ?: return
        webClient.stopStream(wv, conversationId, messageId)
    }

    fun vote(conversationId: String, value: String) {
        val wv = webView ?: return
        webClient.vote(wv, conversationId, value)
    }

    fun openConversation(id: String) {
        val wv = webView ?: return
        webClient.openConversation(wv, id)
    }

    fun newChat() {
        val wv = webView ?: return
        webClient.newChat(wv)
    }

    fun refreshHistory() {
        val wv = webView ?: return
        webClient.refreshHistory(wv)
    }

    fun login(email: String, password: String) {
        val wv = ensureInitialized()
        webClient.loginWithEmail(wv, email, password)
    }

    fun signOut() {
        val wv = webView ?: return
        webClient.signOut(wv)
    }

    /**
     * Reload arena.ai - useful after auth state changes.
     */
    fun reload() {
        webView?.reload()
    }

    /**
     * Cleanup - call when the host Activity is destroyed.
     */
    fun destroy() {
        webView?.apply {
            loadUrl("about:blank")
            clearHistory()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        webView = null
    }
}
