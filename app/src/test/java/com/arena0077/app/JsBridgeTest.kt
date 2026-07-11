package com.arena0077.app.webview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Unit tests for JsBridge event parsing.
 *
 * Uses UnconfinedTestDispatcher so Dispatchers.Main works in unit tests.
 */
class JsBridgeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var bridge: JsBridge

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        bridge = JsBridge(json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `AuthStateChanged parses correctly`() = runTest {
        val payload = """
            {
                "isLoggedIn": true,
                "email": "user@test.com",
                "userId": "abc-123"
            }
        """.trimIndent()

        bridge.onAuthStateChanged(payload)

        val event = bridge.events.first() as ChatEvent.AuthStateChanged
        assertTrue(event.isLoggedIn)
        assertEquals("user@test.com", event.email)
        assertEquals("abc-123", event.userId)
    }

    @Test
    fun `ConversationCreated parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "modality": "chat",
                "mode": "battle",
                "title": "Test conversation"
            }
        """.trimIndent()

        bridge.onConversationCreated(payload)

        val event = bridge.events.first() as ChatEvent.ConversationCreated
        assertEquals("conv-001", event.conversationId)
        assertEquals(com.arena0077.app.data.models.Modality.CHAT, event.modality)
        assertEquals(com.arena0077.app.data.models.BattleMode.BATTLE, event.mode)
        assertEquals("Test conversation", event.title)
    }

    @Test
    fun `StreamStarted parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "modelLabel": "A"
            }
        """.trimIndent()

        bridge.onStreamStarted(payload)

        val event = bridge.events.first() as ChatEvent.StreamStarted
        assertEquals("conv-001", event.conversationId)
        assertEquals("msg-001", event.messageId)
        assertEquals("A", event.modelLabel)
    }

    @Test
    fun `StreamChunk parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "delta": "Hello",
                "modelLabel": "A"
            }
        """.trimIndent()

        bridge.onStreamChunk(payload)

        val event = bridge.events.first() as ChatEvent.StreamChunk
        assertEquals("Hello", event.delta)
        assertEquals("A", event.modelLabel)
    }

    @Test
    fun `StreamCompleted parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "finalContent": "Hello, world!",
                "modelLabel": "A"
            }
        """.trimIndent()

        bridge.onStreamCompleted(payload)

        val event = bridge.events.first() as ChatEvent.StreamCompleted
        assertEquals("Hello, world!", event.finalContent)
    }

    @Test
    fun `StreamError parses with reCAPTCHA flag`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "message": "recaptcha validation failed",
                "isRecaptchaError": true
            }
        """.trimIndent()

        bridge.onStreamError(payload)

        val event = bridge.events.first() as ChatEvent.StreamError
        assertTrue(event.isRecaptchaError)
        assertEquals("recaptcha validation failed", event.message)
    }

    @Test
    fun `ImageGenerated parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "imageUrl": "https://example.com/image.png",
                "modelLabel": "A"
            }
        """.trimIndent()

        bridge.onImageGenerated(payload)

        val event = bridge.events.first() as ChatEvent.ImageGenerated
        assertEquals("https://example.com/image.png", event.imageUrl)
    }

    @Test
    fun `AgentStep parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "messageId": "msg-001",
                "stepNumber": 1,
                "action": "Search web",
                "result": "Found 5 pages"
            }
        """.trimIndent()

        bridge.onAgentStep(payload)

        val event = bridge.events.first() as ChatEvent.AgentStep
        assertEquals(1, event.stepNumber)
        assertEquals("Search web", event.action)
        assertEquals("Found 5 pages", event.result)
    }

    @Test
    fun `HistoryLoaded parses list of items`() = runTest {
        val payload = """
            {
                "items": [
                    {
                        "id": "conv-1",
                        "title": "First conversation",
                        "modality": "chat",
                        "mode": "battle",
                        "createdAt": "2026-07-10T10:00:00Z"
                    },
                    {
                        "id": "conv-2",
                        "title": "Second conversation",
                        "modality": "image",
                        "mode": "direct",
                        "createdAt": "2026-07-10T11:00:00Z"
                    }
                ]
            }
        """.trimIndent()

        bridge.onHistoryLoaded(payload)

        val event = bridge.events.first() as ChatEvent.HistoryLoaded
        assertEquals(2, event.items.size)
        assertEquals("conv-1", event.items[0].id)
        assertEquals("First conversation", event.items[0].title)
        assertEquals("chat", event.items[0].modality)
    }

    @Test
    fun `RecaptchaChallenge parses correctly`() = runTest {
        val payload = """
            {
                "conversationId": "conv-001",
                "reason": "recaptcha_v3_failed",
                "sitekey": "6Le3_cYsAAAAAGwWOK2RLDgNI15Bh8C0yLBOL1yL"
            }
        """.trimIndent()

        bridge.onRecaptchaChallenge(payload)

        val event = bridge.events.first() as ChatEvent.RecaptchaChallenge
        assertEquals("recaptcha_v3_failed", event.reason)
        assertEquals("6Le3_cYsAAAAAGwWOK2RLDgNI15Bh8C0yLBOL1yL", event.sitekey)
    }

    @Test
    fun `log method does not throw`() {
        // Just verify no exception is thrown
        bridge.log("test log message")
    }

    @Test
    fun `emitEvent handles unknown event types gracefully`() {
        // Should not throw
        bridge.emitEvent("unknown_type", "{}")
    }
}
