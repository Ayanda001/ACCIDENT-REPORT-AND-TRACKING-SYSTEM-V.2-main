package com.example.arts.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.arts.data.ContractedService
import com.example.arts.data.DocumentLiveData
import com.example.arts.data.Incident
import com.example.arts.data.QueryLiveData
import com.example.arts.data.ReportDraft
import com.example.arts.data.SubmitResult
import com.example.arts.data.toIncident
import com.example.arts.data.toService
import com.example.arts.utils.TimeFormat
import com.example.arts.utils.TriageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "ARTS-Incidents"

/** How long to wait for the server before treating a report as queued on the phone (offline). */
private const val SERVER_WAIT_MS = 10_000L

/** Incident reports, backed by Cloud Firestore. People can only read their own reports (see firestore.rules). */
class IncidentRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    /** The signed-in person's reports, newest first, updating live. */
    fun observeMyIncidents(): LiveData<List<Incident>> {
        val uid = uid() ?: return MutableLiveData(emptyList())
        return QueryLiveData(db.collection("incidents").whereEqualTo("reporterId", uid)) { snapshot ->
            snapshot.documents
                .map { it.toIncident() }
                .sortedByDescending { TimeFormat.parse(it.createdAt) ?: 0L }
        }
    }

    fun observeIncident(id: String): LiveData<Incident?> =
        DocumentLiveData(db.collection("incidents").document(id)) { snapshot ->
            if (snapshot.exists()) snapshot.toIncident() else null
        }

    suspend fun getMyIncidents(): List<Incident> {
        val uid = uid() ?: return emptyList()
        return db.collection("incidents").whereEqualTo("reporterId", uid).get().await()
            .documents.map { it.toIncident() }
    }

    private suspend fun getServices(): List<ContractedService> = try {
        db.collection("contractedServices").get().await().documents.map { it.toService() }
    } catch (e: Exception) {
        // A report should still be saved if the service list cannot be read.
        Log.w(TAG, "Could not load emergency services: ${e.message}")
        emptyList()
    }

    /**
     * Saves a report, runs it through triage, and writes everything in one
     * atomic batch: the incident, a dispatch-log entry for each service
     * notified, the notice administrators see, and an alert for the reporter.
     */
    suspend fun submitReport(draft: ReportDraft, reporterName: String): SubmitResult {
        val uid = uid() ?: throw IllegalStateException("You need to be signed in to report an incident")
        val now = TimeFormat.nowIso()

        val urgentWords = TriageHelper.findUrgentWords(draft.description, draft.type)
        val urgent = TriageHelper.isUrgent(draft.priority, urgentWords)
        val services = if (urgent) TriageHelper.chooseServices(draft.type, urgentWords, getServices()) else emptyList()
        val dispatched = services.isNotEmpty()
        val priority = if (dispatched) "High" else draft.priority
        val title = if (draft.panic) "Emergency at ${draft.location}" else "${draft.type} at ${draft.location}"

        val incidentRef = db.collection("incidents").document()
        val batch = db.batch()

        batch.set(incidentRef, hashMapOf<String, Any?>(
            "title" to title,
            "type" to draft.type,
            "description" to draft.description,
            "priority" to priority,
            "status" to if (dispatched) "Forwarded" else "Pending",
            "location" to draft.location,
            "latitude" to draft.latitude,
            "longitude" to draft.longitude,
            "photoUrl" to draft.photoDataUrl,
            "panic" to draft.panic,
            "reporterId" to uid,
            "reporterName" to reporterName,
            "createdAt" to now,
            "forwardedTo" to services.map { it.id },
            "forwardedAt" to if (dispatched) now else null,
            "autoForwarded" to dispatched
        ))

        services.forEach { service ->
            batch.set(db.collection("dispatchLog").document(), hashMapOf<String, Any?>(
                "incidentId" to incidentRef.id,
                "serviceId" to service.id,
                "serviceName" to service.name,
                "forwardedBy" to "ai_triage",
                "forwardedAt" to now,
                "notes" to "Auto-dispatched by AI triage"
            ))
        }

        val adminMessage = when {
            draft.panic -> "Panic button pressed by $reporterName at ${draft.location}. Dispatched to ${services.size} service${if (services.size == 1) "" else "s"}."
            dispatched -> "Urgent ${draft.type} at ${draft.location} was auto-dispatched by AI triage to ${services.size} service${if (services.size == 1) "" else "s"}."
            else -> "New $priority priority ${draft.type} reported at ${draft.location}."
        }
        batch.set(db.collection("notifications").document(), hashMapOf<String, Any?>(
            "type" to "new_incident",
            "incidentId" to incidentRef.id,
            "message" to adminMessage,
            "read" to false,
            "createdAt" to now
        ))

        batch.set(db.collection("users").document(uid).collection("alerts").document(), hashMapOf<String, Any?>(
            "title" to if (dispatched) "Help is on the way" else "Report received",
            "message" to if (dispatched) "Your $priority priority report was sent to ${services.size} emergency service${if (services.size == 1) "" else "s"}."
                         else "Your report is waiting for an administrator to review it.",
            "type" to "INCIDENT_UPDATE",
            "read" to false,
            "createdAt" to now
        ))

        // withTimeoutOrNull returns the result of the block, or null on timeout.
        // Task.await() on batch.commit() returns Void (null), so we return true 
        // inside the block to distinguish success from timeout.
        val committed = withTimeoutOrNull(SERVER_WAIT_MS) { 
            batch.commit().await()
            true 
        }
        val queued = committed == null
        Log.i(TAG, "Report ${incidentRef.id} saved (dispatched=${services.size}, queued=$queued)")
        return SubmitResult(incidentRef.id, services.size, queued)
    }
}
