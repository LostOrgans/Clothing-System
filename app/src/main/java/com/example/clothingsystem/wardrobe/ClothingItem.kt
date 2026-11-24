package com.example.clothingsystem.wardrobe

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class ClothingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val color: String,
    val photoFile: String? = null,
    val design: String? = null,
) {
    fun serialise(): String = listOf(id, name, category, color, photoFile ?: "", design ?: "")
        .joinToString("|") { URLEncoder.encode(it, "UTF-8") }

    companion object {
        fun deserialise(value: String): ClothingItem? {
            val parts = value.split("|").map { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            if (parts.size < 5 || parts.any { it == null }) return null
            return ClothingItem(
                id = parts[0]!!, name = parts[1]!!, category = parts[2]!!,
                color = parts[3]!!, photoFile = parts[4]!!.ifEmpty { null },
                design = parts.getOrNull(5)?.ifEmpty { null },
            )
        }
    }
}

val CATEGORIES = listOf("Tops", "Bottoms", "Outerwear", "Shoes", "Accessories")

fun colorHex(label: String): Long = COLORS.firstOrNull { it.first == label }?.second ?: 0xFF9E9E9E

val COLORS = listOf(
    "Black" to 0xFF1C1C1E,
    "White" to 0xFFF5F5F5,
    "Grey" to 0xFF9E9E9E,
    "Red" to 0xFFE53935,
    "Blue" to 0xFF1E88E5,
    "Green" to 0xFF43A047,
    "Beige" to 0xFFD7C0A0,
    "Pink" to 0xFFF06292,
)
