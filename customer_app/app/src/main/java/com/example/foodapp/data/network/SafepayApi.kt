package com.example.foodapp.data.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.Body
import retrofit2.http.POST

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TrackerRequest(
    @SerialName("client") val client: String,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    @SerialName("environment") val environment: String = "sandbox",
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    @SerialName("amount") val amount: Double,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
    @SerialName("currency") val currency: String = "PKR"
)

@Serializable
data class TrackerData(
    @SerialName("token") val token: String
)

@Serializable
data class TrackerResponse(
    @SerialName("data") val data: TrackerData
)

@Serializable
data class SafepayErrorResponse(
    @SerialName("error") val error: String? = null,
    @SerialName("message") val message: String? = null
)

interface SafepayApi {

    /**
     * Initialize a tracker token for Hosted Checkout.
     */
    @POST("order/v1/init")
    suspend fun initTracker(
        @Body request: TrackerRequest
    ): TrackerResponse
}
