package com.example.clothingsystem.avatar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.avatarDataStore: DataStore<Preferences> by preferencesDataStore(name = "avatar")

data class AvatarConfig(
    val slim: Float = 0f,
    val tall: Float = 0f,
    val muscle: Float = 0f,
) {
    init {
        require(slim in 0f..1f && tall in 0f..1f && muscle in 0f..1f) {
            "Morph weights must be between 0 and 1"
        }
    }

    // Order must match the shape keys in tools/make_avatar.py.
    fun morphWeights(): FloatArray = floatArrayOf(slim, tall, muscle)

    object Prefs {
        private val slimKey = floatPreferencesKey("slim")
        private val tallKey = floatPreferencesKey("tall")
        private val muscleKey = floatPreferencesKey("muscle")

        suspend fun read(context: Context): AvatarConfig {
            val prefs = context.avatarDataStore.data.first()
            return AvatarConfig(
                slim = prefs[slimKey] ?: 0f,
                tall = prefs[tallKey] ?: 0f,
                muscle = prefs[muscleKey] ?: 0f,
            )
        }

        suspend fun write(context: Context, config: AvatarConfig) {
            context.avatarDataStore.edit { prefs ->
                prefs[slimKey] = config.slim
                prefs[tallKey] = config.tall
                prefs[muscleKey] = config.muscle
            }
        }
    }
}
