package com.example.foodapp.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface PayfastApi {
    @POST("api/v1/oauth2/token")
    suspend fun requestToken(@Body request: PayfastTokenRequest): PayfastTokenResponse
}
