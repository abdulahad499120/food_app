package com.example.foodapp.utils

import com.github.devnied.emvnfccard.model.EmvCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NfcManager {
    private val _scannedCards = MutableSharedFlow<EmvCard>(extraBufferCapacity = 1)
    val scannedCards = _scannedCards.asSharedFlow()
    
    var isListening = false

    fun emitCard(card: EmvCard) {
        _scannedCards.tryEmit(card)
    }
}
