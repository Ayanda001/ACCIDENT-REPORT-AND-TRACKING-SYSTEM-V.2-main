package com.example.arts

import com.example.arts.utils.GeminiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests AI Assist: the answer from Gemini is read safely, and the built-in rules work without a key. */
class GeminiHelperTest {

    private fun geminiReply(innerJson: String): String {
        val escaped = innerJson.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    @Test
    fun readsAWellFormedGeminiAnswer() {
        val reply = geminiReply("""{"priority":"High","type":"Collision","refinedDescription":"Two cars collided."}""")
        val result = GeminiHelper.parseSuggestion(reply, "cars crashed")
        assertEquals("High", result.priority)
        assertEquals("Collision", result.type)
        assertEquals("Two cars collided.", result.refinedDescription)
        assertEquals("Gemini", result.source)
    }

    @Test
    fun stripsMarkdownFencesAroundTheJson() {
        val reply = geminiReply("```json\n{\"priority\":\"Low\",\"type\":\"Breakdown\",\"refinedDescription\":\"Car broke down.\"}\n```")
        val result = GeminiHelper.parseSuggestion(reply, "car broke down")
        assertEquals("Low", result.priority)
        assertEquals("Breakdown", result.type)
    }

    @Test
    fun replacesUnexpectedValuesWithSafeDefaults() {
        val reply = geminiReply("""{"priority":"URGENT!!","type":"Spaceship","refinedDescription":""}""")
        val result = GeminiHelper.parseSuggestion(reply, "something happened")
        assertEquals("Medium", result.priority)
        assertEquals("Collision", result.type)
        assertEquals("Something happened.", result.refinedDescription)
    }

    @Test
    fun builtInRulesRaisePriorityForUrgentWords() {
        val result = GeminiHelper.localSuggestion("car on fire and driver trapped inside")
        assertEquals("High", result.priority)
        assertEquals("built-in rules", result.source)
    }

    @Test
    fun builtInRulesRecogniseAPedestrianIncident() {
        assertEquals("Pedestrian", GeminiHelper.localSuggestion("a man was walking across the road and got hit").type)
    }

    @Test
    fun builtInRulesRecogniseABreakdownAsLowPriority() {
        val result = GeminiHelper.localSuggestion("my car broke down on the shoulder")
        assertEquals("Breakdown", result.type)
        assertEquals("Low", result.priority)
    }

    @Test
    fun tidyCapitalisesAndEndsWithAFullStop() {
        assertEquals("Car stopped suddenly.", GeminiHelper.tidy("  car   stopped suddenly "))
        assertEquals("Already fine!", GeminiHelper.tidy("already fine!"))
        assertTrue(GeminiHelper.tidy("").isEmpty())
    }
}
