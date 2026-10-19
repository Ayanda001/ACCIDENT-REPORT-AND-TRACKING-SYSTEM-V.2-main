package com.example.arts.utils

import com.example.arts.data.ContractedService

/**
 * Rule-based triage, identical to the logic on the website so a report
 * is treated the same way whichever one it came from.
 *
 * A report is urgent when it is marked High priority, or when its
 * description contains two or more urgent warning words. Urgent reports
 * are sent automatically to the matching active emergency services.
 */
object TriageHelper {

    val URGENT_WORDS = listOf(
        "fire", "explosion", "trapped", "unconscious", "not breathing", "bleeding",
        "fatal", "dead", "overturned", "pinned", "fuel leak", "child"
    )

    private val FIRE_WORDS = listOf("fire", "explosion", "trapped", "pinned", "fuel leak")

    private val SERVICES_FOR = mapOf(
        "Collision" to listOf("Ambulance", "Police", "Traffic"),
        "Pedestrian Incident" to listOf("Ambulance", "Police"),
        "Breakdown" to listOf("Towing", "Traffic"),
        "Emergency" to listOf("Ambulance", "Police")
    )

    fun findUrgentWords(description: String, type: String): List<String> {
        val text = "$description $type".lowercase()
        return URGENT_WORDS.filter { text.contains(it) }
    }

    fun isUrgent(priority: String, urgentWords: List<String>): Boolean =
        priority == "High" || urgentWords.size >= 2

    fun serviceTypesFor(type: String, urgentWords: List<String>): Set<String> {
        val wanted = LinkedHashSet(SERVICES_FOR[type] ?: listOf("Ambulance", "Police"))
        if (urgentWords.any { it in FIRE_WORDS }) wanted.add("Fire")
        return wanted
    }

    /** The first active service of each wanted type. Types with no active service are skipped. */
    fun chooseServices(type: String, urgentWords: List<String>, services: List<ContractedService>): List<ContractedService> {
        val active = services.filter { it.active }
        return serviceTypesFor(type, urgentWords).mapNotNull { wanted ->
            active.firstOrNull { it.serviceType == wanted }
        }
    }
}
