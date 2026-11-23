package com.example.clothingsystem.avatar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AvatarViewModel(application: Application) : AndroidViewModel(application) {
    private val _config = MutableStateFlow(AvatarConfig())
    val config: StateFlow<AvatarConfig> = _config

    init {
        viewModelScope.launch {
            _config.value = AvatarConfig.Companion.Prefs.read(getApplication())
        }
    }

    fun setWeights(weights: Map<String, Float>) = update { old -> old.copy(weights = old.weights + weights) }
    fun setSkinTone(tone: SkinTone) = update { it.copy(skinTone = tone) }
    fun setHairColor(color: AppearanceColor) = update { it.copy(hairColor = color) }
    fun setEyeColor(color: AppearanceColor) = update { it.copy(eyeColor = color) }
    fun setNippleColor(color: AppearanceColor) = update { it.copy(nippleColor = color) }
    fun reset() = update { AvatarConfig() }

    private fun update(transform: (AvatarConfig) -> AvatarConfig) {
        val next = transform(_config.value)
        _config.value = next
        viewModelScope.launch { AvatarConfig.Companion.Prefs.write(getApplication(), next) }
    }
}
