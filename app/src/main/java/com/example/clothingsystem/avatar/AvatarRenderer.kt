package com.example.clothingsystem.avatar

import android.content.Context
import com.google.android.filament.IndirectLight
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AvatarModel(val morphNames: List<String>, val defaultWeights: FloatArray)

object AvatarRenderer {
    private const val MODEL_PATH = "models/avatar.glb"

    init {
        System.loadLibrary("filament-jni")
        System.loadLibrary("gltfio-jni")
        System.loadLibrary("filament-utils-jni")
    }

    fun configure(viewer: ModelViewer) {
        viewer.view.blendMode = View.BlendMode.TRANSLUCENT
        viewer.cameraFocalLength = 75f
        viewer.scene.indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.7f, 0.7f, 0.7f))
            .build(viewer.engine)
    }

    fun loadModel(viewer: ModelViewer, context: Context): AvatarModel? = try {
        val bytes = context.assets.open(MODEL_PATH).use { it.readBytes() }
        val model = readMorphMetadata(bytes)
        val buffer = ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
            .flip()
        viewer.loadModelGlb(buffer)
        viewer.transformToUnitCube()
        model
    } catch (e: Exception) {
        null
    }

    private fun readMorphMetadata(bytes: ByteArray): AvatarModel {
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val type = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (type == 0x4E4F534A) {
                val json = String(bytes, offset + 8, length, Charsets.UTF_8)
                val mesh = JSONObject(json).getJSONArray("meshes").getJSONObject(0)
                val names = mesh.getJSONObject("extras").getJSONArray("targetNames")
                val weights = mesh.optJSONArray("weights")
                return AvatarModel(
                    morphNames = List(names.length()) { names.getString(it) },
                    defaultWeights = FloatArray(names.length()) { i ->
                        weights?.optDouble(i)?.toFloat() ?: 0f
                    },
                )
            }
            offset += 8 + length
        }
        return AvatarModel(emptyList(), floatArrayOf())
    }

    fun applyWeights(viewer: ModelViewer, model: AvatarModel, config: AvatarConfig) {
        val asset = viewer.asset ?: return
        val renderables = viewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!renderables.hasComponent(entity)) continue
            val instance = renderables.getInstance(entity)
            val count = renderables.getMorphTargetCount(instance)
            if (count <= 0) continue
            val weights = FloatArray(count) { i ->
                val name = model.morphNames.getOrNull(i) ?: return@FloatArray 0f
                config.weight(name, model.defaultWeights.getOrNull(i) ?: 0f)
            }
            renderables.setMorphWeights(instance, weights, 0)
        }
    }
}
