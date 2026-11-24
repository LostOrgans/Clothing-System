package com.example.clothingsystem.outfits

import com.example.clothingsystem.wardrobe.ClothingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutfitTest {
    private fun item(id: String, name: String, category: String, color: String) =
        ClothingItem(id = id, name = name, category = category, color = color)

    @Test
    fun suggestNeedsTopsAndBottoms() {
        assertTrue(OutfitEngine.suggest(listOf(item("1", "Shirt", "Tops", "Blue"))).isEmpty())
    }

    @Test
    fun suggestRanksColourPairs() {
        val items = listOf(
            item("t1", "Blue top", "Tops", "Blue"),
            item("t2", "Pink top", "Tops", "Pink"),
            item("b1", "Grey pants", "Bottoms", "Grey"),
            item("b2", "Red pants", "Bottoms", "Red"),
        )
        val suggestions = OutfitEngine.suggest(items)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("Blue top + Grey pants", suggestions.first().name)
        assertTrue(suggestions.first().score >= suggestions.last().score)
    }

    @Test
    fun suggestionIncludesLayersWhenAvailable() {
        val items = listOf(
            item("t", "Top", "Tops", "Blue"),
            item("b", "Bottom", "Bottoms", "Grey"),
            item("o", "Jacket", "Outerwear", "Black"),
            item("s", "Shoes", "Shoes", "White"),
        )
        val suggestion = OutfitEngine.suggest(items).first()
        assertEquals(listOf("t", "b", "o", "s"), suggestion.itemIds)
    }

    @Test
    fun outfitSerialisesRoundTrip() {
        val outfit = Outfit(name = "Sunday", itemIds = listOf("a", "b"))
        assertEquals(outfit, Outfit.deserialise(outfit.serialise()))
    }

    @Test
    fun malformedOutfitIsRejected() {
        assertEquals(null, Outfit.deserialise("broken"))
    }
}
