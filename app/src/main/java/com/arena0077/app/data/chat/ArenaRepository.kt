package com.arena0077.app.data.chat

import com.arena0077.app.data.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ArenaRepository - top-level coordinator between native REST calls and WebView engine.
 *
 * Two execution paths:
 *
 *   1. Pure REST (no reCAPTCHA):
 *        - Auth (sign-in / sign-out / sign-up)
 *        - History list
 *        - /api/me
 *        - auto-modality
 *        - file uploads
 *        - leaderboard
 *
 *   2. WebView-mediated (reCAPTCHA-protected):
 *        - create-evaluation (new chat)
 *        - post-to-evaluation (followup)
 *        - stop / rerun / resample / retry
 *        - vote
 *        - webdev / video workflows
 *
 * The ChatWebViewEngine exposes a Flow<ChatEvent> that the ChatViewModel
 * collects to update UI state.
 */
@Singleton
class ArenaRepository @Inject constructor(
    val authManager: AuthManager,
    val chatRepository: ChatRepository
) {
    val isLoggedIn get() = authManager.isLoggedIn
    val currentUser get() = authManager.currentUser
}
