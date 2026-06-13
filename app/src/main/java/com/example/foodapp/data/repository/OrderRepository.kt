package com.example.foodapp.data.repository

import com.example.foodapp.data.models.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    suspend fun placeOrder(order: Order): Result<String> {
        return try {
            // Let Firestore generate an ID if we don't supply one
            val documentRef = if (order.orderId.isEmpty()) {
                ordersCollection.document()
            } else {
                ordersCollection.document(order.orderId)
            }
            
            val orderToSave = order.copy(orderId = documentRef.id)
            documentRef.set(orderToSave).await()
            
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes the user's past orders.
     * 
     * IMPORTANT: This query uses both .whereEqualTo("userId", userId) and 
     * .orderBy("timestamp", Query.Direction.DESCENDING). Firestore requires a 
     * Composite Index for this. If this query fails to emit data, check your 
     * Logcat console for an error containing a direct Firebase URL to automatically 
     * generate this index.
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                    } catch (e: Exception) {
                        // Skip corrupted/old documents that fail to deserialize
                        null
                    }
                } ?: emptyList()
                
                trySend(orders)
            }
            
        awaitClose { listener.remove() }
    }

    suspend fun getOrder(orderId: String): Result<Order?> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            if (doc.exists()) {
                val order = doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                Result.success(order)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(orderId: String, rating: Int, reviewText: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "rating" to rating,
                    "reviewText" to reviewText
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes a single active order for real-time tracking updates.
     */
    fun observeActiveOrder(orderId: String): Flow<Order?> = callbackFlow {
        val listener = ordersCollection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val order = snapshot.toObject(Order::class.java)?.copy(orderId = snapshot.id)
                        trySend(order)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
            
        awaitClose { listener.remove() }
    }
}
