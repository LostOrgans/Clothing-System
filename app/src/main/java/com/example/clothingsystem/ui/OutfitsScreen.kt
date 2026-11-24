package com.example.clothingsystem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.outfits.OutfitEngine
import com.example.clothingsystem.outfits.OutfitSuggestion
import com.example.clothingsystem.outfits.OutfitViewModel
import com.example.clothingsystem.wardrobe.WardrobeViewModel

@Composable
fun OutfitsScreen(
    outfits: OutfitViewModel = viewModel(),
    wardrobe: WardrobeViewModel = viewModel(),
) {
    val items by wardrobe.items.collectAsState()
    val saved by outfits.saved.collectAsState()
    val name = remember { mutableStateOf("") }
    val selected = remember { mutableStateOf<OutfitSuggestion?>(null) }
    val suggestions = remember(items) { OutfitEngine.suggest(items) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        item {
            Text("Suggested outfits", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            if (suggestions.isEmpty()) {
                Text(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    text = "Add at least one Tops and one Bottoms item to your wardrobe to get suggestions.",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        items(suggestions, key = { it.name }) { suggestion ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(suggestion.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                Text(suggestion.reason, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                Row {
                    Button(onClick = { selected.value = suggestion }) { Text("Select") }
                    TextButton(onClick = {
                        outfits.add("Outfit ${saved.size + 1}", suggestion.itemIds)
                    }) { Text("Save") }
                }
            }
        }
        item {
            Text(
                "Saved outfits",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = { Text("Outfit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = {
                    selected.value?.let { outfits.add(name.value.ifBlank { it.name }, it.itemIds) }
                    name.value = ""
                },
                enabled = selected.value != null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Save selected outfit")
            }
        }
        items(saved, key = { it.id }) { outfit ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(outfit.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    Text("${outfit.itemIds.size} items", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                }
                TextButton(onClick = { outfits.remove(outfit.id) }) { Text("Delete") }
            }
        }
    }
}
