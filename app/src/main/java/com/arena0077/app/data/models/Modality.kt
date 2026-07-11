package com.arena0077.app.data.models

import kotlinx.serialization.Serializable

/**
 * Modalities supported by arena.ai - extracted from the actual JS bundles.
 *
 * The enum EEvaluationModality appears in arena.ai's source code:
 *   - CHAT:   Standard text chat (Battle Mode default)
 *   - IMAGE:  Image generation (DALL-E, Midjourney, Stable Diffusion, etc.)
 *   - VIDEO:  Video generation
 *   - WEBDEV: Web development mode (Design to Code, Build dashboard, Build game, etc.)
 */
@Serializable
enum class Modality(val apiValue: String, val displayName: String) {
    CHAT("chat", "Chat"),
    IMAGE("image", "Image"),
    VIDEO("video", "Video"),
    WEBDEV("webdev", "Web Dev");

    companion object {
        fun fromApi(value: String?): Modality =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: CHAT
    }
}

/**
 * Battle modes - how two models are compared.
 * Extracted from the actual arena.ai source.
 *
 *   - BATTLE:      Two anonymous models compete side-by-side
 *   - SIDE:        Side-by-side comparison with model names visible
 *   - DIRECT:      Chat with a single specific model
 *   - AGENT:       Agent mode - autonomous multi-step task execution
 */
@Serializable
enum class BattleMode(val apiValue: String, val displayName: String) {
    BATTLE("battle", "Battle Mode"),
    SIDE("side", "Side Mode"),
    DIRECT("direct", "Direct Chat"),
    AGENT("agent", "Agent Mode");

    companion object {
        fun fromApi(value: String?): BattleMode =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: BATTLE
    }
}

/**
 * Chat modality toolbar options - the small buttons above the input box.
 *   - CODE:   Code generation modality
 *   - SEARCH: Web search modality
 *   - IMAGE:  Image generation
 *   - VIDEO:  Video generation
 */
@Serializable
enum class ChatToolbar(val apiValue: String, val displayName: String) {
    CODE("code", "Code"),
    SEARCH("search", "Search"),
    IMAGE("image", "Image"),
    VIDEO("video", "Video")
}

/**
 * Quick action templates shown on the empty chat state.
 * Extracted verbatim from arena.ai's homepage.
 */
@Serializable
enum class QuickAction(val title: String, val description: String, val prompt: String, val modality: Modality) {
    LANDING_PAGE(
        "Create a landing page",
        "Create a sleek, modern landing page",
        "Create a sleek, modern landing page for a SaaS product",
        Modality.WEBDEV
    ),
    DASHBOARD(
        "Build a dashboard",
        "Turn data into interactive charts",
        "Build an interactive analytics dashboard with charts",
        Modality.WEBDEV
    ),
    GAME(
        "Make a game",
        "Build a playable browser game",
        "Build a playable browser game with HTML5 canvas",
        Modality.WEBDEV
    ),
    DESIGN_TO_CODE(
        "Design to Code",
        "Upload an image and have AI build it",
        "Convert this design image into clean HTML/CSS code",
        Modality.WEBDEV
    ),
    FULLSTACK_APP(
        "Build a fullstack app",
        "Create a templated full-stack app",
        "Create a full-stack todo app with authentication",
        Modality.WEBDEV
    ),
    STOREFRONT(
        "Launch a storefront",
        "Create a beautiful online shop",
        "Build a beautiful e-commerce storefront",
        Modality.WEBDEV
    )
}
