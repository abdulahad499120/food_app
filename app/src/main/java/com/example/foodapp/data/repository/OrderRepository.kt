package com.example.foodapp.data.repository

import com.example.foodapp.data.models.Order
import com.google.firebase.firestore.FirebaseFirestore
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
}
