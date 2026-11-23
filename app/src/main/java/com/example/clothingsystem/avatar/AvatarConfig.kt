package com.example.clothingsystem.avatar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.avatarDataStore: DataStore<Preferences> by preferencesDataStore(name = "avatar")

data class AvatarConfig(
    val weights: Map<String, Float> = emptyMap(),
    val skinTone: SkinTone = SkinTone.MEDIUM,
    val hairColor: AppearanceColor = AppearanceColors.HAIR[0],
    val eyeColor: AppearanceColor = AppearanceColors.EYE[1],
    val nippleColor: AppearanceColor = AppearanceColors.NIPPLE[1],
) {
    fun weight(name: String, default: Float): Float = weights[name] ?: default

    fun with(name: String, value: Float): AvatarConfig = copy(weights = weights + (name to value))

    fun serialise(): String = weights.entries.joinToString(";") { "${it.key}=${it.value}" }

    companion object {
        fun deserialise(value: String): AvatarConfig = AvatarConfig(
            value.split(';').mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1].toFloatOrNull() else null
            }.mapNotNull { (key, weight) -> weight?.let { key to it } }.toMap(),
        )

        object Prefs {
            private val key = stringPreferencesKey("morph_weights")
            private val skinKey = stringPreferencesKey("skin_tone")
            private val hairKey = stringPreferencesKey("hair_color")
            private val eyeKey = stringPreferencesKey("eye_color")
            private val nippleKey = stringPreferencesKey("nipple_color")

            suspend fun read(context: Context): AvatarConfig {
                val prefs = context.avatarDataStore.data.first()
                return AvatarConfig(
                    weights = deserialise(prefs[key] ?: "").weights,
                    skinTone = SkinTone.fromName(prefs[skinKey]),
                    hairColor = AppearanceColors.hair(prefs[hairKey]),
                    eyeColor = AppearanceColors.eye(prefs[eyeKey]),
                    nippleColor = AppearanceColors.nipple(prefs[nippleKey]),
                )
            }

            suspend fun write(context: Context, config: AvatarConfig) {
                context.avatarDataStore.edit { prefs ->
                    prefs[key] = config.serialise()
                    prefs[skinKey] = config.skinTone.name
                    prefs[hairKey] = config.hairColor.label
                    prefs[eyeKey] = config.eyeColor.label
                    prefs[nippleKey] = config.nippleColor.label
                }
            }
        }
    }
}
