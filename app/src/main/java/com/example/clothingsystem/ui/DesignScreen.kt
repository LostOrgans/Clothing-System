package com.example.clothingsystem.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.wardrobe.COLORS
import com.example.clothingsystem.wardrobe.WardrobeViewModel

private data class GarmentType(val label: String, val category: String, val design: String)

private val TYPES = listOf(
    GarmentType("T-shirt", "Tops", "tshirt"),
    GarmentType("Jacket", "Outerwear", "jacket"),
    GarmentType("Trousers", "Bottoms", "trousers"),
    GarmentType("Shoes", "Shoes", "shoes"),
)

@Composable
fun DesignScreen(viewModel: WardrobeViewModel = viewModel()) {
    val name = remember { mutableStateOf("My design") }
    val type = remember { mutableStateOf(TYPES[0]) }
    val color = remember { mutableStateOf(COLORS.last()) }
    val saved = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Text("Clothing designer", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)

        Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(vertical = 12.dp)) {
            GarmentPreview(type.value.design, Color(color.value.second))
        }

        Text("Garment", color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (option in TYPES) {
                TextButton(onClick = { type.value = option }) {
                    Text(if (type.value == option) "[${option.label}]" else option.label)
                }
            }
        }

        Text("Colour", color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in COLORS) {
                Button(
                    onClick = { color.value = option },
                    modifier = Modifier.size(44.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(option.second),
                    ),
                ) {}
            }
        }

        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Button(
            onClick = {
                viewModel.add(
                    name = name.value.ifBlank { "My design" },
                    category = type.value.category,
                    color = color.value.first,
                    photoUri = null,
                    design = type.value.design,
                )
                saved.value = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("Add to wardrobe")
        }
        if (saved.value) {
            Text("Added — find it in Wardrobe and tap Wear.",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun GarmentPreview(design: String, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (design) {
            "tshirt" -> drawTshirt(color, w, h)
            "jacket" -> drawJacket(color, w, h)
            "trousers" -> drawTrousers(color, w, h)
            "shoes" -> drawShoes(color, w, h)
        }
    }
}

private fun DrawScope.drawTshirt(color: Color, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.35f, h * 0.18f)
        lineTo(w * 0.22f, h * 0.30f)
        lineTo(w * 0.30f, h * 0.42f)
        lineTo(w * 0.35f, h * 0.36f)
        lineTo(w * 0.35f, h * 0.85f)
        lineTo(w * 0.65f, h * 0.85f)
        lineTo(w * 0.65f, h * 0.36f)
        lineTo(w * 0.70f, h * 0.42f)
        lineTo(w * 0.78f, h * 0.30f)
        lineTo(w * 0.65f, h * 0.18f)
        lineTo(w * 0.58f, h * 0.14f)
        lineTo(w * 0.42f, h * 0.14f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawJacket(color: Color, w: Float, h: Float) {
    drawTshirt(color, w, h)
    drawLine(color, Offset(w * 0.5f, h * 0.16f), Offset(w * 0.5f, h * 0.85f), strokeWidth = 6f)
}

private fun DrawScope.drawTrousers(color: Color, w: Float, h: Float) {
    drawRect(color, Offset(w * 0.30f, h * 0.15f), Size(w * 0.40f, h * 0.20f))
    drawRect(color, Offset(w * 0.31f, h * 0.35f), Size(w * 0.17f, h * 0.55f))
    drawRect(color, Offset(w * 0.52f, h * 0.35f), Size(w * 0.17f, h * 0.55f))
}

private fun DrawScope.drawShoes(color: Color, w: Float, h: Float) {
    drawRoundRect(color, Offset(w * 0.18f, h * 0.45f), Size(w * 0.28f, h * 0.20f))
    drawRoundRect(color, Offset(w * 0.54f, h * 0.45f), Size(w * 0.28f, h * 0.20f))
}
