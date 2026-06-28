package com.example.foodapp.data.remote

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PayfastRepository(private val api: PayfastApi) {

    // Dummy credentials for now - to be replaced with actual ones
    private val merchantId = "YOUR_MERCHANT_ID"
    private val securedKey = "YOUR_SECURE_KEY"
    
    // We will use the sandbox URL until production
    private val checkoutBaseUrl = "https://ipg.apps.payfast.com.pk/payment/"

    suspend fun generateCheckoutUrl(amount: Double, orderId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Request authentication token
                val req = PayfastTokenRequest(
                    merchantId = merchantId,
                    securedKey = securedKey
                )
                
                val response = api.requestToken(req)
                val token = response.token ?: response.accessToken
                
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Failed to generate PayFast token"))
                }

                // 2. Build the Hosted Checkout URL with the required parameters
                val url = Uri.parse(checkoutBaseUrl).buildUpon()
                    .appendQueryParameter("merchant_id", merchantId)
                    .appendQueryParameter("token", token)
                    .appendQueryParameter("txnamt", amount.toString())
                    .appendQueryParameter("order_id", orderId)
                    .appendQueryParameter("customer_email_address", "customer@example.com")
                    .appendQueryParameter("customer_mobile_no", "03001234567")
                    .appendQueryParameter("store_name", "FoodApp")
                    .appendQueryParameter("return_url", "https://foodapp.local/success")
                    .build()
                    .toString()
                    
                Result.success(url)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
