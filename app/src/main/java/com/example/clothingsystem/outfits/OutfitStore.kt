package com.example.clothingsystem.outfits

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.outfitDataStore: DataStore<Preferences> by preferencesDataStore(name = "outfits")

class OutfitStore(private val context: Context) {
    private val key = stringPreferencesKey("saved")

    suspend fun load(): List<Outfit> {
        val raw = context.outfitDataStore.data.first()[key] ?: ""
        return raw.split(';').filter { it.isNotBlank() }.mapNotNull { Outfit.deserialise(it) }
    }

    suspend fun save(outfits: List<Outfit>) {
        context.outfitDataStore.edit { it[key] = outfits.joinToString(";") { o -> o.serialise() } }
    }
}
