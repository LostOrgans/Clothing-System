package com.example.clothingsystem.avatar

enum class SkinTone(val label: String, val r: Float, val g: Float, val b: Float, val hex: Long) {
    FAIR("Fair", 0.96f, 0.83f, 0.74f, 0xFFF5D9BC),
    LIGHT("Light", 0.87f, 0.70f, 0.60f, 0xFFE8B399),
    MEDIUM("Medium", 0.76f, 0.59f, 0.48f, 0xFFC2907A),
    TAN("Tan", 0.63f, 0.45f, 0.33f, 0xFFA27154),
    BROWN("Brown", 0.45f, 0.30f, 0.21f, 0xFF734D36),
    DEEP("Deep", 0.28f, 0.18f, 0.13f, 0xFF472E21);

    companion object {
        fun fromName(name: String?): SkinTone = entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
