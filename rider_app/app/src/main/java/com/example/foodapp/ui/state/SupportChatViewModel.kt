package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import com.example.foodapp.data.models.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SupportChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _messagesState = MutableStateFlow<List<Message>>(emptyList())
    val messagesState: StateFlow<List<Message>> = _messagesState.asStateFlow()

    private var currentOrderId: String? = null
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _initialPrompt = MutableStateFlow<String?>(null)
    val initialPrompt: StateFlow<String?> = _initialPrompt.asStateFlow()

    fun initialize(orderId: String, prompt: String? = null) {
        if (currentOrderId == orderId) return
        currentOrderId = orderId
        _initialPrompt.value = prompt
        
        // Setup Firestore listener for messages
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection("support_chats")
            .document(orderId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    _messagesState.value = messages
                }
            }
            
        // Ensure chat document exists
        val chatRef = firestore.collection("support_chats").document(orderId)
        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                chatRef.set(mapOf(
                    "chatId" to orderId,
                    "orderId" to orderId,
                    "status" to "OPEN"
                ))
            }
        }
    }

    fun sendMessage(text: String) {
        val orderId = currentOrderId ?: return
        if (text.isBlank()) return

        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            sender = "USER",
            text = text.trim(),
            timestamp = null // Will be populated by ServerTimestamp
        )

        firestore.collection("support_chats")
            .document(orderId)
            .collection("messages")
            .document(messageId)
            .set(message)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
