package com.example.ui.util

object ValidationUtils {

    /**
     * Extracts and normalizes Pakistan mobile digits after +92.
     * Handles inputs like "03001234567", "+923001234567", "923001234567", "3001234567".
     * Returns digits only (up to 10 digits), starting with '3'.
     */
    fun sanitizePkPhoneDigits(input: String): String {
        var clean = input.trim()
        if (clean.startsWith("+92")) {
            clean = clean.substring(3)
        } else if (clean.startsWith("92")) {
            clean = clean.substring(2)
        } else if (clean.startsWith("03")) {
            clean = clean.substring(1)
        }
        clean = clean.filter { it.isDigit() }
        if (clean.length > 10) {
            clean = clean.substring(0, 10)
        }
        return clean
    }

    /**
     * Converts raw phone input or extracted digits into canonical string "+923XXXXXXXXX".
     */
    fun toCanonicalPkPhone(input: String): String {
        val digits = sanitizePkPhoneDigits(input)
        return if (digits.isNotBlank()) "+92$digits" else ""
    }

    /**
     * Validates Pakistan mobile number format.
     * Requirements:
     * - Only numeric mobile digits after +92
     * - Exactly 10 digits after +92
     * - Starts with '3'
     * - Rejects numbers starting with '0' after +92
     */
    fun validatePkPhone(input: String, isRequired: Boolean = false): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return if (isRequired) Pair(false, "Phone number is required.") else Pair(true, null)
        }
        val digits = sanitizePkPhoneDigits(trimmed)
        if (digits.isEmpty()) {
            return Pair(false, "Please enter a valid Pakistan mobile number.")
        }
        if (!digits.startsWith("3")) {
            return Pair(false, "Pakistan mobile number must start with '3' (e.g. 300 1234567).")
        }
        if (digits.length < 10) {
            return Pair(false, "Incomplete mobile number (${digits.length}/10 digits). Format: +92 3XX XXXXXXX")
        }
        if (digits.length == 10 && digits.startsWith("3")) {
            return Pair(true, null)
        }
        return Pair(false, "Invalid Pakistan mobile number.")
    }

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Validates real-time email syntax.
     */
    fun validateEmail(email: String, isRequired: Boolean = false): Pair<Boolean, String?> {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return if (isRequired) Pair(false, "Email address is required.") else Pair(true, null)
        }
        if (trimmed.contains(" ")) {
            return Pair(false, "Email address cannot contain spaces.")
        }
        if (!trimmed.contains("@")) {
            return Pair(false, "Email must contain '@' symbol (e.g. name@company.com).")
        }
        val parts = trimmed.split("@")
        if (parts.size != 2 || parts[0].isBlank()) {
            return Pair(false, "Invalid username before '@'.")
        }
        val domain = parts[1]
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            return Pair(false, "Please enter a valid domain (e.g. company.com).")
        }
        if (!emailRegex.matches(trimmed)) {
            return Pair(false, "Please enter a valid email address.")
        }
        return Pair(true, null)
    }

    /**
     * Validates required non-empty string.
     */
    fun validateRequired(text: String, fieldName: String): Pair<Boolean, String?> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "$fieldName is required.")
        }
        return Pair(true, null)
    }

    /**
     * Validates positive double number.
     */
    fun validatePositiveAmount(text: String, fieldName: String, allowZero: Boolean = false): Pair<Boolean, String?> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "$fieldName is required.")
        }
        val number = trimmed.toDoubleOrNull()
        if (number == null) {
            return Pair(false, "Enter a valid number for $fieldName.")
        }
        if (allowZero) {
            if (number < 0) return Pair(false, "$fieldName cannot be negative.")
        } else {
            if (number <= 0) return Pair(false, "$fieldName must be greater than 0.")
        }
        return Pair(true, null)
    }

    /**
     * Validates integer quantity.
     */
    fun validateQuantity(text: String, fieldName: String, allowZero: Boolean = false): Pair<Boolean, String?> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "$fieldName is required.")
        }
        val number = trimmed.toIntOrNull()
        if (number == null) {
            return Pair(false, "Enter a valid whole number for $fieldName.")
        }
        if (allowZero) {
            if (number < 0) return Pair(false, "$fieldName cannot be negative.")
        } else {
            if (number <= 0) return Pair(false, "$fieldName must be greater than 0.")
        }
        return Pair(true, null)
    }
}
