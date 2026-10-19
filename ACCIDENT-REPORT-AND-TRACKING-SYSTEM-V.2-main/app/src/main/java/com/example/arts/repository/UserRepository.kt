package com.example.arts.repository

import android.util.Log
import com.example.arts.data.UserProfile
import com.example.arts.data.toUserProfile
import com.example.arts.utils.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "ARTS-Users"

/**
 * Accounts and profiles, backed by Firebase Authentication and Cloud Firestore.
 * Passwords are never handled by the app: Firebase stores and verifies them.
 */
class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun currentUser(): FirebaseUser? = auth.currentUser

    private fun profileRef(uid: String): DocumentReference = db.collection("users").document(uid)

    // Two-factor secrets live in a private subdocument that only the owner can read.
    private fun securityRef(uid: String): DocumentReference =
        profileRef(uid).collection("private").document("security")

    suspend fun register(fullName: String, email: String, phone: String, password: String): UserProfile {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: throw IllegalStateException("The account could not be created")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()).await()
        Log.i(TAG, "Registered ${user.uid}")
        return ensureProfile(user, fullName.trim(), phone.trim())
    }

    suspend fun login(email: String, password: String): UserProfile {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: throw IllegalStateException("Sign-in failed")
        Log.i(TAG, "Signed in ${user.uid}")
        return ensureProfile(user, null, null)
    }

    /** Single sign-on: exchanges a Google ID token for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): UserProfile {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: throw IllegalStateException("Google sign-in failed")
        Log.i(TAG, "Signed in with Google ${user.uid}")
        return ensureProfile(user, user.displayName, null)
    }

    /** Creates users/{uid} the first time a person signs in, then returns it. */
    suspend fun ensureProfile(user: FirebaseUser, fullName: String?, phone: String?): UserProfile {
        val ref = profileRef(user.uid)
        if (!ref.get().await().exists()) {
            val name = fullName ?: user.displayName ?: user.email?.substringBefore("@") ?: "ARTS user"
            val data = hashMapOf<String, Any?>(
                "fullName" to name,
                "email" to (user.email ?: ""),
                "phone" to (phone ?: ""),
                "createdAt" to TimeFormat.nowIso(),
                "safetyScore" to 85,
                "avatarDataUrl" to null,
                "settings" to mapOf("theme" to "light", "language" to "en"),
                "idNumber" to null, "age" to null, "gender" to null, "bloodType" to null,
                "weightKg" to null, "heightCm" to null,
                "allergiesOrConditions" to null, "chronicConditions" to null, "currentMedications" to null,
                "medicalAidProvider" to null, "medicalAidNumber" to null, "preferredHospital" to null,
                "emergencyContactName" to null, "emergencyContactPhone" to null
            )
            ref.set(data).await()
            Log.i(TAG, "Created profile for ${user.uid}")
        }
        return getProfile(user.uid) ?: throw IllegalStateException("Profile could not be loaded")
    }

    suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = profileRef(uid).get().await()
        if (!snapshot.exists()) return null
        val security = try {
            securityRef(uid).get().await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read security settings: ${e.message}")
            null
        }
        return snapshot.toUserProfile(
            totpEnabled = security?.getBoolean("totpEnabled") ?: false,
            totpSecret = security?.getString("totpSecret")
        )
    }

    suspend fun updateProfile(uid: String, fullName: String, phone: String) {
        profileRef(uid).update(mapOf("fullName" to fullName, "phone" to phone)).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName).build())?.await()
    }

    suspend fun updateAvatar(uid: String, dataUrl: String) {
        profileRef(uid).update("avatarDataUrl", dataUrl).await()
    }

    /** Saves the emergency medical profile. Keys must match the field names the website uses. */
    suspend fun saveMedical(uid: String, fields: Map<String, Any?>) {
        profileRef(uid).update(fields).await()
    }

    suspend fun setTotp(uid: String, secret: String?, enabled: Boolean) {
        securityRef(uid).set(mapOf("totpSecret" to secret, "totpEnabled" to enabled)).await()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth.signOut()
    }
}
