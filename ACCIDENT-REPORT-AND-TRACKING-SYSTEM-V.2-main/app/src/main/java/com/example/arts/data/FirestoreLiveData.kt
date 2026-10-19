package com.example.arts.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

private const val TAG = "ARTS-Firestore"

/**
 * Live view of a Firestore query. The listener is attached only while a
 * screen is observing and removed when it stops, so no data is fetched
 * for a screen nobody can see.
 */
class QueryLiveData<T>(
    private val query: Query,
    private val mapper: (QuerySnapshot) -> T
) : LiveData<T>() {

    private var registration: ListenerRegistration? = null

    override fun onActive() {
        registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Query listener failed: ${error.code} ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) value = mapper(snapshot)
        }
    }

    override fun onInactive() {
        registration?.remove()
        registration = null
    }
}

/** Live view of a single Firestore document. */
class DocumentLiveData<T>(
    private val reference: DocumentReference,
    private val mapper: (DocumentSnapshot) -> T
) : LiveData<T>() {

    private var registration: ListenerRegistration? = null

    override fun onActive() {
        registration = reference.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Document listener failed: ${error.code} ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) value = mapper(snapshot)
        }
    }

    override fun onInactive() {
        registration?.remove()
        registration = null
    }
}
