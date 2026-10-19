package com.example.arts.viewmodels

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.arts.data.UserProfile
import com.example.arts.repository.UserRepository
import com.example.arts.utils.AuthErrors
import com.example.arts.utils.GoogleSignInHelper
import com.example.arts.utils.PrefsHelper
import com.example.arts.utils.SessionGate
import com.example.arts.utils.TotpHelper
import com.example.arts.utils.Validators
import kotlinx.coroutines.launch

private const val TAG = "ARTS-Auth"

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserProfile) : AuthState()
    data class RequiresTotp(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

/** Sign-in, registration, Google single sign-on and two-factor authentication, all through Firebase. */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()
    private val context = application.applicationContext

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<UserProfile?>()
    val currentUser: LiveData<UserProfile?> = _currentUser

    private val _resetMessage = MutableLiveData<String?>()
    val resetMessage: LiveData<String?> = _resetMessage

    /** True if Firebase already has a saved session on this phone. */
    fun isSignedIn(): Boolean = repository.currentUser() != null

    fun currentUid(): String? = repository.currentUser()?.uid

    fun login(email: String, password: String) {
        val problem = Validators.loginError(email, password)
        if (problem != null) {
            _authState.value = AuthState.Error(problem)
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                proceed(repository.login(email, password))
            } catch (e: Exception) {
                Log.w(TAG, "Login failed: ${e.message}")
                _authState.value = AuthState.Error(AuthErrors.friendly(e))
            }
        }
    }

    fun register(fullName: String, email: String, phone: String, password: String) {
        val problem = Validators.registrationError(fullName, email, phone, password)
        if (problem != null) {
            _authState.value = AuthState.Error(problem)
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                proceed(repository.register(fullName, email, phone, password))
            } catch (e: Exception) {
                Log.w(TAG, "Registration failed: ${e.message}")
                _authState.value = AuthState.Error(AuthErrors.friendly(e))
            }
        }
    }

    /** Single sign-on with a Google account. */
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val idToken = GoogleSignInHelper.getIdToken(activity)
                proceed(repository.signInWithGoogle(idToken))
            } catch (e: GetCredentialCancellationException) {
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                Log.w(TAG, "Google sign-in failed: ${e.message}")
                _authState.value = AuthState.Error(AuthErrors.friendly(e))
            }
        }
    }

    /** Continues with the account Firebase already has saved on this phone (after a biometric check, if that is on). */
    fun resumeSession() {
        val uid = currentUid() ?: run {
            _authState.value = AuthState.Idle
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val profile = repository.getProfile(uid)
                if (profile == null) {
                    // Profile doesn't exist yet (e.g. first launch after reinstall).
                    // Create it from the Firebase Auth account rather than signing out.
                    val user = repository.currentUser()
                    if (user != null) {
                        val created = repository.ensureProfile(user, user.displayName, null)
                        proceed(created)
                    } else {
                        repository.signOut()
                        _authState.value = AuthState.Idle
                    }
                } else {
                    proceed(profile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Resume failed: ${e.message}")
                // Don't sign the user out on a network error — let them try again
                _authState.value = AuthState.Error(AuthErrors.friendly(e))
            }
        }
    }

    /** Sends a password-reset email through Firebase. */
    fun sendPasswordReset(email: String) {
        if (!Validators.isValidEmail(email)) {
            _resetMessage.value = "Enter your email address above first"
            return
        }
        viewModelScope.launch {
            _resetMessage.value = try {
                repository.sendPasswordReset(email)
                "If an account exists for that email, a reset link is on its way"
            } catch (e: Exception) {
                AuthErrors.friendly(e)
            }
        }
    }

    fun clearResetMessage() {
        _resetMessage.value = null
    }

    private fun proceed(profile: UserProfile) {
        if (profile.totpEnabled && profile.totpSecret != null && SessionGate.totpVerifiedUid != profile.id) {
            // Signed in with Firebase, but a 2FA code is still needed before the app opens.
            _authState.value = AuthState.RequiresTotp(profile)
        } else {
            finalizeLogin(profile)
        }
    }

    /** Call after the user enters their 6-digit authenticator code. */
    fun verifyTotp(user: UserProfile, code: String) {
        val secret = user.totpSecret
        if (secret == null) {
            _authState.value = AuthState.Error("2FA is not set up for this account")
            return
        }
        if (!TotpHelper.verifyCode(secret, code)) {
            _authState.value = AuthState.Error("Incorrect code, please try again")
            return
        }
        SessionGate.totpVerifiedUid = user.id
        finalizeLogin(user)
    }

    private fun finalizeLogin(profile: UserProfile) {
        _currentUser.value = profile
        PrefsHelper.saveUser(context, profile.fullName, profile.email, profile.phone)
        _authState.value = AuthState.Success(profile)
    }

    /** Turns TOTP 2FA on for the signed-in user, storing the confirmed secret. */
    fun enableTotp(secret: String, onDone: () -> Unit) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            try {
                repository.setTotp(uid, secret, true)
                SessionGate.totpVerifiedUid = uid
                onDone()
            } catch (e: Exception) {
                Log.w(TAG, "Could not enable 2FA: ${e.message}")
                _authState.value = AuthState.Error("Could not turn on 2FA: ${AuthErrors.friendly(e)}")
            }
        }
    }

    /** Turns TOTP 2FA off for the signed-in user. */
    fun disableTotp(onDone: () -> Unit) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            try {
                repository.setTotp(uid, null, false)
                onDone()
            } catch (e: Exception) {
                Log.w(TAG, "Could not disable 2FA: ${e.message}")
                _authState.value = AuthState.Error("Could not turn off 2FA: ${AuthErrors.friendly(e)}")
            }
        }
    }

    fun logout() {
        repository.signOut()
        SessionGate.clear()
        PrefsHelper.logout(context)
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
