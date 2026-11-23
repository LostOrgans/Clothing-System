package com.example.clothingsystem.avatar

data class MorphControl(
    val key: String,
    val label: String,
    val paired: Boolean,
)

object MorphMapping {
    private val labels = mapOf(
        "gender" to "Gender",
        "age" to "Age",
        "muscle" to "Muscle",
        "weight" to "Weight",
        "proportions" to "Proportions",
        "height" to "Height",
        "cupsize" to "Cup size",
        "firmness" to "Firmness",
    )

    fun controls(names: List<String>): List<MorphControl> {
        val pairedBases = names.filter { it.endsWith("_min") }.map { it.removeSuffix("_min") }.toSet()
        return names.mapNotNull { name ->
            when {
                name.endsWith("_min") && pairedBases.contains(name.removeSuffix("_min")) -> {
                    val base = name.removeSuffix("_min")
                    MorphControl(base, pretty(base), true)
                }
                name.endsWith("_max") && pairedBases.contains(name.removeSuffix("_max")) -> null
                else -> MorphControl(name, pretty(name), false)
            }
        }
    }

    fun weights(control: MorphControl, value: Float): Map<String, Float> {
        if (!control.paired) return mapOf(control.key to value)
        val min = ((0.5f - value) * 2f).coerceIn(0f, 1f)
        val max = ((value - 0.5f) * 2f).coerceIn(0f, 1f)
        return mapOf("${control.key}_min" to min, "${control.key}_max" to max)
    }

    fun value(control: MorphControl, weights: Map<String, Float>, defaults: Map<String, Float>): Float {
        if (!control.paired) return weights[control.key] ?: defaults[control.key] ?: 0f
        val min = weights["${control.key}_min"] ?: defaults["${control.key}_min"] ?: 0f
        val max = weights["${control.key}_max"] ?: defaults["${control.key}_max"] ?: 0f
        return (0.5f + (max - min) / 2f).coerceIn(0f, 1f)
    }

    private fun pretty(name: String): String = labels[name] ?: name
        .replace("\$md-", "detail ")
        .replace("\$as", "Asian")
        .replace("\$ca", "Caucasian")
        .replace("\$af", "African")
        .replace("\$fe", "female")
        .replace("\$ma", "male")
        .replace("\$yn", "young")
        .replace("\$av", "average")
        .replace("\$mu", "muscle")
        .replace("\$wg", "weight")
        .replace("\$", "")
        .replace('-', ' ')
        .trim()
        .replaceFirstChar { it.uppercase() }
}

data class Measurements(
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val chestCm: Float = 95f,
    val waistCm: Float = 85f,
    val hipsCm: Float = 98f,
    val shoulderCm: Float = 44f,
    val ageYears: Float = 30f,
    val muscleTone: Float = 5f,
    val firmness: Float = 5f,
) {
    fun toControlValues(): Map<String, Float> {
        fun norm(value: Float, min: Float, max: Float) = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return mapOf(
            "height" to norm(heightCm, 140f, 210f),
            "weight" to (norm(weightKg, 40f, 150f) * 0.75f + norm(waistCm, 55f, 140f) * 0.25f),
            "cupsize" to norm(chestCm, 70f, 130f),
            "proportions" to (norm(hipsCm, 75f, 150f) * 0.6f + norm(shoulderCm, 35f, 55f) * 0.4f),
            "muscle" to norm(muscleTone, 0f, 10f),
            "age" to norm(ageYears, 5f, 90f),
            "firmness" to norm(firmness, 0f, 10f),
        )
    }

    companion object {
        val FIELDS = listOf(
            Field("Height", "cm", 140f, 210f) { it.heightCm },
            Field("Weight", "kg", 40f, 150f) { it.weightKg },
            Field("Chest", "cm", 70f, 130f) { it.chestCm },
            Field("Waist", "cm", 55f, 140f) { it.waistCm },
            Field("Hips", "cm", 75f, 150f) { it.hipsCm },
            Field("Shoulders", "cm", 35f, 55f) { it.shoulderCm },
            Field("Age", "years", 5f, 90f) { it.ageYears },
            Field("Muscle tone", "0-10", 0f, 10f) { it.muscleTone },
            Field("Firmness", "0-10", 0f, 10f) { it.firmness },
        )
    }

    data class Field(
        val label: String,
        val unit: String,
        val min: Float,
        val max: Float,
        val get: (Measurements) -> Float,
    )
}
