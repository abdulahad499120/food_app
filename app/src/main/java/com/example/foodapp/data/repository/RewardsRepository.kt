package com.example.foodapp.data.repository

import com.example.foodapp.data.models.RedemptionItem
import com.example.foodapp.data.models.StarHistory
import com.example.foodapp.data.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.tasks.await
import java.util.Date
import android.util.Log

class RewardsRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun observeUserRewards(userId: String): Flow<UserProfile> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RewardsRepository", "Error observing user profile: ${error.message}")
                    trySend(UserProfile(uid = userId)) // Send default instead of crashing
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val userProfile = snapshot.toObject(UserProfile::class.java)?.copy(uid = snapshot.id)
                        ?: UserProfile(uid = userId)
                    trySend(userProfile)
                } else {
                    // Fallback empty if doesn't exist
                    trySend(UserProfile(uid = userId))
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeStarHistory(userId: String): Flow<List<StarHistory>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("stars_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RewardsRepository", "Error observing star history: ${error.message}")
                    trySend(emptyList()) // Send default instead of crashing
                    return@addSnapshotListener
                }

                val history = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(StarHistory::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(history)
            }
        awaitClose { listener.remove() }
    }

    // Mocked redemption items since they aren't fully dynamic yet
    fun getRedemptionItems(): List<RedemptionItem> {
        return listOf(
            RedemptionItem("r1", "Extra Premium Scoop", "", 25),
            RedemptionItem("r2", "Free Mixed Nuts Topping", "", 50),
            RedemptionItem("r3", "Any Mini Dessert", "", 150),
            RedemptionItem("r4", "Free Premium Box", "", 200),
            RedemptionItem("r5", "Free Pack of Pistachios", "", 400)
        )
    }

    suspend fun seedUserRewards(userId: String) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val snapshot = userRef.get().await()
        
        // Only seed if it doesn't already have stars
        val currentStars = snapshot.getLong("starsBalance") ?: 0L
        if (currentStars == 0L) {
            userRef.set(
                mapOf(
                    "starsBalance" to 120,
                    "loyaltyTier" to "Gold"
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            // Add dummy history
            val historyRef = userRef.collection("stars_history")
            historyRef.add(
                mapOf(
                    "orderId" to "ORDER_123",
                    "starsEarned" to 50,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            
            historyRef.add(
                mapOf(
                    "orderId" to "ORDER_124",
                    "starsEarned" to 70,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        }
        } catch (e: Exception) {
            Log.e("RewardsRepository", "Error seeding rewards: ${e.message}")
        }
    }
}
