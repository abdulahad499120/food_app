package com.example.foodapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayfastTokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("merchant_id") val merchantId: String,
    @SerialName("secured_key") val securedKey: String
)

@Serializable
data class PayfastTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("token") val token: String? = null // sometimes returned instead of access_token
)
