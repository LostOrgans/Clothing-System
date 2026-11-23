package com.example.clothingsystem.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MorphMappingTest {
    @Test
    fun pairsMinAndMaxIntoOneControl() {
        val controls = MorphMapping.controls(listOf("height_min", "height_max", "age_min", "age_max", "face_a"))
        assertEquals(listOf("height", "age", "face_a"), controls.map { it.key })
        assertTrue(controls[0].paired)
        assertTrue(!controls[2].paired)
    }

    @Test
    fun pairedSliderNeutralGivesZeroWeights() {
        val control = MorphMapping.controls(listOf("height_min", "height_max")).first()
        assertEquals(mapOf("height_min" to 0f, "height_max" to 0f), MorphMapping.weights(control, 0.5f))
    }

    @Test
    fun pairedSliderExtremes() {
        val control = MorphMapping.controls(listOf("height_min", "height_max")).first()
        assertEquals(mapOf("height_min" to 1f, "height_max" to 0f), MorphMapping.weights(control, 0f))
        assertEquals(mapOf("height_min" to 0f, "height_max" to 1f), MorphMapping.weights(control, 1f))
    }

    @Test
    fun readsValueBackFromWeights() {
        val control = MorphMapping.controls(listOf("height_min", "height_max")).first()
        assertEquals(0.25f, MorphMapping.value(control, MorphMapping.weights(control, 0.25f), emptyMap()))
    }

    @Test
    fun measurementsMapWithinRange() {
        val values = Measurements(heightCm = 210f, weightKg = 40f).toControlValues()
        assertEquals(1f, values["height"])
        assertEquals(0f, values["weight"])
        assertTrue(values.getValue("proportions") in 0f..1f)
    }
}
