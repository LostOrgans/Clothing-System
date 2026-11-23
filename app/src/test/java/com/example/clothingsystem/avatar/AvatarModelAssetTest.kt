package com.example.clothingsystem.avatar

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvatarModelAssetTest {
    private val glb = File("src/main/assets/models/avatar.glb")

    private fun jsonChunk(): String {
        assertTrue(glb.exists(), "avatar.glb must be generated and committed")
        val bytes = glb.readBytes()
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val type = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (type == 0x4E4F534A) return String(bytes, offset + 8, length, Charsets.UTF_8)
            offset += 8 + length
        }
        error("No JSON chunk in GLB")
    }

    @Test
    fun modelHasExpectedMorphTargets() {
        val json = jsonChunk()
        assertTrue(json.contains("height_max"), "height metric must map to a morph target")
        assertTrue(json.contains("weight_max"), "weight metric must map to a morph target")
        assertTrue(json.contains("cupsize_max"), "chest metric must map to a morph target")
        assertTrue(json.contains("age_max"), "age metric must map to a morph target")
    }

    @Test
    fun modelHasSkinTexture() {
        val json = jsonChunk()
        assertTrue(json.contains("\"images\""), "generated human must have a skin texture")
        assertTrue(json.contains("\"textures\""), "generated human must reference a texture")
    }

    @Test
    fun allMorphsStartAtZero() {
        val json = jsonChunk()
        val weights = Regex("\"weights\":\\[([^]]*)]").find(json)?.groupValues?.get(1) ?: ""
        val nonzero = weights.split(',').map { it.trim().toFloatOrNull() ?: 0f }.count { it != 0f }
        assertEquals(0, nonzero, "morphs must start neutral; the app drives them from measurements")
    }
}
