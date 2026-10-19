package com.example.arts.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.arts.data.AppAlert
import com.example.arts.data.QueryLiveData
import com.example.arts.data.toAppAlert
import com.example.arts.utils.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The messages on the Alerts screen, stored privately under users/{uid}/alerts. */
class AlertRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun collection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("users").document(uid).collection("alerts")
    }

    val allAlerts: LiveData<List<AppAlert>> = collection()?.let { col ->
        QueryLiveData(col) { snapshot ->
            snapshot.documents
                .map { it.toAppAlert() }
                .sortedByDescending { TimeFormat.parse(it.createdAt) ?: 0L }
        }
    } ?: MutableLiveData(emptyList())

    val unreadCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(allAlerts) { alerts -> value = alerts.count { !it.read } }
    }

    suspend fun markAsRead(id: String) {
        collection()?.document(id)?.update("read", true)?.await()
    }

    suspend fun markAllAsRead(alerts: List<AppAlert>) {
        val col = collection() ?: return
        val batch = db.batch()
        alerts.filter { !it.read }.forEach { batch.update(col.document(it.id), "read", true) }
        batch.commit().await()
    }

    suspend fun delete(id: String) {
        collection()?.document(id)?.delete()?.await()
    }

    suspend fun add(title: String, message: String, type: String) {
        collection()?.add(
            hashMapOf<String, Any?>(
                "title" to title,
                "message" to message,
                "type" to type,
                "read" to false,
                "createdAt" to TimeFormat.nowIso()
            )
        )?.await()
    }
}
