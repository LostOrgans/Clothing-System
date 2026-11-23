package com.example.clothingsystem.avatar

import kotlin.test.Test
import kotlin.test.assertEquals

class AvatarConfigTest {
    @Test
    fun serialiseRoundTripsMorphWeights() {
        val config = AvatarConfig().with("height_max", 0.75f).with("weight_min", 0.25f)
        assertEquals(config, AvatarConfig.deserialise(config.serialise()))
    }

    @Test
    fun missingWeightFallsBackToDefault() {
        assertEquals(0.4f, AvatarConfig().weight("age_max", 0.4f))
    }

    @Test
    fun ignoresMalformedEntries() {
        assertEquals(emptyMap(), AvatarConfig.deserialise("broken;also_broken").weights)
    }
}
