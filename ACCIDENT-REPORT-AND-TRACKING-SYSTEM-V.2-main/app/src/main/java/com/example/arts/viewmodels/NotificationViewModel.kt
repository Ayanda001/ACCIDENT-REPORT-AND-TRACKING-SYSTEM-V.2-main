package com.example.arts.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.arts.data.AppAlert
import com.example.arts.repository.AlertRepository
import kotlinx.coroutines.launch

/** The Alerts screen: the signed-in person's messages, updating live. */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlertRepository()

    val allNotifications: LiveData<List<AppAlert>> = repository.allAlerts
    val unreadCount: LiveData<Int> = repository.unreadCount

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun markAsRead(id: String) {
        viewModelScope.launch { runCatching { repository.markAsRead(id) } }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            runCatching { repository.markAllAsRead(allNotifications.value.orEmpty()) }
        }
    }

    fun deleteNotification(alert: AppAlert) {
        viewModelScope.launch { runCatching { repository.delete(alert.id) } }
    }

    fun addNotification(title: String, message: String, type: String) {
        viewModelScope.launch { runCatching { repository.add(title, message, type) } }
    }
}
