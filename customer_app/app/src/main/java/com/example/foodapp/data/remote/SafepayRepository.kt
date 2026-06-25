package com.example.foodapp.data.remote

import com.ahad.foodapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafepayRepository {
    private val api = SafepayNetworkClient.api
    private val publicKey = BuildConfig.SAFEPAY_PUBLIC_KEY

    suspend fun createCustomer(email: String, firstName: String, lastName: String, phone: String, country: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val req = SafepayCustomerRequest(firstName, lastName, email, phone, country)
                val response = api.createCustomer(req)
                response.data?.token
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getCustomerVault(customerId: String): SafepayCustomerDetailsResponse.CustomerDetailsData? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCustomer(customerId)
                response.data
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun initializeTracker(customerId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val req = SafepayTrackerRequest(
                    user = customerId,
                    merchantApiKey = publicKey,
                    entryMode = "raw"
                )
                val response = api.initializeTracker(req)
                response.data?.tracker?.token
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun submitAuthSetup(tracker: String, cardNumber: String, expMonth: String, expYear: String, cvv: String): SafepayAuthSetupResponse.PayerAuthSetup? {
        return withContext(Dispatchers.IO) {
            try {
                val req = SafepayAuthSetupRequest(
                    payload = SafepayAuthSetupRequest.AuthPayload(
                        paymentMethod = SafepayAuthSetupRequest.PaymentMethodInfo(
                            card = SafepayAuthSetupRequest.CardInfo(
                                cardNumber = cardNumber,
                                expirationMonth = expMonth,
                                expirationYear = expYear,
                                cvv = cvv
                            )
                        )
                    )
                )
                val response = api.submitAuthSetup(tracker, req)
                response.data?.action?.payerAuthSetup
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun submitEnrollment(tracker: String, sessionId: String, street: String, city: String, postalCode: String, country: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val req = SafepayEnrollmentRequest(
                    payload = SafepayEnrollmentRequest.EnrollmentPayload(
                        billing = SafepayEnrollmentRequest.BillingInfo(
                            street1 = street,
                            city = city,
                            postalCode = postalCode,
                            country = country
                        ),
                        authSetup = SafepayEnrollmentRequest.AuthSetupConfig(
                            successUrl = "https://www.getsafepay.com/success",
                            failureUrl = "https://www.getsafepay.com/failure",
                            deviceFingerprintSessionId = sessionId
                        )
                    )
                )
                val response = api.submitEnrollment(tracker, req)
                // If it returns next_actions for Challenge, wait, it might return NOOP or it might fail
                // For a vault/instrument, it might just succeed.
                response.status.message == "success"
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun submitAuthorization(tracker: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val req = SafepayAuthorizationRequest(
                    payload = SafepayAuthorizationRequest.AuthFinalPayload()
                )
                val response = api.submitAuthorization(tracker, req)
                response.status.message == "success"
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    data class VaultedCardDdcInfo(val tracker: String, val accessToken: String, val ddcUrl: String)

    suspend fun startVaultedCardPayment(
        customerId: String,
        instrumentToken: String,
        amount: Double
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize Tracker with mode="unscheduled_cof" to bypass 3DS for Vaulted Cards
                val trackerReq = SafepayTrackerRequest(
                    user = customerId,
                    merchantApiKey = publicKey,
                    mode = "unscheduled_cof",
                    entryMode = null,
                    isAccountVerification = false,
                    amount = (amount * 100).toLong(),
                    source = "instrument",
                    instrument = instrumentToken
                )
                val trackerResp = api.initializeTracker(trackerReq)
                val trackerId = trackerResp.data?.tracker?.token
                    ?: return@withContext Result.failure(Exception("Failed to initialize tracker: ${trackerResp.status.errors.firstOrNull() ?: trackerResp.status.message}"))

                // Directly authorize since it's an unscheduled COF (merchant-initiated)
                val authReq = SafepayAuthorizationRequest(
                    payload = SafepayAuthorizationRequest.AuthFinalPayload()
                )
                val authResp = api.submitAuthorization(trackerId, authReq)

                if (authResp.status.message == "success") {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Authorization failed: ${authResp.status.message}"))
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (ignored: Exception) { null }
                e.printStackTrace()
                Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun completeVaultedCardPayment(tracker: String, sessionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val enrollReq = SafepayEnrollmentRequest(
                    payload = SafepayEnrollmentRequest.EnrollmentPayload(
                        billing = SafepayEnrollmentRequest.BillingInfo(
                            street1 = "123 Test St",
                            city = "Lahore",
                            postalCode = "54000",
                            country = "PK"
                        ),
                        authSetup = SafepayEnrollmentRequest.AuthSetupConfig(
                            successUrl = "https://www.getsafepay.com/success",
                            failureUrl = "https://www.getsafepay.com/failure",
                            deviceFingerprintSessionId = sessionId
                        )
                    )
                )
                api.submitEnrollment(tracker, enrollReq)

                val authReq = SafepayAuthorizationRequest(
                    payload = SafepayAuthorizationRequest.AuthFinalPayload()
                )
                val authResp = api.submitAuthorization(tracker, authReq)

                if (authResp.status.message == "success") {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Authorization failed: ${authResp.status.message}"))
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (ignored: Exception) { null }
                e.printStackTrace()
                Result.failure(Exception("HTTP ${e.code()}: $errorBody"))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
