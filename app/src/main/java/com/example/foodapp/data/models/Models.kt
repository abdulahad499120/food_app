package com.example.foodapp.data.models

import kotlinx.serialization.Serializable
import java.util.Date
import com.google.firebase.firestore.ServerTimestamp

@Serializable
data class Brand(
    val name: String,
    val logo: String,
    val rating: Double,
    val minimum_order: Int
)

@Serializable
data class Category(
    val id: Long,
    val name: String
)

@Serializable
data class Product(
    val id: Long,
    val category_id: Long,
    val category_name: String,
    val name: String,
    val description: String,
    val price: Double,
    val image: String
) {
    // Helper to compute local asset path from the name and id
    val localImagePath: String
        get() {
            val sanitizedName = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            return "file:///android_asset/product_pics/${sanitizedName}_${id}.jpg"
        }
}

@Serializable
data class RestaurantData(
    val brand: Brand,
    val categories: List<Category>,
    val products: List<Product>
)

data class Address(
    val houseNo: String = "",
    val street: String = "",
    val area: String = ""
) {
    val isComplete: Boolean
        get() = houseNo.isNotBlank() && street.isNotBlank() && area.isNotBlank()
        
    override fun toString(): String {
        return if (isComplete) "$houseNo, $street, $area" else "Incomplete Address"
    }
}

data class CartItem(
    val product: Product = Product(0, 0, "", "", "", 0.0, ""),
    val quantity: Int = 0
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val deliveryAddress: Address = Address(),
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "Pending",
    @ServerTimestamp val timestamp: Date? = null
)

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val branchId: String? = null
)
