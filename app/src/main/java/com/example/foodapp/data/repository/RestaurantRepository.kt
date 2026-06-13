package com.example.foodapp.data.repository

import com.example.foodapp.data.models.Branch
import com.example.foodapp.data.models.Category
import com.example.foodapp.data.models.FirestoreProduct
import com.example.foodapp.data.models.InventoryOverride
import com.example.foodapp.data.models.Product
import com.example.foodapp.data.models.RestaurantData
import com.example.foodapp.data.models.Brand
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

class RestaurantRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Default brand configuration
    private val defaultBrand = Brand(
        name = "Ice Land", 
        logo = "https://example.com/logo.png", 
        rating = 4.5, 
        minimum_order = 500
    )

    private fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val categories = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(categories)
        }
        awaitClose { listener.remove() }
    }

    private fun getProducts(): Flow<List<FirestoreProduct>> = callbackFlow {
        val listener = firestore.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(FirestoreProduct::class.java)?.copy(productId = doc.id)
            } ?: emptyList()
            trySend(products)
        }
        awaitClose { listener.remove() }
    }

    private fun getInventory(branchId: String): Flow<Map<String, InventoryOverride>> {
        if (branchId.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyMap())
        }
        return callbackFlow {
            val listener = firestore.collection("branches").document(branchId).collection("inventory")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val overrides = snapshot?.documents?.associate { doc ->
                    val override = doc.toObject(InventoryOverride::class.java) ?: InventoryOverride()
                    doc.id to override
                } ?: emptyMap()
                trySend(overrides)
            }
            awaitClose { listener.remove() }
        }
    }

    fun getBranches(): Flow<List<Branch>> = callbackFlow {
        val listener = firestore.collection("branches").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val branches = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Branch::class.java)?.copy(branchId = doc.id)
            } ?: emptyList()
            trySend(branches)
        }
        awaitClose { listener.remove() }
    }

    fun observeBranch(branchId: String): Flow<Branch?> {
        if (branchId.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(null)
        }
        return callbackFlow {
            val listener = firestore.collection("branches").document(branchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val branch = snapshot?.toObject(Branch::class.java)?.copy(branchId = snapshot.id)
                trySend(branch)
            }
            awaitClose { listener.remove() }
        }
    }

    fun observeMenuForBranch(branchId: String): Flow<RestaurantData> {
        return combine(getCategories(), getProducts(), getInventory(branchId)) { categories, products, inventory ->
            val uiProducts = products.map { fp ->
                val override = inventory[fp.productId]
                val isAvail = override?.isAvailable ?: true
                val finalPrice = override?.priceOverride ?: fp.basePrice
                
                val catName = categories.find { it.id == fp.categoryId }?.name ?: "Unknown Category"

                Product(
                    id = fp.productId,
                    category_id = fp.categoryId,
                    category_name = catName,
                    name = fp.name,
                    description = fp.description,
                    price = finalPrice,
                    image = fp.imageUrl,
                    isAvailable = isAvail,
                    requiresCustomization = fp.requiresCustomization,
                    baseCalories = fp.baseCalories,
                    largeCalorieBonus = fp.largeCalorieBonus,
                    ingredientCalorieMap = fp.ingredientCalorieMap
                )
            }
            RestaurantData(defaultBrand, categories, uiProducts)
        }
    }

    fun seedDatabase() {
        val catRef = firestore.collection("categories")
        catRef.document("cat1").set(mapOf("name" to "Burgers"))
        catRef.document("cat2").set(mapOf("name" to "Pizza"))
        catRef.document("cat3").set(mapOf("name" to "Desserts"))

        val prodRef = firestore.collection("products")
        prodRef.document("prod1").set(mapOf(
            "name" to "Classic Beef Burger",
            "description" to "Juicy beef patty with fresh lettuce and tomatoes.",
            "basePrice" to 8.99,
            "categoryId" to "cat1",
            "imageUrl" to ""
        ))
        prodRef.document("p1").set(FirestoreProduct(productId = "p1", name = "Royal Mixed Nuts Box", description = "A premium blend of roasted almonds, cashews, and pistachios.", basePrice = 1200.0, categoryId = "c1", baseCalories = 450, largeCalorieBonus = 200, ingredientCalorieMap = mapOf("extraToppings" to 50)))
        prodRef.document("p2").set(FirestoreProduct(productId = "p2", name = "Pistachio Ice Cream Bowl", description = "Rich vanilla ice cream topped with crushed roasted pistachios.", basePrice = 850.0, categoryId = "c2", requiresCustomization = true, baseCalories = 500, largeCalorieBonus = 250, ingredientCalorieMap = mapOf("extraToppings" to 70)))
        prodRef.document("p3").set(FirestoreProduct(productId = "p3", name = "Chicken Caesar Wrap", description = "Grilled chicken breasts and savory bacon in a wrap.", basePrice = 850.0, categoryId = "c2", baseCalories = 550, largeCalorieBonus = 200, ingredientCalorieMap = mapOf("extraToppings" to 80)))

        val branchRef = firestore.collection("branches")
        // Default branch
        branchRef.document("default_branch").set(mapOf(
            "name" to "Downtown Default",
            "isOpen" to true,
            "location" to com.google.firebase.firestore.GeoPoint(31.5204, 74.3587)
        ))
        
        // Second branch
        branchRef.document("branch2").set(mapOf(
            "name" to "Uptown Branch",
            "isOpen" to true,
            "location" to com.google.firebase.firestore.GeoPoint(31.5404, 74.3787)
        ))

        val invRef = branchRef.document("default_branch").collection("inventory")
        invRef.document("prod1").set(mapOf("isAvailable" to true, "priceOverride" to 9.99))
        invRef.document("prod2").set(mapOf("isAvailable" to true))
        invRef.document("prod3").set(mapOf("isAvailable" to false)) // Sold out
    }

    fun seedPhase5Branches() {
        val branchRef = firestore.collection("branches")
        val branches = listOf(
            mapOf(
                "id" to "branch_johar_town",
                "name" to "ICELAND JoharTown",
                "location" to com.google.firebase.firestore.GeoPoint(31.4697, 74.2728),
                "status" to "OPEN",
                "operatingHours" to "Closes 2 am",
                "address" to "College Rd, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_allah_ho",
                "name" to "Ice Land & Dry Fruits Allah ho",
                "location" to com.google.firebase.firestore.GeoPoint(31.4680, 74.2780),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours",
                "address" to "411, E ali hajveri plaza allah ho chowk",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_pia",
                "name" to "Iceland PIA Main Boulevard",
                "location" to com.google.firebase.firestore.GeoPoint(31.4550, 74.2900),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours (Testing)",
                "address" to "PIA Main Boulevard, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_faisal_town",
                "name" to "ICE LAND (Faisal Town)",
                "location" to com.google.firebase.firestore.GeoPoint(31.4764, 74.3051),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours (Testing)",
                "address" to "17-D, Near Akbar Chowk, Faisal Town",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_gulberg",
                "name" to "Iceland Juice And Dry Fruits Gulberg",
                "location" to com.google.firebase.firestore.GeoPoint(31.5102, 74.3441),
                "status" to "OPEN",
                "operatingHours" to "Closes 2 am",
                "address" to "MM Alam Road, Gulberg III, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_dha",
                "name" to "Iceland DHA Phase 5",
                "location" to com.google.firebase.firestore.GeoPoint(31.4621, 74.4089),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours",
                "address" to "DHA Phase 5, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_model_town",
                "name" to "Iceland Model Town",
                "location" to com.google.firebase.firestore.GeoPoint(31.4851, 74.3261),
                "status" to "OPEN",
                "operatingHours" to "10:00 AM - 1:00 AM",
                "address" to "Model Town, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_zarrar",
                "name" to "Iceland Zarrar Shaheed Road",
                "location" to com.google.firebase.firestore.GeoPoint(31.5284, 74.3986),
                "status" to "OPEN",
                "operatingHours" to "11:00 AM - 12:00 AM",
                "address" to "Zarrar Shaheed Road, Lahore",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_bahria_town",
                "name" to "Iceland Bahria Town",
                "location" to com.google.firebase.firestore.GeoPoint(31.3683, 74.1855),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours (Testing)",
                "address" to "Shop 66–67, Commercial Zone, Sector C, Bahria Town",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_dha_phase_1",
                "name" to "Iceland DHA Phase 1",
                "location" to com.google.firebase.firestore.GeoPoint(31.4700, 74.3980),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours (Testing)",
                "address" to "26 Street 21, DHA Phase 1",
                "isOpen" to true
            ),
            mapOf(
                "id" to "branch_adda_plot",
                "name" to "Iceland Adda Plot",
                "location" to com.google.firebase.firestore.GeoPoint(31.3200, 74.2000),
                "status" to "OPEN",
                "operatingHours" to "Open 24 hours (Testing)",
                "address" to "Adda Plot Roundabout, Raiwind Road, Lahore",
                "isOpen" to true
            )
        )

        for (branch in branches) {
            val id = branch["id"] as String
            branchRef.document(id).set(branch)
        }
    }
}
