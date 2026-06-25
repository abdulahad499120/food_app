# What We Know About Safepay Implementation

This document summarizes the technical findings and constraints we discovered while integrating Safepay for both standard hosted checkout and Vaulted (saved) Cards.

## 1. Tracker Initialization (`/order/v1/init`)

The Safepay Tracker is the foundation of every payment session. When creating a tracker, several parameters dictate how the session will behave:

- **`mode`**: 
  - `"payment"`: Standard checkout for an immediate charge.
  - `"instrument"`: Used to tokenize (vault) a card. Usually requires `entry_mode="raw"`.
  - `"unscheduled_cof"`: Used for Merchant-Initiated Transactions (MIT), i.e., charging a previously vaulted card.
- **`entry_mode`**: 
  - Represents the method of collecting the customer's payment details.
  - Safepay's default for instrumenting cards is often `"raw"`.
  - **CRITICAL:** When using `mode="unscheduled_cof"` and `intent="CYBERSOURCE"`, you **cannot** use `entry_mode="raw"`. If you send `"raw"`, Safepay will throw a 500 Error: `raw is not a valid entry mode against intent CYBERSOURCE and mode unscheduled_cof`. It must be omitted (`null`) or set to an acceptable alternative.
- **`source` & `instrument`**: 
  - To use a saved card, you must pass `source="instrument"` and `instrument="<token_id>"` when initializing the tracker.

## 2. Vaulted Card Flow (Unscheduled COF)

Charging a vaulted card is fundamentally different from a standard 3D Secure (3DS) flow. Because the card is already saved and the transaction is merchant-initiated, **you completely bypass Payer Authentication (3DS).**

### Flow Steps:
1. **Initialize Tracker**: Call `/order/v1/init` with `mode="unscheduled_cof"`, `source="instrument"`, `instrument="<token>"`, and omit `entry_mode`.
2. **Authorize Payment**: Call `/order/payments/v3/{tracker}` with `action="AUTHORIZATION"` to process the charge directly against the initialized tracker.
3. **No Setup/Enrollment Required**: You do *not* call `PAYER_AUTH_SETUP` or `ENROLLMENT` actions. Attempting to use `PAYER_AUTH_SETUP` with an instrument token will result in Safepay throwing a `missing payment method payload` error because `PAYER_AUTH_SETUP` is strictly designed for full card details (to trigger 3DS), not tokens.

## 3. Standard Checkout & Vaulting Flow

If the user is paying with a new card and wants to save it:
1. **Initialize Tracker**: `mode="instrument"`, `entry_mode="raw"`.
2. **Auth Setup**: Call `/order/payments/v3/{tracker}` with `action="PAYER_AUTH_SETUP"`. Pass the full card details (`card_number`, `expiration_month`, `expiration_year`, `cvv`) inside the `payment_method.card` object.
3. **Device Data Collection (DDC)**: The `PAYER_AUTH_SETUP` response returns a `deviceDataCollectionUrl` and `accessToken`. The app must render this URL in a hidden WebView to profile the device.
4. **Enrollment**: Once the WebView completes successfully, call the API again with `action="ENROLLMENT"` passing the device fingerprint session ID and billing address. This step checks if the user must be challenged (OTP/3DS).
5. **Authorization**: Finally, call the API with `action="AUTHORIZATION"`.

## 4. API Quirks and Common Errors

- **`could not prepare payload for action 'PAYER_AUTH_SETUP': missing payment method payload`**: 
  - This occurs if you attempt to send an instrument token to `PAYER_AUTH_SETUP`, or if you omit the `action` string in an `AUTHORIZATION` request causing Safepay to default to `PAYER_AUTH_SETUP` with an empty payload.
- **`raw is not a valid entry mode against intent CYBERSOURCE and mode unscheduled_cof`**: 
  - Occurs if your backend defaults the tracker's `entry_mode` to `"raw"` when attempting to charge a saved card.
- **`missing card information details`**:
  - Occurs if you try to pass `{ "token": "..." }` inside a `PAYER_AUTH_SETUP` request instead of a full `card` object.

## Conclusion
To summarize:
- **New Cards (3DS)** = `mode: "payment" / "instrument"` -> `PAYER_AUTH_SETUP` -> `DDC` -> `ENROLLMENT` -> `AUTHORIZATION`.
- **Vaulted Cards** = `mode: "unscheduled_cof"` -> `AUTHORIZATION`.
