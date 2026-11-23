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

            suspend fun read(context: Context): AvatarConfig {
                val prefs = context.avatarDataStore.data.first()
                return AvatarConfig(
                    weights = deserialise(prefs[key] ?: "").weights,
                    skinTone = SkinTone.fromName(prefs[skinKey]),
                )
            }

            suspend fun write(context: Context, config: AvatarConfig) {
                context.avatarDataStore.edit { prefs ->
                    prefs[key] = config.serialise()
                    prefs[skinKey] = config.skinTone.name
                }
            }
        }
    }
}
