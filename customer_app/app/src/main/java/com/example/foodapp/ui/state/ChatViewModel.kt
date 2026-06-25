package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentOrderId: String? = null
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun initializeChat(orderId: String) {
        if (currentOrderId == orderId) return
        currentOrderId = orderId

        listenerRegistration?.remove()
        
        listenerRegistration = db.collection("delivery_chats")
            .document(orderId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val chatMessages = snapshot?.documents?.mapNotNull { doc ->
                    val msg = doc.toObject(ChatMessage::class.java)
                    msg?.copy(id = doc.id)
                } ?: emptyList()

                _messages.value = chatMessages
            }
    }

    fun sendMessage(orderId: String, text: String, senderType: String = "CUSTOMER") {
        if (text.isBlank()) return

        val newMessage = ChatMessage(
            senderType = senderType,
            text = text.trim()
        )

        viewModelScope.launch {
            try {
                db.collection("delivery_chats")
                    .document(orderId)
                    .collection("messages")
                    .add(newMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
