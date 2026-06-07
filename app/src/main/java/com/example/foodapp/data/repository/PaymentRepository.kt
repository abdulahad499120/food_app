package com.example.foodapp.data.repository

import com.example.foodapp.data.models.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * # H1 Payment Repository
 * 
 * Manages CRUD operations for the user's tokenized payment methods in Firestore.
 */
class PaymentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /**
     * ## H2 Observe User Payments
     * 
     * Returns a real-time flow of all saved payment methods for a given user.
     */
    fun getUserPayments(userId: String): Flow<List<PaymentMethod>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val payments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(payments)
            }

        awaitClose { listener.remove() }
    }

    /**
     * ## H2 Save Payment Method
     * 
     * Saves a new payment method. If [isDefault] is true, it performs a batch write 
     * to unset any existing default payment methods.
     */
    suspend fun savePaymentMethod(userId: String, payment: PaymentMethod): Result<Unit> {
        return try {
            val paymentsRef = usersCollection.document(userId).collection("payments")
            val documentRef = if (payment.id.isEmpty()) {
                paymentsRef.document()
            } else {
                paymentsRef.document(payment.id)
            }

            val paymentToSave = payment.copy(id = documentRef.id, userId = userId)

            if (paymentToSave.isDefault) {
                // Batch write to unset default on other payments
                val batch = db.batch()
                val allPaymentsSnapshot = paymentsRef.get().await()

                for (doc in allPaymentsSnapshot.documents) {
                    if (doc.id != paymentToSave.id) {
                        val isDefault = doc.getBoolean("isDefault") ?: false
                        if (isDefault) {
                            batch.update(doc.reference, "isDefault", false)
                        }
                    }
                }
                batch.set(documentRef, paymentToSave)
                batch.commit().await()
            } else {
                documentRef.set(paymentToSave).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ## H2 Delete Payment Method
     * 
     * Removes the specified payment method from Firestore.
     */
    suspend fun deletePaymentMethod(userId: String, paymentId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).collection("payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
