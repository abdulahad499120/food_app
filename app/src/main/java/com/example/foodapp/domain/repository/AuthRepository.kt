package com.example.foodapp.domain.repository

import com.example.foodapp.data.models.UserProfile
import android.app.Activity
import com.google.firebase.auth.PhoneAuthProvider

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>
    
    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<UserProfile>
    
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )
    
    suspend fun verifyOtp(verificationId: String, code: String): Result<UserProfile>
    
    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<UserProfile>
    
    fun logout()
    
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    fun getCurrentUser(): UserProfile?
}
