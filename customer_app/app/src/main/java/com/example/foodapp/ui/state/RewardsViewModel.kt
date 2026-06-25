package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.RedemptionItem
import com.example.foodapp.data.models.StarHistory
import com.example.foodapp.data.models.UserProfile
import com.example.foodapp.data.repository.FirebaseAuthRepositoryImpl
import com.example.foodapp.data.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log

class RewardsViewModel(
    private val authRepository: FirebaseAuthRepositoryImpl = FirebaseAuthRepositoryImpl(),
    private val rewardsRepository: RewardsRepository = RewardsRepository()
) : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _starHistory = MutableStateFlow<List<StarHistory>>(emptyList())
    val starHistory: StateFlow<List<StarHistory>> = _starHistory.asStateFlow()

    private val _redemptionItems = MutableStateFlow<List<RedemptionItem>>(emptyList())
    val redemptionItems: StateFlow<List<RedemptionItem>> = _redemptionItems.asStateFlow()

    init {
        loadRewardsData()
    }

    private fun loadRewardsData() {
        val user = authRepository.getCurrentUser()
        _redemptionItems.value = rewardsRepository.getRedemptionItems()

        if (user != null) {
            viewModelScope.launch {
                try {
                    // Ensure seeded for testing
                    rewardsRepository.seedUserRewards(user.uid)
                } catch (e: Exception) {
                    Log.e("RewardsViewModel", "Failed to seed rewards: ${e.message}")
                }
                
                launch {
                    try {
                        rewardsRepository.observeUserRewards(user.uid).collectLatest { profile ->
                            _userProfile.value = profile
                        }
                    } catch (e: Exception) {
                        Log.e("RewardsViewModel", "Failed to observe user rewards: ${e.message}")
                    }
                }
                launch {
                    try {
                        rewardsRepository.observeStarHistory(user.uid).collectLatest { history ->
                            _starHistory.value = history
                        }
                    } catch (e: Exception) {
                        Log.e("RewardsViewModel", "Failed to observe star history: ${e.message}")
                    }
                }
            }
        }
    }
}
