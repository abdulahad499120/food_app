package com.example.foodapp.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SafepayApi {
    @POST("user/customers/v1/")
    suspend fun createCustomer(@Body request: SafepayCustomerRequest): SafepayCustomerResponse

    @GET("user/customers/v1/{customerId}")
    suspend fun getCustomer(@Path("customerId") customerId: String): SafepayCustomerDetailsResponse

    @POST("order/payments/v3/")
    suspend fun initializeTracker(@Body request: SafepayTrackerRequest): SafepayTrackerResponse

    @POST("order/payments/v3/{tracker}")
    suspend fun submitAuthSetup(
        @Path("tracker") tracker: String,
        @Body request: SafepayAuthSetupRequest
    ): SafepayAuthSetupResponse

    @POST("order/payments/v3/{tracker}")
    suspend fun submitEnrollment(
        @Path("tracker") tracker: String,
        @Body request: SafepayEnrollmentRequest
    ): SafepayEnrollmentResponse

    @POST("order/payments/v3/{tracker}")
    suspend fun submitAuthorization(
        @Path("tracker") tracker: String,
        @Body request: SafepayAuthorizationRequest
    ): SafepayAuthorizationResponse
}
