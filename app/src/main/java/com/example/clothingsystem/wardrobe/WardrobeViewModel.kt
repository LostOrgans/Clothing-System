package com.example.clothingsystem.wardrobe

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WardrobeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = WardrobeStore(application)
    private val _items = MutableStateFlow<List<ClothingItem>>(emptyList())
    val items: StateFlow<List<ClothingItem>> = _items
    private val _worn = MutableStateFlow<List<String>>(emptyList())
    val worn: StateFlow<List<String>> = _worn

    init {
        viewModelScope.launch {
            _items.value = store.load()
            _worn.value = store.loadWorn()
        }
    }

    fun add(name: String, category: String, color: String, photoUri: Uri?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val photoFile = photoUri?.let { store.savePhoto(it) }
            val updated = _items.value + ClothingItem(
                name = name.trim(), category = category, color = color, photoFile = photoFile,
            )
            _items.value = updated
            store.save(updated)
        }
    }

    fun toggleWorn(id: String) {
        val updated = if (id in _worn.value) _worn.value - id else _worn.value + id
        _worn.value = updated
        viewModelScope.launch { store.saveWorn(updated) }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            val updated = _items.value.filterNot { it.id == id }
            _items.value = updated
            _worn.value = _worn.value - id
            store.save(updated)
            store.saveWorn(_worn.value)
        }
    }
}
