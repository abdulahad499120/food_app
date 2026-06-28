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
     * IMPORTANT: We removed the Firestore orderBy and composite filters to avoid 
     * requiring the user to manually create a Composite Index in the Firebase Console.
     * Sorting is now handled locally.
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
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
                }?.sortedByDescending { it.timestamp?.time ?: 0L } ?: emptyList()
                
                trySend(orders)
            }
            
        awaitClose { listener.remove() }
    }

    /**
     * Observes the user's non-hidden orders for the new Order Management Dashboard.
     */
    fun listenToUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.filter { !it.isHiddenLocally }
                 ?.sortedByDescending { it.timestamp?.time ?: 0L } 
                 ?: emptyList()
                
                trySend(orders)
            }
            
        awaitClose { listener.remove() }
    }

    /**
     * Observes a single order for real-time tracking updates (like driver location and status changes).
     */
    fun observeOrder(orderId: String): Flow<Order?> = callbackFlow {
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

    suspend fun cancelActiveOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update("orderStatus", com.example.foodapp.data.models.OrderStatus.CANCELLED).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hideOrderFromHistory(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update("isHiddenLocally", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun claimOrderTransaction(orderId: String, riderId: String, riderName: String, riderVehicle: String, riderPhone: String): Result<Unit> {
        return try {
            val docRef = ordersCollection.document(orderId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentRiderId = snapshot.getString("riderId")
                
                if (currentRiderId != null) {
                    throw Exception("Order already claimed by another rider.")
                }
                
                transaction.update(docRef, mapOf(
                    "riderId" to riderId,
                    "riderName" to riderName,
                    "riderVehicle" to riderVehicle,
                    "riderPhone" to riderPhone
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAvailableJobs(branchId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("branchId", branchId)
            .whereEqualTo("orderStatus", com.example.foodapp.data.models.OrderStatus.PREPARING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val order = doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                        if (order?.riderId == null) order else null
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.timestamp?.time ?: 0L } ?: emptyList()
                
                trySend(orders)
            }
            
        awaitClose { listener.remove() }
    }

    /**
     * Observes the rider's completed deliveries.
     */
    fun observeRiderOrderHistory(riderId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("riderId", riderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(orderId = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.filter { it.orderStatus == com.example.foodapp.data.models.OrderStatus.DELIVERED }
                 ?.sortedByDescending { it.timestamp?.time ?: 0L }
                 ?: emptyList()
                
                trySend(orders)
            }
            
        awaitClose { listener.remove() }
    }
}
