package com.example.foodapp.data.repository

import com.example.foodapp.data.models.Address
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AddressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /**
     * Observes the user's saved addresses in real-time.
     */
    fun getUserAddresses(userId: String): Flow<List<Address>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("addresses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val addresses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Address::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(addresses)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Saves a new address or updates an existing one. 
     * If isDefault is true, it triggers a batch write to set all other addresses to isDefault = false.
     */
    suspend fun saveAddress(userId: String, address: Address): Result<Unit> {
        return try {
            val addressesRef = usersCollection.document(userId).collection("addresses")
            val documentRef = if (address.id.isEmpty()) {
                addressesRef.document()
            } else {
                addressesRef.document(address.id)
            }

            val addressToSave = address.copy(id = documentRef.id)

            if (addressToSave.isDefault) {
                // Batch write to set all others to false
                val batch = db.batch()
                val allAddressesSnapshot = addressesRef.get().await()

                for (doc in allAddressesSnapshot.documents) {
                    if (doc.id != addressToSave.id) {
                        val isDefault = doc.getBoolean("isDefault") ?: false
                        if (isDefault) {
                            batch.update(doc.reference, "isDefault", false)
                        }
                    }
                }
                
                // Add the new/updated address to the batch
                batch.set(documentRef, addressToSave)
                batch.commit().await()
            } else {
                // Just save the single address
                documentRef.set(addressToSave).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an address.
     */
    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
