package com.example.foodapp.data.repository

import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.data.models.eGift
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GiftRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun observeGiftTemplates(): Flow<List<GiftTemplate>> = callbackFlow {
        val listener = firestore.collection("gift_templates")
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

    suspend fun submitEGiftOrder(gift: eGift): Result<Unit> {
        return try {
            val giftRef = firestore.collection("gifts").document()
            giftRef.set(gift.copy(id = giftRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedGiftTemplates() {
        try {
            val collection = firestore.collection("gift_templates")
            val snapshot = collection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val templates = listOf(
                    GiftTemplate("t1", "https://mock.url/birthday", "Birthday"),
                    GiftTemplate("t2", "https://mock.url/graduation", "Graduation"),
                    GiftTemplate("t3", "https://mock.url/thank_you", "Thank You"),
                    GiftTemplate("t4", "https://mock.url/coffee", "Dry Fruit Lover"),
                    GiftTemplate("t5", "https://mock.url/congrats", "Congratulations")
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
