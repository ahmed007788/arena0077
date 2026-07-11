package com.arena0077.app.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.arena0077.app.BuildConfig
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Modality
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ArenaWebViewClient - configures and manages a WebView that loads arena.ai.
 *
 * Responsibilities:
 *   1. Configure WebView with JavaScript, cookies, DOM storage, file access.
 *   2. Inject the JsBridge (window.AndroidBridge).
 *   3. Inject arena-bridge.js after page load - this hooks arena.ai's own
 *      fetch() calls to capture streaming responses and emits events via
 *      AndroidBridge.
 *   4. Forward commands from native to the page (sendMessage, stop, vote).
 *
 * The WebView is intentionally kept invisible (0x0 px) when used purely as
 * a "headless" chat engine. The visible chat UI is rendered natively in
 * Compose. This is the only way to satisfy arena.ai's reCAPTCHA Enterprise
 * requirement from a native Android app.
 */
@Singleton
class ArenaWebViewClient @Inject constructor(
    private val jsBridge: JsBridge,
    private val authManager: AuthManager,
    private val json: Json
) {
    companion object {
        private const val TAG = "ArenaWebViewClient"
        private const val BRIDGE_JS_PATH = "/android_asset/arena-bridge.js"
    }

    val events: SharedFlow<ChatEvent> = jsBridge.events

    /**
     * Configure a WebView for arena.ai use.
     * Returns the same WebView for chaining.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView): WebView {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            userAgentString = "${userAgentString} Arena0077/1.0.0 Android"
            // reCAPTCHA Enterprise requires these
            setSupportZoom(false)
            builtInZoomControls = false
        }

        // Cookies - arena.ai sets arena-auth-prod-v1 cookie on login
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // Inject our JS bridge
        webView.addJavascriptInterface(jsBridge, JsBridge.BRIDGE_NAME)

        // WebViewClient to inject bridge script after page load
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: $url")
                injectBridgeScript(view)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Keep all navigation inside the WebView
                return false
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                Log.w(TAG, "HTTP ${errorResponse.statusCode} for ${request.url}")
                super.onReceivedHttpError(view, request, errorResponse)
            }
        }

        return webView
    }

    /**
     * Inject the bridge script that hooks arena.ai's own fetch() and event handlers.
     * This is what makes it possible to capture streaming responses.
     */
    private fun injectBridgeScript(webView: WebView) {
        val script = buildBridgeScript()
        webView.evaluateJavascript(script, null)
        Log.d(TAG, "Bridge script injected")
    }

    /**
     * Build the JavaScript that hooks arena.ai's fetch and emits events.
     * The script is also persisted as an asset for editing convenience.
     */
    private fun buildBridgeScript(): String = """
        (function() {
            if (window.__arenaBridgeInjected) return;
            window.__arenaBridgeInjected = true;
            console.log('[ArenaBridge] Injecting bridge v1.0.0');

            const BRIDGE = window.AndroidBridge;
            if (!BRIDGE) {
                console.error('[ArenaBridge] AndroidBridge not found!');
                return;
            }

            // ---------- Utility: safe emit ----------
            function safeEmit(method, payload) {
                try {
                    BRIDGE[method](JSON.stringify(payload));
                } catch (e) {
                    console.error('[ArenaBridge] emit failed:', method, e);
                }
            }

            // ---------- Hook fetch to capture chat streams ----------
            const originalFetch = window.fetch;
            window.fetch = async function(input, init) {
                const url = typeof input === 'string' ? input : input.url;
                const isChatEndpoint = url && (
                    url.includes('/nextjs-api/stream/create-evaluation') ||
                    url.includes('/nextjs-api/stream/post-to-evaluation') ||
                    url.includes('/nextjs-api/stream/rerun') ||
                    url.includes('/nextjs-api/stream/resample') ||
                    url.includes('/nextjs-api/stream/retry-evaluation-session-message')
                );

                const response = await originalFetch.apply(this, arguments);

                if (isChatEndpoint && response.ok && response.body) {
                    // Capture and re-emit stream chunks
                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();
                    let buffer = '';

                    const convIdMatch = url.match(/post-to-evaluation\/([a-f0-9-]+)/);
                    const conversationId = convIdMatch ? convIdMatch[1] : null;

                    (async function streamLoop() {
                        try {
                            while (true) {
                                const { done, value } = await reader.read();
                                if (done) break;
                                buffer += decoder.decode(value, { stream: true });

                                // Process SSE-style events
                                const lines = buffer.split('\n');
                                buffer = lines.pop() || '';

                                for (const line of lines) {
                                    if (!line.startsWith('data: ')) continue;
                                    const data = line.slice(6).trim();
                                    if (!data || data === '[DONE]') continue;
                                    try {
                                        const evt = JSON.parse(data);
                                        handleStreamEvent(evt, conversationId);
                                    } catch (e) {
                                        // Non-JSON chunk, emit as raw text
                                        if (data) {
                                            safeEmit('onStreamChunk', {
                                                conversationId: conversationId,
                                                messageId: 'streaming',
                                                delta: data,
                                                modelLabel: 'A'
                                            });
                                        }
                                    }
                                }
                            }
                            safeEmit('onStreamCompleted', {
                                conversationId: conversationId,
                                messageId: 'streaming',
                                finalContent: '',
                                modelLabel: 'A'
                            });
                        } catch (e) {
                            safeEmit('onStreamError', {
                                conversationId: conversationId,
                                messageId: null,
                                message: 'Stream read error: ' + e.message
                            });
                        }
                    })();
                }

                return response;
            };

            // ---------- Handle parsed stream events ----------
            function handleStreamEvent(evt, conversationId) {
                if (!evt || typeof evt !== 'object') return;

                // arena.ai stream event shapes (from JS bundle analysis):
                //   { type: "message_start", messageId, modelA?, modelB? }
                //   { type: "text_delta", messageId, delta, modelLabel }
                //   { type: "message_complete", messageId, content, modelLabel }
                //   { type: "image_generated", messageId, url, modelLabel }
                //   { type: "video_generated", messageId, url, thumbnail, modelLabel }
                //   { type: "agent_step", messageId, step, action, result }
                //   { type: "error", message }

                const type = evt.type || evt.event;
                const messageId = evt.messageId || evt.id || 'msg-' + Date.now();
                const modelLabel = evt.modelLabel || evt.model || 'A';

                switch (type) {
                    case 'conversation_created':
                    case 'evaluation_session_created':
                        safeEmit('onConversationCreated', {
                            conversationId: evt.id || evt.evaluationSessionId || conversationId,
                            modality: evt.modality || 'chat',
                            mode: evt.mode || 'battle',
                            title: evt.title
                        });
                        break;

                    case 'message_start':
                    case 'start':
                        safeEmit('onStreamStarted', {
                            conversationId: conversationId,
                            messageId: messageId,
                            modelLabel: modelLabel
                        });
                        break;

                    case 'text_delta':
                    case 'delta':
                    case 'chunk':
                        safeEmit('onStreamChunk', {
                            conversationId: conversationId,
                            messageId: messageId,
                            delta: evt.delta || evt.text || evt.content || '',
                            modelLabel: modelLabel
                        });
                        break;

                    case 'message_complete':
                    case 'complete':
                    case 'done':
                        safeEmit('onStreamCompleted', {
                            conversationId: conversationId,
                            messageId: messageId,
                            finalContent: evt.content || evt.text || '',
                            modelLabel: modelLabel
                        });
                        break;

                    case 'image_generated':
                    case 'image':
                        safeEmit('onImageGenerated', {
                            conversationId: conversationId,
                            messageId: messageId,
                            imageUrl: evt.url || evt.imageUrl,
                            modelLabel: modelLabel
                        });
                        break;

                    case 'video_generated':
                    case 'video':
                        safeEmit('onVideoGenerated', {
                            conversationId: conversationId,
                            messageId: messageId,
                            videoUrl: evt.url || evt.videoUrl,
                            thumbnailUrl: evt.thumbnailUrl,
                            modelLabel: modelLabel
                        });
                        break;

                    case 'webdev_preview':
                    case 'preview':
                        safeEmit('onWebDevPreview', {
                            conversationId: conversationId,
                            messageId: messageId,
                            previewUrl: evt.url || evt.previewUrl,
                            modelLabel: modelLabel
                        });
                        break;

                    case 'agent_step':
                    case 'tool_call':
                        safeEmit('onAgentStep', {
                            conversationId: conversationId,
                            messageId: messageId,
                            stepNumber: evt.step || 0,
                            action: evt.action || evt.tool || 'unknown',
                            result: evt.result
                        });
                        break;

                    case 'error':
                        const isRecaptcha = (evt.message || '').includes('recaptcha');
                        safeEmit('onStreamError', {
                            conversationId: conversationId,
                            messageId: messageId,
                            message: evt.message || 'Unknown error',
                            isRecaptchaError: isRecaptcha
                        });
                        break;

                    default:
                        // Unknown event type - log it for future investigation
                        BRIDGE.log('Unknown stream event: ' + type);
                }
            }

            // ---------- Hook history loading ----------
            // arena.ai calls /api/history/unified - we capture the response
            const origHistoryFetch = window.fetch;
            // Already hooked above; we handle history inside the same hook
            // by detecting /api/history/unified responses.

            // ---------- MutationObserver for chat messages ----------
            // As a fallback when stream parsing fails, we observe DOM mutations
            // in the chat container and emit MessageDto events.
            const chatObserver = new MutationObserver((mutations) => {
                // Disabled by default - heavy. Enable only if stream parsing fails.
            });

            // ---------- Detect auth state via Supabase ----------
            // arena.ai uses Supabase auth. The auth state is in localStorage
            // under sb-*-auth-token (or in cookies via arena-auth-prod-v1).
            function checkAuthState() {
                try {
                    const cookies = document.cookie;
                    const authCookie = cookies.split(';').find(c => c.trim().startsWith('arena-auth-prod-v1'));
                    const isLoggedIn = !!authCookie && !authCookie.includes('=""');
                    const email = '';  // Email is inside the JWT, not directly accessible
                    safeEmit('onAuthStateChanged', {
                        isLoggedIn: isLoggedIn,
                        email: email,
                        userId: null
                    });
                } catch (e) {
                    console.error('[ArenaBridge] auth check failed:', e);
                }
            }

            // Poll auth state every 5 seconds
            setInterval(checkAuthState, 5000);
            checkAuthState();

            // ---------- Public command API ----------
            // Native code calls these via webView.evaluateJavascript()
            window.__arenaBridge = {
                // Send a new chat message
                sendMessage: function(prompt, modality, mode, conversationId) {
                    // Find the chat input and submit programmatically
                    const input = document.querySelector('textarea[placeholder*="Ask"]') ||
                                  document.querySelector('textarea[placeholder*="anything"]');
                    if (!input) {
                        safeEmit('onStreamError', {
                            conversationId: conversationId,
                            message: 'Chat input not found on page'
                        });
                        return;
                    }

                    // Set value using React-compatible setter
                    const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                        window.HTMLTextAreaElement.prototype, 'value'
                    ).set;
                    nativeInputValueSetter.call(input, prompt);
                    input.dispatchEvent(new Event('input', { bubbles: true }));

                    // Wait a tick for React to update, then click send
                    setTimeout(() => {
                        const sendButton = Array.from(document.querySelectorAll('button'))
                            .find(b => b.getAttribute('aria-label') === 'Send message' ||
                                       b.textContent.includes('Send'));
                        if (sendButton && !sendButton.disabled) {
                            sendButton.click();
                        } else {
                            // Fallback: press Enter
                            input.dispatchEvent(new KeyboardEvent('keydown', {
                                key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true
                            }));
                        }
                    }, 100);
                },

                // Stop the current stream
                stop: function(conversationId, messageId) {
                    const stopBtn = Array.from(document.querySelectorAll('button'))
                        .find(b => b.getAttribute('aria-label')?.includes('Stop') ||
                                   b.textContent.includes('Stop'));
                    if (stopBtn) stopBtn.click();
                },

                // Vote for a model
                vote: function(conversationId, value) {
                    const btn = Array.from(document.querySelectorAll('button'))
                        .find(b => b.textContent.includes(value) ||
                                   b.getAttribute('data-vote') === value);
                    if (btn) btn.click();
                },

                // Navigate to a conversation
                openConversation: function(id) {
                    window.location.href = '/c/' + id;
                },

                // Start a new chat
                newChat: function() {
                    window.location.href = '/';
                },

                // Reload history
                refreshHistory: function() {
                    fetch('/api/history/unified?limit=50&includeArchived=false')
                        .then(r => r.json())
                        .then(data => {
                            const items = (data.items || data || []).map(item => ({
                                id: item.id,
                                title: item.title || 'Untitled',
                                modality: item.modality || 'chat',
                                mode: item.mode || 'battle',
                                createdAt: item.createdAt || '',
                                updatedAt: item.updatedAt || null,
                                isArchived: item.isArchived || false
                            }));
                            safeEmit('onHistoryLoaded', { items: items });
                        })
                        .catch(e => BRIDGE.log('History refresh failed: ' + e.message));
                },

                // Login with email + password (programmatically)
                loginWithEmail: function(email, password) {
                    return fetch('/nextjs-api/sign-in/email', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email: email, password: password })
                    }).then(r => {
                        if (!r.ok) throw new Error('Login failed: ' + r.status);
                        return r.json();
                    });
                },

                // Sign out
                signOut: function() {
                    return fetch('/nextjs-api/sign-out', { method: 'POST' })
                        .then(() => { window.location.href = '/'; });
                }
            };

            BRIDGE.log('[ArenaBridge] Bridge ready');
        })();
    """.trimIndent()

    /**
     * Send a chat message through the WebView.
     */
    fun sendMessage(
        webView: WebView,
        prompt: String,
        modality: Modality,
        mode: BattleMode,
        conversationId: String? = null
    ) {
        val escapedPrompt = prompt.replace("\\", "\\\\").replace("'", "\\'")
        val modalityStr = modality.apiValue
        val modeStr = mode.apiValue
        val convId = conversationId ?: "null"
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.sendMessage('$escapedPrompt', '$modalityStr', '$modeStr', $convId);",
            null
        )
    }

    /**
     * Stop the current stream.
     */
    fun stopStream(webView: WebView, conversationId: String?, messageId: String?) {
        val convId = conversationId ?: "null"
        val msgId = messageId ?: "null"
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.stop($convId, $msgId);",
            null
        )
    }

    /**
     * Vote for a model.
     */
    fun vote(webView: WebView, conversationId: String, value: String) {
        val escapedValue = value.replace("'", "\\'")
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.vote('$conversationId', '$escapedValue');",
            null
        )
    }

    /**
     * Navigate to a conversation.
     */
    fun openConversation(webView: WebView, conversationId: String) {
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.openConversation('$conversationId');",
            null
        )
    }

    /**
     * Start a new chat.
     */
    fun newChat(webView: WebView) {
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.newChat();",
            null
        )
    }

    /**
     * Refresh history.
     */
    fun refreshHistory(webView: WebView) {
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.refreshHistory();",
            null
        )
    }

    /**
     * Login with email + password through the WebView (handles reCAPTCHA automatically).
     */
    fun loginWithEmail(webView: WebView, email: String, password: String) {
        val escapedEmail = email.replace("\\", "\\\\").replace("'", "\\'")
        val escapedPassword = password.replace("\\", "\\\\").replace("'", "\\'")
        webView.evaluateJavascript(
            """
            (async function() {
                try {
                    await window.__arenaBridge.loginWithEmail('$escapedEmail', '$escapedPassword');
                    window.AndroidBridge.log('Login succeeded');
                } catch (e) {
                    window.AndroidBridge.log('Login error: ' + e.message);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    /**
     * Sign out through the WebView.
     */
    fun signOut(webView: WebView) {
        webView.evaluateJavascript(
            "window.__arenaBridge && window.__arenaBridge.signOut();",
            null
        )
    }

    /**
     * Load arena.ai in the WebView.
     */
    fun loadArena(webView: WebView, path: String = "/") {
        val url = "${BuildConfig.ARENA_BASE_URL}$path"
        Log.d(TAG, "Loading: $url")
        webView.loadUrl(url)
    }
}
