package com.arena0077.app.ui.navigation

/**
 * App navigation routes.
 * Mirrors arena.ai's URL structure:
 *   /            -> home (chat)
 *   /c/{id}      -> conversation
 *   /leaderboard -> leaderboard
 *   /settings    -> settings (native-only, no arena.ai equivalent)
 */
object Routes {
    const val LOGIN = "login"
    const val CHAT = "chat"
    const val CONVERSATION = "conversation/{id}"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"

    fun conversation(id: String) = "conversation/$id"
}
