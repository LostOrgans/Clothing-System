package com.example.clothingsystem.avatar

import android.content.Context
import com.google.android.filament.Box
import com.google.android.filament.IndirectLight
import com.google.android.filament.View
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation
import com.google.android.filament.utils.transpose
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AvatarModel(val morphNames: List<String>, val defaultWeights: FloatArray)

object AvatarRenderer {
    private const val MODEL_PATH = "models/avatar.glb"
    data class Garment(val node: String, val hideMorph: String)

    private val garmentNodes = mapOf(
        "Tops" to Garment("Garment_Casual", "hide_Casual"),
        "Outerwear" to Garment("Garment_Elegant", "hide_Elegant"),
        "Shoes" to Garment("Garment_Shoes", "hide_Shoes"),
    )
    private val baseTransforms = mutableMapOf<String, FloatArray>()

    fun garmentFor(category: String): Garment? = garmentNodes[category]

    init {
        System.loadLibrary("filament-jni")
        System.loadLibrary("gltfio-jni")
        System.loadLibrary("filament-utils-jni")
    }

    fun configure(viewer: ModelViewer) {
        viewer.view.blendMode = View.BlendMode.OPAQUE
        viewer.cameraFocalLength = 115f
        viewer.scene.indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.7f, 0.7f, 0.7f))
            .build(viewer.engine)
    }

    fun loadModel(viewer: ModelViewer, context: Context): AvatarModel? = try {
        baseTransforms.clear()
        val bytes = context.assets.open(MODEL_PATH).use { it.readBytes() }
        val model = readMorphMetadata(bytes)
        val buffer = ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
            .flip()
        viewer.loadModelGlb(buffer)
        fitToBody(viewer)
        model
    } catch (e: Exception) {
        null
    }

    private fun fitToBody(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val body = asset.getFirstEntityByName("Avatar")
        val renderables = viewer.engine.renderableManager
        if (body == 0 || !renderables.hasComponent(body)) {
            viewer.transformToUnitCube()
            return
        }
        val box = renderables.getAxisAlignedBoundingBox(renderables.getInstance(body), Box())
        val center = Float3(box.center[0], box.center[1], box.center[2])
        val halfExtent = Float3(box.halfExtent[0], box.halfExtent[1], box.halfExtent[2])
        val scaleFactor = 2f / (2f * maxOf(halfExtent.x, halfExtent.y, halfExtent.z))
        val target = Float3(0f, 0f, -4f)
        val adjustedCenter = center - target / scaleFactor
        val transform = scale(Float3(scaleFactor)) * translation(-adjustedCenter)
        val transforms = viewer.engine.transformManager
        transforms.setTransform(
            transforms.getInstance(asset.root),
            transpose(transform).toFloatArray(),
        )
    }

    private fun readMorphMetadata(bytes: ByteArray): AvatarModel {
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val type = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (type == 0x4E4F534A) {
                val json = String(bytes, offset + 8, length, Charsets.UTF_8)
                val meshes = JSONObject(json).getJSONArray("meshes")
                // The body is the mesh with morph targets; accessory meshes come first when
                // MPFB hair/eyes are present, so never assume index 0.
                var mesh: JSONObject? = null
                for (i in 0 until meshes.length()) {
                    val candidate = meshes.getJSONObject(i)
                    if (candidate.optJSONObject("extras")?.has("targetNames") == true) {
                        mesh = candidate
                        break
                    }
                }
                if (mesh == null) return AvatarModel(emptyList(), floatArrayOf())
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

    fun applyAppearance(viewer: ModelViewer, config: AvatarConfig) {
        val asset = viewer.asset ?: return
        val renderables = viewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!renderables.hasComponent(entity)) continue
            val instance = renderables.getInstance(entity)
            for (primitive in 0 until renderables.getPrimitiveCount(instance)) {
                val material = renderables.getMaterialInstanceAt(instance, primitive) ?: continue
                val name = runCatching { material.name }.getOrDefault("")
                if (name.contains("body", ignoreCase = true) || name == "Skin") {
                    val tone = config.skinTone
                    runCatching { material.setParameter("baseColorFactor", tone.r, tone.g, tone.b) }
                }
            }
        }
    }

    fun applyGarments(viewer: ModelViewer, active: Set<Garment>) {
        val asset = viewer.asset ?: return
        val transforms = viewer.engine.transformManager
        for (garment in garmentNodes.values) {
            val node = garment.node
            val entity = asset.getFirstEntityByName(node)
            if (entity == 0) continue
            val instance = transforms.getInstance(entity)
            val base = baseTransforms.getOrPut(node) { transforms.getTransform(instance, FloatArray(16)) }
            val scaled = base.copyOf()
            val factor = if (active.any { it.node == node }) 1f else 0.001f
            for (index in intArrayOf(0, 1, 2, 4, 5, 6, 8, 9, 10)) scaled[index] *= factor
            transforms.setTransform(instance, scaled)
        }
    }

    fun applyWeights(
        viewer: ModelViewer,
        model: AvatarModel,
        config: AvatarConfig,
        activeGarments: Set<Garment> = emptySet(),
    ) {
        val asset = viewer.asset ?: return
        val renderables = viewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!renderables.hasComponent(entity)) continue
            val instance = renderables.getInstance(entity)
            val count = renderables.getMorphTargetCount(instance)
            if (count <= 0) continue
            val weights = FloatArray(count) { i ->
                val name = model.morphNames.getOrNull(i) ?: return@FloatArray 0f
                if (activeGarments.any { it.hideMorph == name }) 1f
                else config.weight(name, model.defaultWeights.getOrNull(i) ?: 0f)
            }
            renderables.setMorphWeights(instance, weights, 0)
        }
    }
}
