package com.example.clothingsystem.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.wardrobe.CATEGORIES
import com.example.clothingsystem.wardrobe.COLORS
import com.example.clothingsystem.wardrobe.WardrobeStore
import com.example.clothingsystem.wardrobe.WardrobeViewModel

@Composable
fun WardrobeScreen(viewModel: WardrobeViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val name = remember { mutableStateOf("") }
    val category = remember { mutableStateOf(CATEGORIES.first()) }
    val color = remember { mutableStateOf(COLORS.first().first) }
    val photoUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri.value = uri
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Item name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Category", modifier = Modifier.padding(top = 8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (option in CATEGORIES) {
                TextButton(onClick = { category.value = option }) {
                    Text(if (category.value == option) "[$option]" else option)
                }
            }
        }
        Text("Color", modifier = Modifier.padding(top = 4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for ((label, hex) in COLORS) {
                TextButton(onClick = { color.value = label }) {
                    Text(if (color.value == label) "[$label]" else label, color = Color(hex))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { picker.launch("image/*") }) { Text("Add photo") }
            Spacer(Modifier.width(8.dp))
            Text(if (photoUri.value == null) "No photo" else "Photo selected")
        }
        Button(
            onClick = {
                viewModel.add(name.value, category.value, color.value, photoUri.value)
                name.value = ""
                photoUri.value = null
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Add item")
        }

        Text("Your wardrobe (${items.size})", modifier = Modifier.padding(top = 16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                WardrobeRow(item, viewModel)
            }
        }
    }
}

@Composable
private fun WardrobeRow(
    item: com.example.clothingsystem.wardrobe.ClothingItem,
    viewModel: WardrobeViewModel,
) {
    val context = LocalContext.current
    val store = remember { WardrobeStore(context) }
    val image = remember(item.photoFile) {
        item.photoFile?.let { file ->
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(store.photoFile(file).absolutePath, options)?.asImageBitmap()
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp)) {
            if (image != null) {
                Image(image, contentDescription = null, modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name)
            Text("${item.category} · ${item.color}")
        }
        TextButton(onClick = { viewModel.remove(item.id) }) { Text("Delete") }
    }
    Spacer(Modifier.height(2.dp))
}
