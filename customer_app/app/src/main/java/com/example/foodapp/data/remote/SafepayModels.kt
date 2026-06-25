package com.example.foodapp.data.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SafepayCustomerRequest(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("email") val email: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("country") val country: String
)

@Serializable
data class SafepayCustomerResponse(
    @SerialName("data") val data: CustomerData?,
    @SerialName("status") val status: SafepayStatus
) {
    @Serializable
    data class CustomerData(
        @SerialName("token") val token: String
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SafepayTrackerRequest(
    @SerialName("user") val user: String,
    @SerialName("merchant_api_key") val merchantApiKey: String,
    @SerialName("intent") val intent: String = "CYBERSOURCE",
    @SerialName("mode") val mode: String = "instrument",
    @SerialName("currency") val currency: String = "PKR",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("entry_mode") val entryMode: String? = null,
    @SerialName("is_account_verification") val isAccountVerification: Boolean = true,
    @SerialName("amount") val amount: Long? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("instrument") val instrument: String? = null
)

@Serializable
data class SafepayTrackerResponse(
    @SerialName("data") val data: TrackerDataWrapper?,
    @SerialName("status") val status: SafepayStatus
) {
    @Serializable
    data class TrackerDataWrapper(
        @SerialName("tracker") val tracker: TrackerInfo?
    )
    
    @Serializable
    data class TrackerInfo(
        @SerialName("token") val token: String
    )
}

@Serializable
data class SafepayAuthSetupRequest(
    @SerialName("payload") val payload: AuthPayload
) {
    @Serializable
    data class AuthPayload(
        @SerialName("payment_method") val paymentMethod: PaymentMethodInfo
    )
    
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class PaymentMethodInfo(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("card") val card: CardInfo? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @SerialName("instrument") val instrument: String? = null
    )
    
    @Serializable
    data class CardInfo(
        @SerialName("card_number") val cardNumber: String,
        @SerialName("expiration_month") val expirationMonth: String,
        @SerialName("expiration_year") val expirationYear: String,
        @SerialName("cvv") val cvv: String
    )
}

@Serializable
data class SafepayAuthSetupResponse(
    @SerialName("data") val data: AuthSetupData?,
    @SerialName("status") val status: SafepayStatus
) {
    @Serializable
    data class AuthSetupData(
        @SerialName("action") val action: ActionInfo?
    )
    
    @Serializable
    data class ActionInfo(
        @SerialName("payer_authentication_setup") val payerAuthSetup: PayerAuthSetup?
    )
    
    @Serializable
    data class PayerAuthSetup(
        @SerialName("access_token") val accessToken: String,
        @SerialName("device_data_collection_url") val deviceDataCollectionUrl: String
    )
}

@Serializable
data class SafepayEnrollmentRequest(
    @SerialName("payload") val payload: EnrollmentPayload
) {
    @Serializable
    data class EnrollmentPayload(
        @SerialName("billing") val billing: BillingInfo,
        @SerialName("authorization") val authorization: AuthConfig = AuthConfig(false),
        @SerialName("authentication_setup") val authSetup: AuthSetupConfig
    )
    
    @Serializable
    data class BillingInfo(
        @SerialName("street_1") val street1: String,
        @SerialName("street_2") val street2: String = "",
        @SerialName("city") val city: String,
        @SerialName("state") val state: String = "",
        @SerialName("postal_code") val postalCode: String,
        @SerialName("country") val country: String
    )
    
    @Serializable
    data class AuthConfig(
        @SerialName("do_capture") val doCapture: Boolean
    )
    
    @Serializable
    data class AuthSetupConfig(
        @SerialName("success_url") val successUrl: String,
        @SerialName("failure_url") val failureUrl: String,
        @SerialName("device_fingerprint_session_id") val deviceFingerprintSessionId: String
    )
}

@Serializable
data class SafepayEnrollmentResponse(
    @SerialName("data") val data: EnrollmentData?,
    @SerialName("status") val status: SafepayStatus
) {
    @Serializable
    data class EnrollmentData(
        @SerialName("tracker") val tracker: TrackerNextActions?
    )
    
    @Serializable
    data class TrackerNextActions(
        @SerialName("next_actions") val nextActions: NextActions?
    )
    
    @Serializable
    data class NextActions(
        @SerialName("CYBERSOURCE") val cybersource: CybersourceAction?
    )
    
    @Serializable
    data class CybersourceAction(
        @SerialName("kind") val kind: String
    )
}

@Serializable
data class SafepayAuthorizationRequest(
    @SerialName("action") val action: String = "AUTHORIZATION",
    @SerialName("payload") val payload: AuthFinalPayload
) {
    @Serializable
    data class AuthFinalPayload(
        @SerialName("authorization") val authorization: SafepayEnrollmentRequest.AuthConfig = SafepayEnrollmentRequest.AuthConfig(false)
    )
}

@Serializable
data class SafepayAuthorizationResponse(
    @SerialName("status") val status: SafepayStatus
)

@Serializable
data class SafepayCustomerDetailsResponse(
    @SerialName("data") val data: CustomerDetailsData?,
    @SerialName("status") val status: SafepayStatus
) {
    @Serializable
    data class CustomerDetailsData(
        @SerialName("token") val token: String,
        @SerialName("wallet") val wallet: List<WalletItem> = emptyList()
    )

    @Serializable
    data class WalletItem(
        @SerialName("token") val token: String,
        @SerialName("kind") val kind: Int,
        @SerialName("cybersource") val cybersource: CybersourceCard? = null
    )

    @Serializable
    data class CybersourceCard(
        @SerialName("token") val token: String,
        @SerialName("bin") val bin: String,
        @SerialName("last_four") val lastFour: String,
        @SerialName("expiry_month") val expiryMonth: String,
        @SerialName("expiry_year") val expiryYear: String
    )
}

@Serializable
data class SafepayStatus(
    @SerialName("message") val message: String,
    @SerialName("errors") val errors: List<String> = emptyList()
)
