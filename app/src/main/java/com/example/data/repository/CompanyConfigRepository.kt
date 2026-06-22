package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CompanyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompanyConfigRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("company_branding_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<CompanyConfig> = _config.asStateFlow()

    private fun loadConfig(): CompanyConfig {
        val name = prefs.getString("company_name", "Mi Empresa Creativa") ?: "Mi Empresa Creativa"
        val logoUri = prefs.getString("logo_uri", null)
        val primary = prefs.getString("primary_color", "#1A1A2E") ?: "#1A1A2E"
        val secondary = prefs.getString("secondary_color", "#16213E") ?: "#16213E"
        val cta = prefs.getString("cta_color", "#E94560") ?: "#E94560"
        val text = prefs.getString("text_color", "#FFFFFF") ?: "#FFFFFF"
        return CompanyConfig(name, logoUri, primary, secondary, cta, text)
    }

    fun saveConfig(newConfig: CompanyConfig) {
        prefs.edit().apply {
            putString("company_name", newConfig.name)
            putString("logo_uri", newConfig.logoUri)
            putString("primary_color", newConfig.primaryColorHex)
            putString("secondary_color", newConfig.secondaryColorHex)
            putString("cta_color", newConfig.ctaColorHex)
            putString("text_color", newConfig.textColorHex)
            apply()
        }
        _config.value = newConfig
    }
}
