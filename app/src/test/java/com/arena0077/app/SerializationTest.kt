package com.arena0077.app.data.models

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Unit tests for serialization of data models.
 *
 * These tests verify that our @Serializable models correctly serialize and
 * deserialize to the JSON shapes that arena.ai's actual API uses.
 */
class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `ArenaUser deserializes from arena.ai response shape`() {
        val responseJson = """
        {
            "id": "cb3f7163-622e-4f8a-b9c3-84f0d6acc976",
            "aud": "authenticated",
            "role": "authenticated",
            "email": "Ai9900@bjedu.tech",
            "email_confirmed_at": "2026-01-19T09:24:11.978647Z",
            "confirmed_at": "2026-01-19T09:24:11.978647Z",
            "last_sign_in_at": "2026-07-11T00:09:14.78794Z",
            "app_metadata": {
                "provider": "email",
                "providers": ["email"]
            },
            "user_metadata": {
                "domain_url": "https://lmarena.ai",
                "email": "Ai9900@bjedu.tech",
                "email_verified": true,
                "full_name": "A",
                "phone_verified": false,
                "should_link_history": true
            },
            "is_anonymous": false,
            "created_at": "2026-01-19T09:22:49.666901Z"
        }
        """.trimIndent()

        val user = json.decodeFromString(ArenaUser.serializer(), responseJson)

        assertEquals("cb3f7163-622e-4f8a-b9c3-84f0d6acc976", user.id)
        assertEquals("Ai9900@bjedu.tech", user.email)
        assertEquals("authenticated", user.aud)
        assertEquals("email", user.appMetadata.provider)
        assertEquals("https://lmarena.ai", user.userMetadata.domainUrl)
        assertTrue(user.userMetadata.emailVerified)
        assertNotNull(user.emailConfirmedAt)
    }

    @Test
    fun `ArenaUser ignores unknown fields`() {
        val jsonWithExtras = """
        {
            "id": "test-id",
            "email": "test@test.com",
            "unknown_field": "should be ignored",
            "another_unknown": 123
        }
        """.trimIndent()

        val user = json.decodeFromString(ArenaUser.serializer(), jsonWithExtras)
        assertEquals("test-id", user.id)
        assertEquals("test@test.com", user.email)
    }

    @Test
    fun `CreateEvaluationRequest serializes with correct field names`() {
        val request = CreateEvaluationRequest(
            modality = Modality.CHAT,
            mode = BattleMode.BATTLE,
            prompt = "Hello, what is 2+2?",
            modelAId = null,
            modelBId = null
        )

        val jsonStr = json.encodeToString(CreateEvaluationRequest.serializer(), request)

        assertTrue(jsonStr.contains("\"modality\":\"chat\""))
        assertTrue(jsonStr.contains("\"mode\":\"battle\""))
        assertTrue(jsonStr.contains("\"prompt\":\"Hello, what is 2+2?\""))
    }

    @Test
    fun `PostToEvaluationRequest serializes with null mode for followups`() {
        val request = PostToEvaluationRequest(
            prompt = "Followup question",
            mode = null
        )

        val jsonStr = json.encodeToString(PostToEvaluationRequest.serializer(), request)

        assertTrue(jsonStr.contains("\"prompt\":\"Followup question\""))
        // mode should be omitted (explicitNulls = false)
        assertTrue(!jsonStr.contains("\"mode\""))
    }

    @Test
    fun `AuthSession isExpired returns true for past timestamps`() {
        val session = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() / 1000 - 3600,  // 1 hour ago
            user = ArenaUser(id = "1", email = "test@test.com")
        )

        assertTrue(session.isExpired)
        assertTrue(!session.isValid)
    }

    @Test
    fun `AuthSession isValid returns true for future timestamps`() {
        val session = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = System.currentTimeMillis() / 1000 + 3600,  // 1 hour from now
            user = ArenaUser(id = "1", email = "test@test.com")
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
    fun `SignInEmailRequest serializes correctly`() {
        val request = SignInEmailRequest(
            email = "user@example.com",
            password = "password123"
        )

        val jsonStr = json.encodeToString(SignInEmailRequest.serializer(), request)

        assertTrue(jsonStr.contains("\"email\":\"user@example.com\""))
        assertTrue(jsonStr.contains("\"password\":\"password123\""))
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
}
