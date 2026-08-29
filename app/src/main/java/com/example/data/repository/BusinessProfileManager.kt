package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BusinessProfile(
    val companyName: String = "",
    val taxId: String = "",
    val currencySymbol: String = "Rs",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val logoUrl: String = "",
    val isSaved: Boolean = false
)

class BusinessProfileManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("business_profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<BusinessProfile> = _profile.asStateFlow()

    private fun loadProfile(): BusinessProfile {
        val isSaved = prefs.getBoolean(KEY_IS_SAVED, false)
        val loadedCurrency = prefs.getString(KEY_CURRENCY_SYMBOL, "Rs") ?: "Rs"
        return BusinessProfile(
            companyName = prefs.getString(KEY_COMPANY_NAME, "") ?: "",
            taxId = prefs.getString(KEY_TAX_ID, "") ?: "",
            currencySymbol = if (loadedCurrency.isBlank() || loadedCurrency == "$") "Rs" else loadedCurrency,
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            address = prefs.getString(KEY_ADDRESS, "") ?: "",
            website = prefs.getString(KEY_WEBSITE, "") ?: "",
            logoUrl = prefs.getString(KEY_LOGO_URL, "") ?: "",
            isSaved = isSaved
        )
    }

    fun saveProfile(profile: BusinessProfile) {
        val toSave = profile.copy(
            currencySymbol = profile.currencySymbol.ifBlank { "Rs" },
            isSaved = true
        )
        prefs.edit()
            .putBoolean(KEY_IS_SAVED, true)
            .putString(KEY_COMPANY_NAME, toSave.companyName)
            .putString(KEY_TAX_ID, toSave.taxId)
            .putString(KEY_CURRENCY_SYMBOL, toSave.currencySymbol)
            .putString(KEY_PHONE, toSave.phone)
            .putString(KEY_EMAIL, toSave.email)
            .putString(KEY_ADDRESS, toSave.address)
            .putString(KEY_WEBSITE, toSave.website)
            .putString(KEY_LOGO_URL, toSave.logoUrl)
            .apply()
        _profile.value = toSave
    }

    fun deleteProfile() {
        prefs.edit().clear().apply()
        _profile.value = BusinessProfile(
            companyName = "",
            taxId = "",
            currencySymbol = "Rs",
            phone = "",
            email = "",
            address = "",
            website = "",
            logoUrl = "",
            isSaved = false
        )
    }

    companion object {
        private const val KEY_IS_SAVED = "is_profile_saved"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_TAX_ID = "tax_id"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_ADDRESS = "address"
        private const val KEY_WEBSITE = "website"
        private const val KEY_LOGO_URL = "logo_url"

        @Volatile
        private var INSTANCE: BusinessProfileManager? = null

        fun getInstance(context: Context): BusinessProfileManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BusinessProfileManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
