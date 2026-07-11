package com.arena0077.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modalities supported by arena.ai - extracted from production traffic.
 *
 * The auto-modality endpoint returns confidence scores for all 5 modalities:
 *   text, image, video, search, code
 *
 * The create-evaluation endpoint accepts: chat, image, video, webdev
 * (note: "chat" not "text" for the chat modality)
 */
@Serializable
enum class Modality(val apiValue: String, val displayName: String) {
    @SerialName("chat") CHAT("chat", "Chat"),
    @SerialName("image") IMAGE("image", "Image"),
    @SerialName("video") VIDEO("video", "Video"),
    @SerialName("webdev") WEBDEV("webdev", "Web Dev"),
    @SerialName("search") SEARCH("search", "Search"),
    @SerialName("code") CODE("code", "Code");

    companion object {
        fun fromApi(value: String?): Modality =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: CHAT

        fun fromConfidence(confidence: ModalityConfidence): Modality {
            val map = mapOf(
                IMAGE to confidence.image,
                SEARCH to confidence.search,
                CHAT to confidence.text,
                VIDEO to confidence.video,
                CODE to confidence.code
            )
            return map.maxByOrNull { it.value }?.key ?: CHAT
        }
    }
}

/**
 * Battle modes - how two models are compared.
 * Captured from arena.ai: battle, side, direct, direct-battle, agent
 *
 * Note: "direct-battle" is a variant of direct that still pits 2 models
 * but with user-selected models.
 */
@Serializable
enum class BattleMode(val apiValue: String, val displayName: String) {
    @SerialName("battle") BATTLE("battle", "Battle Mode"),
    @SerialName("side") SIDE("side", "Side Mode"),
    @SerialName("direct") DIRECT("direct", "Direct Chat"),
    @SerialName("direct-battle") DIRECT_BATTLE("direct-battle", "Direct Battle"),
    @SerialName("agent") AGENT("agent", "Agent Mode");

    companion object {
        fun fromApi(value: String?): BattleMode =
            values().firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: BATTLE
    }
}

/**
 * Quick action templates shown on the empty chat state.
 * Extracted verbatim from arena.ai's homepage on 2026-07-11.
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
