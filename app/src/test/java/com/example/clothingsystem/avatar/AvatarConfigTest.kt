package com.example.clothingsystem.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AvatarConfigTest {
    @Test
    fun weightsMatchShapeKeyOrder() {
        assertEquals(floatArrayOf(0.2f, 0.7f, 1f), AvatarConfig(0.2f, 0.7f, 1f).morphWeights())
    }

    @Test
    fun rejectsOutOfRangeWeight() {
        assertFailsWith<IllegalArgumentException> { AvatarConfig(slim = 1.2f) }
    }
}
