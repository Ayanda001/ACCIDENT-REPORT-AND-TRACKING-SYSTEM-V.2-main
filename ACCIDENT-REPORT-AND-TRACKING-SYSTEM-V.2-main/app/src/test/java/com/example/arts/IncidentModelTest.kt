package com.example.arts

import com.example.arts.data.Incident
import com.example.arts.utils.QrHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests the incident model and the QR payload shared with the website. */
class IncidentModelTest {

    @Test
    fun progressFollowsStatus() {
        assertEquals(10, Incident(status = "Pending").progressPercent)
        assertEquals(50, Incident(status = "Forwarded").progressPercent)
        assertEquals(100, Incident(status = "Resolved").progressPercent)
        assertEquals(0, Incident(status = "Unknown").progressPercent)
    }

    @Test
    fun qrPayloadRoundTrips() {
        val payload = QrHelper.buildIncidentPayload("abc123")
        assertEquals("arts://incident/abc123", payload)
        assertEquals("abc123", QrHelper.parseIncidentId(payload))
    }

    @Test
    fun qrAcceptsABareId() = assertEquals("abc123", QrHelper.parseIncidentId("  abc123 "))

    @Test
    fun qrRejectsEmptyAndForeignContent() {
        assertNull(QrHelper.parseIncidentId("arts://incident/"))
        assertNull(QrHelper.parseIncidentId("https://example.com/page"))
        assertNull(QrHelper.parseIncidentId(""))
    }
}
