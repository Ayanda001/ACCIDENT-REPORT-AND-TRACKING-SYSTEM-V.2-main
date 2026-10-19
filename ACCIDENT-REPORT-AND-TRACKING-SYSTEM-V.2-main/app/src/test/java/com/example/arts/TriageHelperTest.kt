package com.example.arts

import com.example.arts.data.ContractedService
import com.example.arts.utils.TriageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the rule-based triage that decides which emergency services a report is sent to. */
class TriageHelperTest {

    private val services = listOf(
        ContractedService("a", "Netcare 911", "Ambulance", true),
        ContractedService("p", "SAPS", "Police", true),
        ContractedService("t", "JMPD", "Traffic", true),
        ContractedService("f", "Fire Department", "Fire", true),
        ContractedService("p2", "Retired police contract", "Police", false)
    )

    @Test
    fun findsUrgentWordsInTheDescription() {
        val hits = TriageHelper.findUrgentWords("Car on fire and driver trapped", "Collision")
        assertTrue(hits.containsAll(listOf("fire", "trapped")))
    }

    @Test
    fun twoUrgentWordsMakeAMediumReportUrgent() {
        assertTrue(TriageHelper.isUrgent("Medium", listOf("fire", "trapped")))
    }

    @Test
    fun oneUrgentWordIsNotEnough() {
        assertFalse(TriageHelper.isUrgent("Medium", listOf("fire")))
    }

    @Test
    fun highPriorityIsAlwaysUrgent() {
        assertTrue(TriageHelper.isUrgent("High", emptyList()))
    }

    @Test
    fun collisionWithFireNotifiesAmbulancePoliceTrafficAndFire() {
        val hits = TriageHelper.findUrgentWords("fire and trapped driver", "Collision")
        val chosen = TriageHelper.chooseServices("Collision", hits, services).map { it.serviceType }
        assertEquals(listOf("Ambulance", "Police", "Traffic", "Fire"), chosen)
    }

    @Test
    fun typesWithNoActiveServiceAreSkipped() {
        val chosen = TriageHelper.chooseServices("Breakdown", emptyList(), services).map { it.serviceType }
        assertEquals(listOf("Traffic"), chosen)
    }

    @Test
    fun panicReportNotifiesAmbulanceAndPoliceAndIgnoresInactiveServices() {
        val chosen = TriageHelper.chooseServices("Emergency", listOf("panic"), services).map { it.id }
        assertEquals(listOf("a", "p"), chosen)
    }

    @Test
    fun anUnknownTypeFallsBackToAmbulanceAndPolice() {
        assertEquals(setOf("Ambulance", "Police"), TriageHelper.serviceTypesFor("Something else", emptyList()))
    }
}
