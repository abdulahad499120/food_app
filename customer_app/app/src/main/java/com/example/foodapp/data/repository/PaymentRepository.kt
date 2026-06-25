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

    suspend fun getSafepayCustomerId(userId: String): String? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.getString("safepayCustomerId")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setSafepayCustomerId(userId: String, customerId: String) {
        try {
            usersCollection.document(userId).set(
                mapOf("safepayCustomerId" to customerId),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("PaymentRepo", "Failed to save safepayCustomerId: ${e.message}")
        }
    }

    /**
     * ## H2 Observe User Payments
     * 
     * Returns a real-time flow of all saved payment methods for a given user.
     */
    fun getUserPayments(userId: String): Flow<List<PaymentMethod>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("PaymentRepo", "Firestore listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                android.util.Log.d("PaymentRepo", "Firestore snapshot received: ${snapshot?.documents?.size ?: 0} documents")

                val payments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val payment = doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
                        if (payment == null) {
                            android.util.Log.e("PaymentRepo", "toObject() returned NULL for doc: ${doc.id} data: ${doc.data}")
                        } else {
                            android.util.Log.d("PaymentRepo", "Parsed payment: id=${payment.id} category=${payment.category} last4=${payment.last4}")
                        }
                        payment
                    } catch (e: Exception) {
                        android.util.Log.e("PaymentRepo", "Exception parsing doc ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                android.util.Log.d("PaymentRepo", "Emitting ${payments.size} payments")
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

    /**
     * ## H2 Set Default Payment
     *
     * Batch write: clears isDefault on ALL existing payment docs,
     * then sets isDefault=true only on the chosen one.
     * This is the correct fix for the ghost-default bug.
     */
    suspend fun setDefaultPayment(userId: String, paymentId: String): Result<Unit> {
        return try {
            val paymentsRef = usersCollection.document(userId).collection("payments")
            val snapshot = paymentsRef.get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "isDefault", doc.id == paymentId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PaymentRepo", "setDefaultPayment failed: ${e.message}")
            Result.failure(e)
        }
    }

}
