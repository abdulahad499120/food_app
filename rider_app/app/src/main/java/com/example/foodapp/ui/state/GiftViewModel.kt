package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.data.models.Gift
import com.example.foodapp.data.repository.FirebaseAuthRepositoryImpl
import com.example.foodapp.data.repository.GiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch

class GiftViewModel(
    private val authRepository: FirebaseAuthRepositoryImpl = FirebaseAuthRepositoryImpl(),
    private val giftRepository: GiftRepository = GiftRepository()
) : ViewModel() {

    private val _templates = MutableStateFlow<List<GiftTemplate>>(emptyList())
    val templates: StateFlow<List<GiftTemplate>> = _templates.asStateFlow()

    private val _pendingGifts = MutableStateFlow<List<Gift>>(emptyList())
    val pendingGifts: StateFlow<List<Gift>> = _pendingGifts.asStateFlow()

    init {
        loadData()
        observePendingGifts()
    }

    private fun loadData() {
        viewModelScope.launch {
            giftRepository.seedGiftTemplates()
            giftRepository.observeGiftTemplates()
                .catch { e ->
                    // Handle permission denied or network errors gracefully
                    _templates.value = emptyList()
                }
                .collectLatest { fetchedTemplates ->
                    _templates.value = fetchedTemplates
                }
        }
    }

    private fun observePendingGifts() {
        viewModelScope.launch {
            val authStateFlow = callbackFlow {
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
                    trySend(auth.currentUser)
                }
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }
            
            authStateFlow.collectLatest { user ->
                if (user?.email != null) {
                    try {
                        giftRepository.listenForPendingGifts(user.email!!).collectLatest { gifts ->
                            _pendingGifts.value = gifts
                        }
                    } catch (e: Exception) {
                        _pendingGifts.value = emptyList()
                    }
                } else {
                    _pendingGifts.value = emptyList()
                }
            }
        }
    }

    fun submitGift(
        recipientName: String,
        recipientEmail: String,
        amount: Int,
        message: String,
        templateId: String,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (!com.example.foodapp.utils.NetworkUtils.isNetworkAvailable(context)) {
            onError(Exception("No internet connection. Please check your network and try again."))
            return
        }

        val user = authRepository.getCurrentUser()
        if (user == null) {
            onError(Exception("User not authenticated"))
            return
        }

        viewModelScope.launch {
            val gift = Gift(
                id = "",
                senderName = user.name ?: "Unknown Sender",
                recipientEmail = recipientEmail,
                amount = amount.toDouble(),
                message = message,
                themeUrl = "https://mock.url/${templateId}", // using mock based on template
                isClaimed = false,
                timestamp = System.currentTimeMillis()
            )
            val result = giftRepository.sendGift(gift)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull() ?: Exception("Unknown error submitting gift"))
            }
        }
    }

    fun claimGift(gift: Gift, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            onError(Exception("User not authenticated"))
            return
        }

        viewModelScope.launch {
            val result = giftRepository.claimGift(gift.id, user.uid, gift.amount)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull() ?: Exception("Failed to claim gift"))
            }
        }
    }
}
