package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.data.models.eGift
import com.example.foodapp.data.repository.FirebaseAuthRepositoryImpl
import com.example.foodapp.data.repository.GiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GiftViewModel(
    private val authRepository: FirebaseAuthRepositoryImpl = FirebaseAuthRepositoryImpl(),
    private val giftRepository: GiftRepository = GiftRepository()
) : ViewModel() {

    private val _templates = MutableStateFlow<List<GiftTemplate>>(emptyList())
    val templates: StateFlow<List<GiftTemplate>> = _templates.asStateFlow()

    init {
        loadData()
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

    fun submitGift(
        recipientName: String,
        recipientEmail: String,
        amount: Int,
        message: String,
        templateId: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            onError(Exception("User not authenticated"))
            return
        }

        viewModelScope.launch {
            val gift = eGift(
                senderId = user.uid,
                recipientName = recipientName,
                recipientEmail = recipientEmail,
                amount = amount,
                message = message,
                designTemplateId = templateId
            )
            val result = giftRepository.submitEGiftOrder(gift)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull() ?: Exception("Unknown error submitting gift"))
            }
        }
    }
}
