package com.example.foodapp.ui.state

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.domain.repository.AuthRepository
import com.example.foodapp.data.repository.FirebaseAuthRepositoryImpl
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider

class AuthViewModel : ViewModel() {
    // In a real app, inject this via Dagger/Hilt
    private val repository: AuthRepository = FirebaseAuthRepositoryImpl()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = repository.getCurrentUser()
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signInWithEmail(email, password).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Sign in failed")
                }
            )
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String, phone: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signUpWithEmail(email, password, name, phone).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                }
            )
        }
    }

    fun sendOTP(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        repository.startPhoneVerification(phoneNumber, activity, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _authState.value = AuthState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _authState.value = AuthState.OTPVerification(verificationId)
            }
        })
    }

    fun verifyOTP(verificationId: String, code: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.verifyOtp(verificationId, code).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "OTP Verification failed")
                }
            )
        }
    }

    fun handleGoogleSignInResult(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        signInWithCredential(credential)
    }

    fun handleCredentialManagerResult(credential: Credential) {
        _authState.value = AuthState.Loading
        try {
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                signInWithCredential(firebaseCredential)
            } else {
                _authState.value = AuthState.Error("Unexpected credential type")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Failed to process credential")
        }
    }

    private fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            repository.signInWithCredential(credential).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Sign in failed")
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Unauthenticated
    }
    
    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            onResult(result)
        }
    }
    
    fun resetError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun validateSignIn(email: String, password: String): String? {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        if (!email.matches(emailRegex)) {
            return "Invalid email format"
        }
        if (password.trim().isEmpty()) {
            return "Password cannot be empty"
        }
        return null
    }

    fun validateSignUp(name: String, email: String, password: String): String? {
        if (name.trim().isEmpty() || !name.matches(Regex("^[a-zA-Z\\s]+$"))) {
            return "Full name must contain only letters and spaces"
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        if (!email.matches(emailRegex)) {
            return "Invalid email format"
        }
        if (password.length < 8) {
            return "Password must be at least 8 characters"
        }
        if (!password.matches(Regex(".*[A-Z].*"))) {
            return "Password must contain at least one uppercase letter"
        }
        if (!password.matches(Regex(".*[a-z].*"))) {
            return "Password must contain at least one lowercase letter"
        }
        if (!password.matches(Regex(".*[0-9].*"))) {
            return "Password must contain at least one number"
        }
        return null
    }
}
