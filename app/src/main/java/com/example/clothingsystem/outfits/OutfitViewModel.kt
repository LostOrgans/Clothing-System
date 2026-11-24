package com.example.clothingsystem.outfits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutfitViewModel(application: Application) : AndroidViewModel(application) {
    private val store = OutfitStore(application)
    private val _saved = MutableStateFlow<List<Outfit>>(emptyList())
    val saved: StateFlow<List<Outfit>> = _saved

    init {
        viewModelScope.launch { _saved.value = store.load() }
    }

    fun add(name: String, itemIds: List<String>) {
        if (name.isBlank() || itemIds.isEmpty()) return
        viewModelScope.launch {
            val updated = _saved.value + Outfit(name = name.trim(), itemIds = itemIds)
            _saved.value = updated
            store.save(updated)
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            val updated = _saved.value.filterNot { it.id == id }
            _saved.value = updated
            store.save(updated)
        }
    }
}
