package com.example.clothingsystem.avatar

import android.content.Context
import com.google.android.filament.IndirectLight
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AvatarRenderer {
    private const val MODEL_PATH = "models/avatar.glb"

    fun configure(viewer: ModelViewer) {
        viewer.view.blendMode = View.BlendMode.TRANSLUCENT
        // Constant irradiance stands in for an IBL so PBR materials are not black.
        viewer.scene.indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.7f, 0.7f, 0.7f))
            .build(viewer.engine)
    }

    fun loadModel(viewer: ModelViewer, context: Context): Boolean = try {
        val bytes = context.assets.open(MODEL_PATH).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
            .flip()
        viewer.loadModelGlb(buffer)
        viewer.transformToUnitCube()
        true
    } catch (e: Exception) {
        false
    }

    fun applyWeights(viewer: ModelViewer, config: AvatarConfig) {
        val asset = viewer.asset ?: return
        val weights = config.morphWeights()
        val renderables = viewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!renderables.hasComponent(entity)) continue
            val instance = renderables.getInstance(entity)
            val count = renderables.getMorphTargetCount(instance)
            if (count <= 0) continue
            val safeWeights = if (count == weights.size) weights else weights.copyOf(count.coerceAtMost(weights.size))
            renderables.setMorphWeights(instance, safeWeights, 0)
        }
    }
}
