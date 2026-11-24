package com.example.clothingsystem.outfits

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class Outfit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val itemIds: List<String>,
) {
    fun serialise(): String = listOf(id, name, itemIds.joinToString(","))
        .joinToString("|") { URLEncoder.encode(it, "UTF-8") }

    companion object {
        fun deserialise(value: String): Outfit? {
            val parts = value.split("|").map { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            if (parts.size != 3 || parts.any { it == null }) return null
            return Outfit(
                id = parts[0]!!,
                name = parts[1]!!,
                itemIds = parts[2]!!.split(',').filter { it.isNotBlank() },
            )
        }
    }
}

data class OutfitSuggestion(
    val name: String,
    val itemIds: List<String>,
    val reason: String,
    val score: Int,
)
