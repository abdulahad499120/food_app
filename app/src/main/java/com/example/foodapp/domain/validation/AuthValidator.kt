package com.example.foodapp.domain.validation

object AuthValidator {

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isStrongPassword(password: String): Boolean {
        // Minimum 8 characters, at least one uppercase letter, one lowercase letter, and one number
        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$".toRegex()
        return passwordRegex.matches(password)
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        // Very basic validation: should start with '+' and have 10-15 digits after
        val phoneRegex = "^\\+[1-9]\\d{9,14}$".toRegex()
        return phoneRegex.matches(phone)
    }
}
