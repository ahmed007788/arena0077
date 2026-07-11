package com.arena0077.app.data.models

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Unit tests for serialization of data models.
 * Based on REAL captured data from arena.ai production traffic (2026-07-11).
 */
class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `ArenaUser deserializes from real arena response shape`() {
        val responseJson = """
        {
            "user": {
                "id": "019bd58f-849d-75c2-a614-10b608b7d4b5",
                "supabaseUserId": "cb3f7163-622e-4f8a-b9c3-84f0d6acc976",
                "touConsentTimestamp": "2026-01-19 09:25:37.256+00",
                "avatarUrl": null,
                "email": "ai9900@bjedu.tech",
                "emailProvider": "email",
                "username": "A",
                "marketingSubscribed": false
            }
        }
        """.trimIndent()

        val user = json.decodeFromString(ArenaUser.serializer(), responseJson)

        assertEquals("019bd58f-849d-75c2-a614-10b608b7d4b5", user.user.id)
        assertEquals("cb3f7163-622e-4f8a-b9c3-84f0d6acc976", user.user.supabaseUserId)
        assertEquals("ai9900@bjedu.tech", user.user.email)
        assertEquals("email", user.user.emailProvider)
        assertEquals("A", user.user.username)
        assertFalse(user.user.marketingSubscribed)
    }

    @Test
    fun `ArenaUser ignores unknown fields`() {
        val jsonWithExtras = """
        {
            "user": {
                "id": "test-id",
                "supabaseUserId": "supabase-id",
                "email": "test@test.com",
                "unknown_field": "should be ignored"
            },
            "extra_field": true
        }
        """.trimIndent()

        val user = json.decodeFromString(ArenaUser.serializer(), jsonWithExtras)
        assertEquals("test-id", user.user.id)
        assertEquals("test@test.com", user.user.email)
    }

    @Test
    fun `AuthSession isExpired returns true for past timestamps`() {
        val session = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() / 1000 - 3600,
            user = SupabaseUser(id = "1", email = "test@test.com")
        )

        assertTrue(session.isExpired)
        assertTrue(!session.isValid)
    }

    @Test
    fun `AuthSession isValid returns true for future timestamps`() {
        val session = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() / 1000 + 3600,
            user = SupabaseUser(id = "1", email = "test@test.com")
        )

        assertTrue(!session.isExpired)
        assertTrue(session.isValid)
    }

    @Test
    fun `Conversation displayTitle falls back to New Chat when title is null`() {
        val conv = Conversation(
            id = "test-id",
            title = null,
            createdAt = "2026-07-11"
        )
        assertEquals("New Chat", conv.displayTitle)
    }

    @Test
    fun `Conversation displayTitle falls back to New Chat when title is blank`() {
        val conv = Conversation(
            id = "test-id",
            title = "   ",
            createdAt = "2026-07-11"
        )
        assertEquals("New Chat", conv.displayTitle)
    }

    @Test
    fun `Conversation displayTitle returns title when non-blank`() {
        val conv = Conversation(
            id = "test-id",
            title = "What is 2+2?",
            createdAt = "2026-07-11"
        )
        assertEquals("What is 2+2?", conv.displayTitle)
    }

    @Test
    fun `Conversation battleMode extracts from mode string`() {
        val conv = Conversation(
            id = "test",
            mode = "battle",
            createdAt = "2026-07-11"
        )
        assertEquals(BattleMode.BATTLE, conv.battleMode)

        val conv2 = Conversation(id = "test", mode = "agent", createdAt = "2026-07-11")
        assertEquals(BattleMode.AGENT, conv2.battleMode)

        val conv3 = Conversation(id = "test", mode = "direct-battle", createdAt = "2026-07-11")
        assertEquals(BattleMode.DIRECT, conv3.battleMode)
    }

    @Test
    fun `Message isUser detects user role`() {
        val msg = Message(id = "1", role = "user", content = "hello")
        assertTrue(msg.isUser)
        assertTrue(!msg.isModelA)
        assertTrue(!msg.isModelB)
    }

    @Test
    fun `Message isModelA detects participant position a`() {
        val msg = Message(id = "1", role = "assistant", content = "hi", participantPosition = "a")
        assertTrue(!msg.isUser)
        assertTrue(msg.isModelA)
        assertTrue(!msg.isModelB)
        assertEquals("Model A", msg.modelLabel)
    }

    @Test
    fun `Message isModelB detects participant position b`() {
        val msg = Message(id = "1", role = "assistant", content = "hi", participantPosition = "b")
        assertTrue(msg.isModelB)
        assertEquals("Model B", msg.modelLabel)
    }

    @Test
    fun `SignInEmailRequest serializes with shouldLinkHistory`() {
        val request = SignInEmailRequest(
            email = "user@example.com",
            password = "password123",
            shouldLinkHistory = true
        )

        val jsonStr = json.encodeToString(SignInEmailRequest.serializer(), request)

        assertTrue(jsonStr.contains("\"email\":\"user@example.com\""))
        assertTrue(jsonStr.contains("\"password\":\"password123\""))
        assertTrue(jsonStr.contains("\"shouldLinkHistory\":true"))
    }

    @Test
    fun `AutoModalityRequest uses user_prompt field name`() {
        val request = AutoModalityRequest(
            userPrompt = "Generate an image of a cat",
            hasImage = false
        )

        val jsonStr = json.encodeToString(AutoModalityRequest.serializer(), request)

        // CRITICAL: arena.ai uses user_prompt (not prompt)
        assertTrue(jsonStr.contains("\"user_prompt\":\"Generate an image of a cat\""))
        assertTrue(jsonStr.contains("\"has_image\":false"))
    }

    @Test
    fun `LeaderboardCategory fromApi returns OVERALL for null`() {
        assertEquals(LeaderboardCategory.OVERALL, LeaderboardCategory.fromApi(null))
    }

    @Test
    fun `LeaderboardCategory fromApi is case insensitive`() {
        assertEquals(LeaderboardCategory.TEXT, LeaderboardCategory.fromApi("TEXT"))
        assertEquals(LeaderboardCategory.CODING, LeaderboardCategory.fromApi("Coding"))
    }

    @Test
    fun `Modality fromConfidence picks highest scoring modality`() {
        val confidence = ModalityConfidence(
            image = 0.9,
            search = 0.1,
            text = 0.05,
            video = 0.02,
            code = 0.01
        )
        assertEquals(Modality.IMAGE, Modality.fromConfidence(confidence))
    }

    @Test
    fun `Modality fromConfidence defaults to CHAT for text-heavy prompts`() {
        val confidence = ModalityConfidence(
            image = 0.0001,
            search = 0.0002,
            text = 0.9995,
            video = 0.00003,
            code = 0.0001
        )
        assertEquals(Modality.CHAT, Modality.fromConfidence(confidence))
    }

    @Test
    fun `VoteValue uses model_a and model_b values`() {
        val req = VoteRequest(
            value = "model_a",
            messageAId = "msg-a",
            messageBId = "msg-b",
            evaluationSessionId = "conv-1"
        )
        val jsonStr = json.encodeToString(VoteRequest.serializer(), req)
        assertTrue(jsonStr.contains("\"value\":\"model_a\""))
    }
}
