package com.example.data.model

import android.graphics.Color
import android.util.Log

data class CompanyConfig(
    val name: String = "Mi Empresa S.A.",
    val logoUri: String? = null,
    val primaryColorHex: String = "#1A1A2E", // Oscuro elegante
    val secondaryColorHex: String = "#16213E", // Bloques de información
    val ctaColorHex: String = "#E94560", // Llamado a la acción (Botones)
    val textColorHex: String = "#FFFFFF" // Tipografía general
) {
    fun getPrimaryColor(): Int = safeParse(primaryColorHex, "#1A1A2E")
    fun getSecondaryColor(): Int = safeParse(secondaryColorHex, "#16213E")
    fun getCtaColor(): Int = safeParse(ctaColorHex, "#E94560")
    fun getTextColor(): Int = safeParse(textColorHex, "#FFFFFF")

    private fun safeParse(hex: String, defaultHex: String): Int {
        val trimmed = hex.trim()
        if (trimmed.isEmpty()) return Color.parseColor(defaultHex)
        val formatted = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return try {
            Color.parseColor(formatted)
        } catch (e: Exception) {
            Log.e("CompanyConfig", "Error parsing color '$hex', using default '$defaultHex'", e)
            try {
                Color.parseColor(defaultHex)
            } catch (ex: Exception) {
                Color.BLACK
            }
        }
    }
}

