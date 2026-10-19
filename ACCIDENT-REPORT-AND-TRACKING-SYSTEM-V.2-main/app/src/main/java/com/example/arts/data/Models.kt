package com.example.arts.data

import com.example.arts.utils.TimeFormat

/**
 * Core models for the ARTS system.
 */

data class Incident(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val description: String = "",
    val priority: String = "Medium",
    val status: String = "Pending",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUrl: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val createdAt: String = "",
    val forwardedTo: List<String> = emptyList(),
    val autoForwarded: Boolean = false,
    val panic: Boolean = false
) {
    val timeAgo: String get() = TimeFormat.ago(createdAt)

    val progressPercent: Int
        get() = when (status) {
            "Pending" -> 10
            "Forwarded" -> 50
            "Resolved" -> 100
            else -> 0
        }
}

data class UserProfile(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val createdAt: String = "",
    val safetyScore: Int = 85,
    val avatarDataUrl: String? = null,
    val idNumber: String? = null,
    val age: String? = null,
    val gender: String? = null,
    val bloodType: String? = null,
    val weightKg: String? = null,
    val heightCm: String? = null,
    val allergiesOrConditions: String? = null,
    val chronicConditions: String? = null,
    val currentMedications: String? = null,
    val medicalAidProvider: String? = null,
    val medicalAidNumber: String? = null,
    val preferredHospital: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val totpEnabled: Boolean = false,
    val totpSecret: String? = null
)

data class AppAlert(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INCIDENT_UPDATE",
    val read: Boolean = false,
    val createdAt: String = ""
) {
    val isRead: Boolean get() = read
    val timeAgo: String get() = TimeFormat.ago(createdAt)
}

data class ContractedService(
    val id: String = "",
    val name: String = "",
    val serviceType: String = "",
    val active: Boolean = true
)

data class ReportDraft(
    val type: String,
    val description: String,
    val priority: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val photoDataUrl: String = "",
    val panic: Boolean = false
)

data class SubmitResult(val incidentId: String, val dispatchedCount: Int, val queued: Boolean = false)

/** AI suggestion for accident reports. */
data class IncidentAiSuggestion(
    val priority: String,
    val type: String,
    val refinedDescription: String,
    val source: String
)

/** Chat message model for the AI Assistant UI. */
data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val time: String = ""
)
