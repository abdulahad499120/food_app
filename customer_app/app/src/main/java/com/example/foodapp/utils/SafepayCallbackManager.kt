package com.example.foodapp.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class SafepayResult {
    data class Success(val tracker: String, val reference: String) : SafepayResult()
    object Cancelled : SafepayResult()
}

object SafepayCallbackManager {
    private val _safepayResult = MutableSharedFlow<SafepayResult>(extraBufferCapacity = 1)
    val safepayResult: SharedFlow<SafepayResult> = _safepayResult.asSharedFlow()

    fun emitSuccess(tracker: String, reference: String) {
        _safepayResult.tryEmit(SafepayResult.Success(tracker, reference))
    }

    fun emitCancelled() {
        _safepayResult.tryEmit(SafepayResult.Cancelled)
    }
}
