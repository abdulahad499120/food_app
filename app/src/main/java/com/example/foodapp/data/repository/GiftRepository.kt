package com.example.foodapp.data.repository

import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.data.models.Gift
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GiftRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun observeGiftTemplates(): Flow<List<GiftTemplate>> = callbackFlow {
        val listener = firestore.collection("gift_templates_v2")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val templates = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GiftTemplate::class.java)?.copy(templateId = doc.id)
                } ?: emptyList()

                trySend(templates)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendGift(gift: Gift): Result<Unit> {
        return try {
            val giftRef = firestore.collection("gifts").document()
            giftRef.set(gift.copy(id = giftRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenForPendingGifts(userEmail: String): Flow<List<Gift>> = callbackFlow {
        val listener = firestore.collection("gifts")
            .whereEqualTo("recipientEmail", userEmail)
            .whereEqualTo("isClaimed", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val gifts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Gift::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(gifts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun claimGift(giftId: String, userId: String, amount: Double): Result<Unit> {
        return try {
            val giftRef = firestore.collection("gifts").document(giftId)
            val userRef = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                val giftSnapshot = transaction.get(giftRef)
                val isClaimed = giftSnapshot.getBoolean("isClaimed") ?: false
                if (isClaimed) {
                    throw Exception("Gift has already been claimed.")
                }

                transaction.update(giftRef, "isClaimed", true)
                transaction.update(userRef, "walletBalance", com.google.firebase.firestore.FieldValue.increment(amount))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedGiftTemplates() {
        try {
            val collection = firestore.collection("gift_templates_v2")
            val snapshot = collection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val templates = listOf(
                    GiftTemplate("t1", "android.resource://com.example.foodapp/drawable/gift_card_birthday", "Birthday"),
                    GiftTemplate("t2", "android.resource://com.example.foodapp/drawable/gift_card_thank_you", "Thank You"),
                    GiftTemplate("t3", "android.resource://com.example.foodapp/drawable/gift_card_coffee", "Coffee & Dry Fruits Lover")
                )
                val batch = firestore.batch()
                for (t in templates) {
                    val doc = collection.document(t.templateId)
                    batch.set(doc, t)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            // Ignore if permission denied or offline
        }
    }
}
