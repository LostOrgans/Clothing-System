package com.example.clothingsystem.wardrobe

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val Context.wardrobeDataStore: DataStore<Preferences> by preferencesDataStore(name = "wardrobe")

class WardrobeStore(private val context: Context) {
    private val itemsKey = stringPreferencesKey("items")

    suspend fun load(): List<ClothingItem> {
        val raw = context.wardrobeDataStore.data.first()[itemsKey] ?: ""
        return raw.split(';').filter { it.isNotBlank() }.mapNotNull { ClothingItem.deserialise(it) }
    }

    suspend fun save(items: List<ClothingItem>) {
        context.wardrobeDataStore.edit { it[itemsKey] = items.joinToString(";") { item -> item.serialise() } }
    }

    suspend fun savePhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        val name = "item_${UUID.randomUUID()}.jpg"
        val file = File(File(context.filesDir, "images"), name)
        file.parentFile?.mkdirs()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            name
        } catch (e: Exception) {
            null
        }
    }

    fun photoFile(name: String): File = File(File(context.filesDir, "images"), name)
}
