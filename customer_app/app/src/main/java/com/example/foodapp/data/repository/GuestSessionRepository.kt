package com.example.foodapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "guest_prefs")

class GuestSessionRepository(private val context: Context) {
    
    companion object {
        private val GUEST_ACTIVE_ORDER_ID = stringPreferencesKey("guest_active_order_id")
    }

    val guestActiveOrderIdFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[GUEST_ACTIVE_ORDER_ID]
        }

    suspend fun setGuestActiveOrderId(orderId: String) {
        context.dataStore.edit { preferences ->
            preferences[GUEST_ACTIVE_ORDER_ID] = orderId
        }
    }

    suspend fun clearGuestActiveOrderId() {
        context.dataStore.edit { preferences ->
            preferences.remove(GUEST_ACTIVE_ORDER_ID)
        }
    }
}
