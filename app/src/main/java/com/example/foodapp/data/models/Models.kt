package com.example.foodapp.data.models

import kotlinx.serialization.Serializable
import java.util.Date
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.GeoPoint

// ---- Firestore Models ----

data class FirestoreProduct(
    val productId: String = "",
    val name: String = "",
    val description: String = "",
    val basePrice: Double = 0.0,
    val categoryId: String = "",
    val imageUrl: String = "",
    val requiresCustomization: Boolean = false,
    val baseCalories: Int = 0,
    val largeCalorieBonus: Int = 0,
    val ingredientCalorieMap: Map<String, Int> = emptyMap(),
    val isFeatured: Boolean = false,
    val isDeliverable: Boolean = true
)

data class Branch(
    val branchId: String = "",
    val name: String = "",
    val location: GeoPoint? = null,
    val isOpen: Boolean = true,
    val status: String = "OPEN",
    val operatingHours: String = "",
    val address: String = ""
)

data class InventoryOverride(
    // The document ID in Firestore is the productId
    @field:JvmField
    val isAvailable: Boolean = true,
    val priceOverride: Double? = null
)

// ---- Domain / UI Models ----

@androidx.compose.runtime.Stable
@Serializable
data class Brand(
    val name: String = "",
    val logo: String = "",
    val rating: Double = 0.0,
    val minimum_order: Int = 0
)

@androidx.compose.runtime.Stable
@Serializable
data class Category(
    val id: String = "",
    val name: String = ""
)

@androidx.compose.runtime.Stable
@Serializable
data class Product(
    val id: String = "",
    val category_id: String = "",
    val category_name: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val image: String = "",
    val isAvailable: Boolean = true,
    val requiresCustomization: Boolean = false,
    val baseCalories: Int = 0,
    val largeCalorieBonus: Int = 0,
    val ingredientCalorieMap: Map<String, Int> = emptyMap(),
    val isFeatured: Boolean = false,
    val isDeliverable: Boolean = true
) {
    @get:Exclude
    val localImagePath: String
        get() {
            val sanitizedName = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            return "file:///android_asset/product_pics/${sanitizedName}_${id}.jpg"
        }
}

@Serializable
data class RestaurantData(
    val brand: Brand = Brand(),
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList()
)

data class Address(
    val id: String = "",
    val label: String = "Home",
    val streetAddress: String = "",
    val city: String = "",
    val postalCode: String = "",
    val phoneNumber: String = "",
    val deliveryInstructions: String = "",
    val location: GeoPoint? = null,
    val isDefault: Boolean = false
) {
    @get:Exclude
    val isComplete: Boolean
        get() = streetAddress.isNotBlank()
        
    override fun toString(): String {
        return streetAddress.ifBlank { "Incomplete Address" }
    }
}

@androidx.compose.runtime.Stable
data class CartItem(
    val product: Product = Product(),
    val quantity: Int = 0,
    val size: String = "Regular", // Default to Regular
    val sweetness: Int = 2,
    val extraToppings: Int = 0,
    val nutType: String = "Mixed Nuts",
    val scoops: Int = 2
)

enum class OrderStatus {
    GRACE_PERIOD,
    PENDING,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val deliveryAddress: Address = Address(),
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val branchLocation: GeoPoint? = null,
    val deliveryLocation: GeoPoint? = null,
    val rating: Int? = null,
    val reviewText: String? = null,
    val isHiddenLocally: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)

@androidx.compose.runtime.Stable
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val branchId: String? = null,
    val starsBalance: Int = 0,
    val loyaltyTier: String = "Green",
    val walletBalance: Double = 0.0,
    val favorites: List<String> = emptyList()
)

data class StarHistory(
    val id: String = "",
    val orderId: String = "",
    val starsEarned: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)

data class RedemptionItem(
    val id: String = "",
    val itemName: String = "",
    val iconUrl: String = "",
    val costInStars: Int = 0
)

@androidx.compose.runtime.Stable
data class Gift(
    val id: String,
    val senderName: String,
    val recipientEmail: String,
    val amount: Double,
    val message: String,
    val themeUrl: String,
    val isClaimed: Boolean = false,
    val timestamp: Long
)

data class GiftTemplate(
    val templateId: String = "",
    val imageUrl: String = "",
    val category: String = ""
)

data class Message(
    val messageId: String = "",
    val sender: String = "USER", // "USER" or "SUPPORT"
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
