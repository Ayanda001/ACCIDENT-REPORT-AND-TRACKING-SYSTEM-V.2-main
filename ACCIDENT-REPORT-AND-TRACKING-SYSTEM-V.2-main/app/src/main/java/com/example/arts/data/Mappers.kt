package com.example.arts.data

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Robust mappers that convert Firestore documents into Kotlin models.
 * Designed to handle inconsistent data types (e.g. string vs number) 
 * and missing fields without crashing the app.
 */

private fun DocumentSnapshot.text(field: String): String =
    try { get(field)?.toString()?.trim().orEmpty() } catch (e: Exception) { "" }

private fun DocumentSnapshot.num(field: String): Double? =
    try {
        val v = get(field)
        when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    } catch (e: Exception) { null }

private fun DocumentSnapshot.bool(field: String, default: Boolean): Boolean {
    return try {
        val v = get(field) ?: return default
        if (v is Boolean) return v
        val s = v.toString().lowercase()
        when (s) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> default
        }
    } catch (e: Exception) { default }
}

fun DocumentSnapshot.toIncident(): Incident = Incident(
    id = id,
    title = text("title"),
    type = text("type"),
    description = text("description"),
    priority = text("priority").ifBlank { "Medium" },
    status = text("status").ifBlank { "Pending" },
    location = text("location"),
    latitude = num("latitude"),
    longitude = num("longitude"),
    photoUrl = text("photoUrl"),
    reporterId = text("reporterId"),
    reporterName = text("reporterName"),
    createdAt = text("createdAt"),
    forwardedTo = try { (get("forwardedTo") as? List<*>)?.filterIsInstance<String>().orEmpty() } catch (e: Exception) { emptyList() },
    autoForwarded = bool("autoForwarded", false),
    panic = bool("panic", false)
)

fun DocumentSnapshot.toUserProfile(totpEnabled: Boolean = false, totpSecret: String? = null): UserProfile = UserProfile(
    id = id,
    fullName = text("fullName"),
    email = text("email"),
    phone = text("phone"),
    createdAt = text("createdAt"),
    safetyScore = num("safetyScore")?.toInt() ?: 85,
    avatarDataUrl = text("avatarDataUrl").takeIf { it.isNotBlank() },
    idNumber = text("idNumber").takeIf { it.isNotBlank() },
    age = text("age").takeIf { it.isNotBlank() },
    gender = text("gender").takeIf { it.isNotBlank() },
    bloodType = text("bloodType").takeIf { it.isNotBlank() },
    weightKg = text("weightKg").takeIf { it.isNotBlank() },
    heightCm = text("heightCm").takeIf { it.isNotBlank() },
    allergiesOrConditions = text("allergiesOrConditions").takeIf { it.isNotBlank() },
    chronicConditions = text("chronicConditions").takeIf { it.isNotBlank() },
    currentMedications = text("currentMedications").takeIf { it.isNotBlank() },
    medicalAidProvider = text("medicalAidProvider").takeIf { it.isNotBlank() },
    medicalAidNumber = text("medicalAidNumber").takeIf { it.isNotBlank() },
    preferredHospital = text("preferredHospital").takeIf { it.isNotBlank() },
    emergencyContactName = text("emergencyContactName").takeIf { it.isNotBlank() },
    emergencyContactPhone = text("emergencyContactPhone").takeIf { it.isNotBlank() },
    totpEnabled = totpEnabled,
    totpSecret = totpSecret
)

fun DocumentSnapshot.toAppAlert(): AppAlert = AppAlert(
    id = id,
    title = text("title"),
    message = text("message"),
    type = text("type").ifBlank { "INCIDENT_UPDATE" },
    read = bool("read", false),
    createdAt = text("createdAt")
)

fun DocumentSnapshot.toService(): ContractedService = ContractedService(
    id = id,
    name = text("name"),
    serviceType = text("serviceType"),
    active = bool("active", true)
)
