package com.arena0077.app.data.models

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for Modality enum.
 */
class ModalityTest {

    @Test
    fun `fromApi returns CHAT for null input`() {
        assertEquals(Modality.CHAT, Modality.fromApi(null))
    }

    @Test
    fun `fromApi returns correct modality for each api value`() {
        assertEquals(Modality.CHAT, Modality.fromApi("chat"))
        assertEquals(Modality.IMAGE, Modality.fromApi("image"))
        assertEquals(Modality.VIDEO, Modality.fromApi("video"))
        assertEquals(Modality.WEBDEV, Modality.fromApi("webdev"))
    }

    @Test
    fun `fromApi is case insensitive`() {
        assertEquals(Modality.CHAT, Modality.fromApi("CHAT"))
        assertEquals(Modality.IMAGE, Modality.fromApi("Image"))
        assertEquals(Modality.VIDEO, Modality.fromApi("VIDEO"))
    }

    @Test
    fun `fromApi falls back to CHAT for unknown values`() {
        assertEquals(Modality.CHAT, Modality.fromApi("unknown"))
        assertEquals(Modality.CHAT, Modality.fromApi(""))
    }

    @Test
    fun `all modalities have unique api values`() {
        val apiValues = Modality.values().map { it.apiValue }
        assertEquals(apiValues.size, apiValues.toSet().size)
    }

    @Test
    fun `all modalities have unique display names`() {
        val displayNames = Modality.values().map { it.displayName }
        assertEquals(displayNames.size, displayNames.toSet().size)
    }
}

class BattleModeTest {

    @Test
    fun `fromApi returns BATTLE for null input`() {
        assertEquals(BattleMode.BATTLE, BattleMode.fromApi(null))
    }

    @Test
    fun `fromApi returns correct mode for each api value`() {
        assertEquals(BattleMode.BATTLE, BattleMode.fromApi("battle"))
        assertEquals(BattleMode.SIDE, BattleMode.fromApi("side"))
        assertEquals(BattleMode.DIRECT, BattleMode.fromApi("direct"))
        assertEquals(BattleMode.AGENT, BattleMode.fromApi("agent"))
    }

    @Test
    fun `fromApi is case insensitive`() {
        assertEquals(BattleMode.BATTLE, BattleMode.fromApi("BATTLE"))
        assertEquals(BattleMode.AGENT, BattleMode.fromApi("Agent"))
    }

    @Test
    fun `fromApi falls back to BATTLE for unknown values`() {
        assertEquals(BattleMode.BATTLE, BattleMode.fromApi("unknown"))
    }
}

class VoteValueTest {

    @Test
    fun `fromApi returns TIE for null input`() {
        assertEquals(VoteValue.TIE, VoteValue.fromApi(null))
    }

    @Test
    fun `fromApi returns correct vote for each api value`() {
        assertEquals(VoteValue.MODEL_A_UPVOTE, VoteValue.fromApi("model_a"))
        assertEquals(VoteValue.MODEL_B_UPVOTE, VoteValue.fromApi("model_b"))
        assertEquals(VoteValue.TIE, VoteValue.fromApi("tie"))
        assertEquals(VoteValue.BOTH_BAD, VoteValue.fromApi("bothbad"))
    }
}

class QuickActionTest {

    @Test
    fun `all quick actions have non-blank prompts`() {
        QuickAction.values().forEach { action ->
            assertTrue(
                "QuickAction ${action.name} should have non-blank prompt",
                action.prompt.isNotBlank()
            )
        }
    }

    @Test
    fun `all quick actions have non-blank titles`() {
        QuickAction.values().forEach { action ->
            assertTrue(
                "QuickAction ${action.name} should have non-blank title",
                action.title.isNotBlank()
            )
        }
    }

    @Test
    fun `quick actions include expected items`() {
        val titles = QuickAction.values().map { it.title }
        assertTrue("Should include landing page", titles.any { it.contains("landing") })
        assertTrue("Should include dashboard", titles.any { it.contains("dashboard") })
        assertTrue("Should include game", titles.any { it.contains("game") })
        assertTrue("Should include Design to Code", titles.any { it.contains("Design to Code") })
    }

    @Test
    fun `webdev actions use WEBDEV modality`() {
        QuickAction.values().forEach { action ->
            assertEquals(
                "QuickAction ${action.name} should use WEBDEV modality",
                Modality.WEBDEV,
                action.modality
            )
        }
    }
}
