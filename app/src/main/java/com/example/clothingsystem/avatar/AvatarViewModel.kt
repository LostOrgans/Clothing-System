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
            _config.value = AvatarConfig.Prefs.read(getApplication())
        }
    }

    fun setSlim(value: Float) = update { it.copy(slim = value) }
    fun setTall(value: Float) = update { it.copy(tall = value) }
    fun setMuscle(value: Float) = update { it.copy(muscle = value) }

    private fun update(transform: (AvatarConfig) -> AvatarConfig) {
        val next = transform(_config.value)
        _config.value = next
        viewModelScope.launch { AvatarConfig.Prefs.write(getApplication(), next) }
    }
}
