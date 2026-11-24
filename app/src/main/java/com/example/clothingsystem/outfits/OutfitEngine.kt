package com.example.clothingsystem.outfits

import com.example.clothingsystem.wardrobe.ClothingItem

object OutfitEngine {
    private val neutrals = setOf("Black", "White", "Grey", "Beige")
    private val pairs = mapOf(
        "Blue" to setOf("White", "Grey", "Beige", "Black"),
        "Red" to setOf("White", "Black", "Grey", "Beige"),
        "Green" to setOf("White", "Beige", "Grey", "Black"),
        "Pink" to setOf("White", "Grey", "Beige", "Black"),
        "Black" to setOf("White", "Grey", "Beige", "Red", "Blue", "Green", "Pink"),
        "White" to setOf("Blue", "Red", "Green", "Black", "Grey", "Pink", "Beige"),
        "Grey" to setOf("Blue", "Red", "Green", "Pink", "Black", "White", "Beige"),
        "Beige" to setOf("Blue", "Green", "White", "Black", "Grey", "Red"),
    )

    fun score(a: String, b: String): Int = when {
        a == b -> 0
        a in neutrals && b in neutrals -> 3
        pairs[a]?.contains(b) == true || pairs[b]?.contains(a) == true -> 2
        else -> 1
    }

    fun suggest(items: List<ClothingItem>, limit: Int = 4): List<OutfitSuggestion> {
        val byCategory = items.groupBy { it.category }
        val tops = byCategory["Tops"].orEmpty()
        val bottoms = byCategory["Bottoms"].orEmpty()
        if (tops.isEmpty() || bottoms.isEmpty()) return emptyList()

        val outer = byCategory["Outerwear"].orEmpty()
        val shoes = byCategory["Shoes"].orEmpty()
        val results = mutableListOf<OutfitSuggestion>()
        for (top in tops) for (bottom in bottoms) {
            val base = score(top.color, bottom.color)
            val bestOuter = outer.maxByOrNull { score(top.color, it.color) + score(bottom.color, it.color) }
            val bestShoes = shoes.maxByOrNull { score(bottom.color, it.color) }
            val ids = listOfNotNull(top.id, bottom.id, bestOuter?.id, bestShoes?.id)
            val total = base +
                (bestOuter?.let { score(top.color, it.color) + score(bottom.color, it.color) } ?: 0) +
                (bestShoes?.let { score(bottom.color, it.color) } ?: 0)
            val reasons = mutableListOf<String>()
            if (base >= 2) reasons += "colours work together"
            if (bestOuter != null) reasons += "adds a layer"
            if (bestShoes != null) reasons += "matching shoes"
            results += OutfitSuggestion(
                name = listOfNotNull(top.name, bottom.name, bestOuter?.name, bestShoes?.name).joinToString(" + "),
                itemIds = ids,
                reason = reasons.joinToString(" · ").ifEmpty { "a simple combination" },
                score = total,
            )
        }
        return results.sortedByDescending { it.score }.take(limit)
    }
}
