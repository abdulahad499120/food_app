package com.example.foodapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * # User Preferences Repository
 * 
 * Manages local user preferences utilizing Android DataStore.
 */
class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
        val ACTIVE_BRANCH_ID = stringPreferencesKey("active_branch_id")
    }

    /**
     * ## Observe Active Branch ID
     * 
     * Emits the saved active branch ID, or null if none is selected.
     */
    val activeBranchIdFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACTIVE_BRANCH_ID]
        }

    /**
     * ## Save Active Branch ID
     * 
     * Persists the newly selected branch ID.
     */
    suspend fun saveActiveBranchId(branchId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_BRANCH_ID] = branchId
        }
    }
}
