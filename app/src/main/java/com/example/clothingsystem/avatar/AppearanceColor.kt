package com.example.clothingsystem.avatar

data class AppearanceColor(val label: String, val hex: Long, val r: Float, val g: Float, val b: Float)

object AppearanceColors {
    val HAIR = listOf(
        AppearanceColor("Black", 0xFF211A16, 0.10f, 0.07f, 0.05f),
        AppearanceColor("Brown", 0xFF5A3A24, 0.28f, 0.16f, 0.09f),
        AppearanceColor("Blonde", 0xFFD9B45C, 0.72f, 0.53f, 0.20f),
        AppearanceColor("Red", 0xFFA64324, 0.50f, 0.14f, 0.05f),
        AppearanceColor("Grey", 0xFF9C9C9C, 0.42f, 0.42f, 0.42f),
        AppearanceColor("White", 0xFFF0EDE8, 0.88f, 0.86f, 0.82f),
    )

    val EYE = listOf(
        AppearanceColor("Brown", 0xFF5A3A24, 0.28f, 0.16f, 0.09f),
        AppearanceColor("Blue", 0xFF3D78BF, 0.25f, 0.45f, 0.65f),
        AppearanceColor("Green", 0xFF4C8A5A, 0.18f, 0.45f, 0.25f),
        AppearanceColor("Hazel", 0xFF9A7A3C, 0.42f, 0.30f, 0.10f),
        AppearanceColor("Grey", 0xFF8C9096, 0.36f, 0.38f, 0.40f),
    )

    val NIPPLE = listOf(
        AppearanceColor("Light", 0xFFC2907A, 0.76f, 0.59f, 0.48f),
        AppearanceColor("Medium", 0xFFA27154, 0.63f, 0.45f, 0.33f),
        AppearanceColor("Brown", 0xFF734D36, 0.45f, 0.30f, 0.21f),
        AppearanceColor("Dark", 0xFF472E21, 0.28f, 0.18f, 0.13f),
    )

    fun hair(name: String?) = HAIR.firstOrNull { it.label == name } ?: HAIR[0]
    fun eye(name: String?) = EYE.firstOrNull { it.label == name } ?: EYE[1]
    fun nipple(name: String?) = NIPPLE.firstOrNull { it.label == name } ?: NIPPLE[1]
}
