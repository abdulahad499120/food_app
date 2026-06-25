package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    private val favoritesRepository = FavoritesRepository()

    private val _favoriteProductIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteProductIds: StateFlow<List<String>> = _favoriteProductIds.asStateFlow()

    private var currentUserId: String? = null

    fun loadFavoritesForUser(userId: String?) {
        if (userId == currentUserId) return
        currentUserId = userId
        
        if (userId == null) {
            _favoriteProductIds.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                favoritesRepository.getFavorites(userId).collectLatest { favs ->
                    _favoriteProductIds.value = favs
                }
            } catch (e: Exception) {
                // Ignore exception to prevent app crash if permission is denied
                _favoriteProductIds.value = emptyList()
            }
        }
    }

    fun toggleFavorite(productId: String) {
        val userId = currentUserId ?: return
        val isFavorite = _favoriteProductIds.value.contains(productId)
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(userId, productId, isFavorite)
        }
    }
}
