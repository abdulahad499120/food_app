package com.example.foodapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavoritesRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getFavorites(userId: String): Flow<List<String>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                @Suppress("UNCHECKED_CAST")
                val favorites = snapshot?.get("favorites") as? List<String> ?: emptyList()
                trySend(favorites)
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleFavorite(userId: String, productId: String, isCurrentlyFavorite: Boolean): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(userId)
            val updateData = if (isCurrentlyFavorite) {
                mapOf("favorites" to FieldValue.arrayRemove(productId))
            } else {
                mapOf("favorites" to FieldValue.arrayUnion(productId))
            }
            // Use set with merge to ensure the document gets created if it doesn't exist
            userRef.set(updateData, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
