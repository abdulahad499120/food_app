package com.example.foodapp.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Category
import com.example.foodapp.data.models.Product
import com.example.foodapp.data.repository.RestaurantRepository
import com.example.foodapp.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class MenuUiState {
    object Loading : MenuUiState()
    data class Success(
        val activeBranch: com.example.foodapp.data.models.Branch?,
        val brandName: String,
        val brandLogo: String,
        val categories: List<Category>,
        val allProducts: List<Product>,
        val filteredProducts: List<Product>,
        val selectedCategoryId: String?,
        val activeDeliveryAddress: com.example.foodapp.data.models.Address?
    ) : MenuUiState()
    data class Error(val message: String) : MenuUiState()
}

enum class OrderFlowState {
    PICKUP,
    DELIVERY
}

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RestaurantRepository()
    private val userPrefs = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    private val _orderFlowState = MutableStateFlow(OrderFlowState.PICKUP)
    val orderFlowState: StateFlow<OrderFlowState> = _orderFlowState.asStateFlow()

    private val _activeDeliveryAddress = MutableStateFlow<com.example.foodapp.data.models.Address?>(null)
    val activeDeliveryAddress: StateFlow<com.example.foodapp.data.models.Address?> = _activeDeliveryAddress.asStateFlow()

    private var activeJob: Job? = null
    private var currentBranchId: String? = null

    fun setActiveDeliveryAddress(address: com.example.foodapp.data.models.Address?) {
        _activeDeliveryAddress.value = address
    }

    init {
        viewModelScope.launch {
            userPrefs.activeBranchIdFlow.collect { savedBranchId ->
                if (savedBranchId != null && currentBranchId == null) {
                    setActiveBranch(savedBranchId)
                } else if (currentBranchId == null) {
                    loadData()
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setOrderFlowState(state: OrderFlowState) {
        _orderFlowState.value = state
        CartManager.setOrderFlowState(state)
    }

    // Fallback for UI that calls loadData directly
    fun loadData() {
        setActiveBranch("")
    }

    /**
     * Sets the active branch and begins observing the real-time Firestore stream
     * for global products merged with branch-specific inventory overrides.
     */
    fun setActiveBranch(branchId: String) {
        if (currentBranchId == branchId) return
        currentBranchId = branchId
        
        viewModelScope.launch {
            userPrefs.saveActiveBranchId(branchId)
        }
        
        // Cancel any existing observation job
        activeJob?.cancel()
        _uiState.value = MenuUiState.Loading

        activeJob = viewModelScope.launch {
            try {
                combine(
                    repository.observeMenuForBranch(branchId),
                    repository.observeBranch(branchId),
                    _searchQuery,
                    _selectedCategoryId,
                    combine(_orderFlowState, _activeDeliveryAddress) { flowState, address -> Pair(flowState, address) }
                ) { data, branch, query, currentCategoryId, flowStateAddressPair ->
                    val (flowState, activeAddress) = flowStateAddressPair
                    
                    val flowFilteredProducts = if (flowState == OrderFlowState.DELIVERY) {
                        data.products.filter { it.isDeliverable }
                    } else {
                        data.products
                    }
                    
                    val filteredBySearch = if (query.isBlank()) {
                        flowFilteredProducts
                    } else {
                        flowFilteredProducts.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                        }
                    }

                    val finalFiltered = if (currentCategoryId == null) {
                        filteredBySearch
                    } else {
                        filteredBySearch.filter { it.category_id == currentCategoryId }
                    }

                    val validCategoryIds = filteredBySearch.map { it.category_id }.toSet()
                    val visibleCategories = if (query.isBlank()) {
                        data.categories
                    } else {
                        data.categories.filter { it.id in validCategoryIds }
                    }

                    MenuUiState.Success(
                        activeBranch = branch,
                        brandName = data.brand.name,
                        brandLogo = data.brand.logo,
                        categories = visibleCategories,
                        allProducts = flowFilteredProducts,
                        filteredProducts = finalFiltered,
                        selectedCategoryId = currentCategoryId,
                        activeDeliveryAddress = activeAddress
                    )
                }.collect { successState ->
                    _uiState.update { successState }
                }
            } catch (e: Exception) {
                _uiState.update { MenuUiState.Error(e.message ?: "Unknown error occurred") }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun seedRealData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("dump.json").bufferedReader().use { it.readText() }
                val rootObj = org.json.JSONObject(jsonString)
                val dataObj = rootObj.getJSONObject("data")
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Cleanup old mock data
                listOf("prod1", "prod2", "prod3").forEach { firestore.collection("products").document(it).delete() }
                listOf("cat1", "cat2", "cat3").forEach { firestore.collection("categories").document(it).delete() }
                
                val menus = dataObj.getJSONArray("menus")
                if (menus.length() > 0) {
                    val firstMenu = menus.getJSONObject(0)
                    val categoriesArray = firstMenu.getJSONArray("menu_categories")
                    
                    for (i in 0 until categoriesArray.length()) {
                        val cat = categoriesArray.getJSONObject(i)
                        val catId = cat.getInt("id").toString()
                        val catName = cat.getString("name")
                        
                        // Write Category
                        firestore.collection("categories").document(catId).set(mapOf("name" to catName))
                        
                        // Parse Products in this category
                        val productsArray = cat.getJSONArray("products")
                        for (j in 0 until productsArray.length()) {
                            val prod = productsArray.getJSONObject(j)
                            val prodId = prod.getInt("id").toString()
                            val prodName = prod.getString("name")
                            val desc = prod.optString("description", "")
                            
                            // Image format from Foodpanda usually has %s for width
                            var image = prod.optString("file_path", "")
                            if (image.contains("%s")) {
                                image = image.replace("%s", "400")
                            }
                            
                            // Price is inside product_variations
                            var price = 0.0
                            val variations = prod.optJSONArray("product_variations")
                            if (variations != null && variations.length() > 0) {
                                price = variations.getJSONObject(0).getDouble("price")
                            }
                            
                            // Write Product
                            firestore.collection("products").document(prodId).set(mapOf(
                                "name" to prodName,
                                "description" to desc,
                                "basePrice" to price,
                                "categoryId" to catId,
                                "imageUrl" to image
                            ))
                        }
                    }
                }
                
                // Cleanup old branches and seed real branches
                firestore.collection("branches").get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                    repository.seedPhase5Branches()
                }
                
                android.util.Log.d("SeedData", "Successfully seeded Foodpanda dump data!")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SeedData", "Failed to seed dump data", e)
            }
        }
    }
}
