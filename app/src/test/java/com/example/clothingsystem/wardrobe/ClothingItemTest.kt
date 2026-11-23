package com.example.clothingsystem.wardrobe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClothingItemTest {
    @Test
    fun jsonRoundTrip() {
        val item = ClothingItem(id = "abc", name = "Blue shirt", category = "Tops", color = "Blue", photoFile = "item_1.jpg")
        assertEquals(item, ClothingItem.deserialise(item.serialise()))
    }

    @Test
    fun emptyPhotoBecomesNull() {
        val item = ClothingItem(name = "Socks", category = "Accessories", color = "Black")
        assertNull(ClothingItem.deserialise(item.serialise())?.photoFile)
    }
}
