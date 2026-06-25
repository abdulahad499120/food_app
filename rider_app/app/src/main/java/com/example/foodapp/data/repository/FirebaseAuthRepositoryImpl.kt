package com.example.foodapp.data.repository

import android.app.Activity
import com.example.foodapp.data.models.UserProfile
import com.example.foodapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseAuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User not found")
            Result.success(
                UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "User",
                    email = user.email,
                    phoneNumber = user.phoneNumber
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String, phone: String): Result<UserProfile> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Failed to create user")
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Save to Firestore 'riders' collection
            val riderData = mapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to user.email,
                "phone" to phone,
                "vehicle" to "Motorcycle" // default vehicle
            )
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("riders").document(user.uid).set(riderData).await()
            
            Result.success(
                UserProfile(
                    uid = user.uid,
                    name = name,
                    email = user.email,
                    phoneNumber = phone
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyOtp(verificationId: String, code: String): Result<UserProfile> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User not found after OTP")
            
            Result.success(
                UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "User",
                    phoneNumber = user.phoneNumber,
                    email = user.email
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<UserProfile> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User not found")
            
            Result.success(
                UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "User",
                    email = user.email,
                    phoneNumber = user.phoneNumber
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): UserProfile? {
        val user = auth.currentUser ?: return null
        return UserProfile(
            uid = user.uid,
            name = user.displayName ?: "User",
            email = user.email,
            phoneNumber = user.phoneNumber
        )
    }
}
