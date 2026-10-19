package com.example.arts.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.arts.data.UserProfile
import com.example.arts.repository.IncidentRepository
import com.example.arts.repository.UserRepository
import com.example.arts.utils.PrefsHelper
import kotlinx.coroutines.launch

private const val TAG = "ARTS-ProfileVM"

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val incidentRepository = IncidentRepository()
    private val context = application.applicationContext

    private val _user = MutableLiveData<UserProfile?>()
    val user: LiveData<UserProfile?> = _user

    private val _totalReports = MutableLiveData(0)
    val totalReports: LiveData<Int> = _totalReports

    private val _resolvedReports = MutableLiveData(0)
    val resolvedReports: LiveData<Int> = _resolvedReports

    private val _updateSuccess = MutableLiveData(false)
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    init {
        loadUser()
        loadStats()
    }

    fun loadUser() {
        val uid = userRepository.currentUser()?.uid ?: return
        viewModelScope.launch {
            _user.value = try {
                userRepository.getProfile(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load profile: ${e.message}")
                _user.value
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            val incidents = try {
                incidentRepository.getMyIncidents()
            } catch (e: Exception) {
                Log.w(TAG, "Could not load report counts: ${e.message}")
                emptyList()
            }
            _totalReports.value = incidents.size
            _resolvedReports.value = incidents.count { it.status == "Resolved" }
        }
    }

    fun updateUser(fullName: String, phone: String) {
        val current = _user.value ?: return
        viewModelScope.launch {
            try {
                userRepository.updateProfile(current.id, fullName, phone)
                _user.value = current.copy(fullName = fullName, phone = phone)
                PrefsHelper.saveUser(context, fullName, current.email, phone)
                _updateSuccess.value = true
            } catch (e: Exception) {
                Log.w(TAG, "Could not update profile: ${e.message}")
                _updateSuccess.value = false
            }
        }
    }

    /** Saves the emergency medical profile. Throws if the write fails, so the caller can tell the person. */
    suspend fun saveMedical(fields: Map<String, Any?>) {
        val current = _user.value ?: throw IllegalStateException("Profile is not loaded yet")
        userRepository.saveMedical(current.id, fields)
        loadUser()
    }

    /** Saves a new profile photo (a data URL). Throws if the write fails. */
    suspend fun saveAvatar(dataUrl: String) {
        val current = _user.value ?: throw IllegalStateException("Profile is not loaded yet")
        userRepository.updateAvatar(current.id, dataUrl)
        _user.value = current.copy(avatarDataUrl = dataUrl)
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
