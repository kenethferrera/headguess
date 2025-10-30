package com.example.headguess.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object CountryManager {
    var selectedCountry by mutableStateOf("General")
        private set
    
    fun setCountry(country: String) {
        selectedCountry = country
    }
    
    fun getCountryFileName(): String {
        return when (selectedCountry) {
            "General" -> "categories"
            "Custom" -> "categories" // For now, use general for custom
            else -> "categories_${selectedCountry.lowercase().replace(" ", "")}"
        }
    }
    
    fun getImpostorFileName(): String {
        return when (selectedCountry) {
            "General" -> "impostor"
            "Custom" -> "impostor" // For now, use general for custom
            else -> "impostor_${selectedCountry.lowercase().replace(" ", "")}"
        }
    }
}











