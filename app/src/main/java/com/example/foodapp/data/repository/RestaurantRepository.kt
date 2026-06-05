package com.example.foodapp.data.repository

import android.content.Context
import com.example.foodapp.data.models.RestaurantData
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class RestaurantRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadRestaurantData(): RestaurantData {
        val inputStream = context.assets.open("restaurant_data.json")
        val jsonString = InputStreamReader(inputStream).readText()
        return json.decodeFromString(jsonString)
    }
}
