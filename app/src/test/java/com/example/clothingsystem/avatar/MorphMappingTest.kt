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
    fun defaultMeasurementsAreNeutral() {
        val values = Measurements().toControlValues()
        assertTrue(values.values.all { it == 0.5f }, "default measurements must map to neutral morphs: $values")
    }

    @Test
    fun defaultMeasurementsProduceZeroMorphWeights() {
        val controls = MorphMapping.controls(FAKE_MORPHS)
        val weights = Measurements().toControlValues().flatMap { (key, value) ->
            val control = controls.firstOrNull { it.key == key }
            if (control != null) MorphMapping.weights(control, value).entries else emptySet()
        }.associate { it.key to it.value }
        assertTrue(weights.values.all { it == 0f }, "defaults must not deform the body: $weights")
    }

    @Test
    fun tallMeasurementPushesTowardsMax() {
        val values = Measurements(heightCm = 210f).toControlValues()
        assertTrue(values.getValue("height") > 0.5f)
        val control = MorphMapping.controls(listOf("height_min", "height_max")).first()
        assertTrue(MorphMapping.weights(control, values.getValue("height")).getValue("height_max") > 0f)
    }

    @Test
    fun measurementsMapWithinRange() {
        val values = Measurements(heightCm = 210f, weightKg = 40f).toControlValues()
        assertTrue(values.getValue("height") in 0f..1f)
        assertTrue(values.getValue("weight") in 0f..1f)
        assertTrue(values.getValue("proportions") in 0f..1f)
    }

    private companion object {
        val FAKE_MORPHS = listOf(
            "gender_min", "gender_max", "age_min", "age_max", "muscle_min", "muscle_max",
            "weight_min", "weight_max", "proportions_min", "proportions_max", "height_min",
            "height_max", "cupsize_min", "cupsize_max", "firmness_min", "firmness_max",
        )
    }
}
