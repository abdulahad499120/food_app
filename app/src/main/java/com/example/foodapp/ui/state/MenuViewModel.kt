package com.example.foodapp.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Category
import com.example.foodapp.data.models.Product
import com.example.foodapp.data.models.RestaurantData
import com.example.foodapp.data.repository.RestaurantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class MenuUiState {
    object Loading : MenuUiState()
    data class Success(
        val brandName: String,
        val brandLogo: String,
        val categories: List<Category>,
        val allProducts: List<Product>,
        val filteredProducts: List<Product>,
        val selectedCategoryId: Long?
    ) : MenuUiState()
    data class Error(val message: String) : MenuUiState()
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RestaurantRepository(application)

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.loadRestaurantData()
                _uiState.update {
                    MenuUiState.Success(
                        brandName = data.brand.name,
                        brandLogo = data.brand.logo, // Original URL from json
                        categories = data.categories,
                        allProducts = data.products,
                        filteredProducts = data.products,
                        selectedCategoryId = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { MenuUiState.Error(e.message ?: "Unknown error occurred") }
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.update { state ->
            if (state is MenuUiState.Success) {
                val filtered = if (categoryId == null) {
                    state.allProducts
                } else {
                    state.allProducts.filter { it.category_id == categoryId }
                }
                state.copy(
                    selectedCategoryId = categoryId,
                    filteredProducts = filtered
                )
            } else {
                state
            }
        }
    }
}
