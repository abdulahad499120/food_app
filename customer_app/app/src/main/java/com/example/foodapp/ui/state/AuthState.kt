package com.example.foodapp.ui.state

import com.example.foodapp.data.models.UserProfile

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class OTPVerification(val verificationId: String) : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}
