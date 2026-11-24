package com.example.clothingsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.clothingsystem.ui.AvatarScreen
import com.example.clothingsystem.ui.DesignScreen
import com.example.clothingsystem.ui.OutfitsScreen
import com.example.clothingsystem.ui.WardrobeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("filament-jni")
        System.loadLibrary("gltfio-jni")
        System.loadLibrary("filament-utils-jni")
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val tab = remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            when (tab.value) {
                0 -> AvatarScreen()
                1 -> WardrobeScreen()
                2 -> OutfitsScreen()
                else -> DesignScreen()
            }
        }
        NavigationBar(modifier = Modifier.fillMaxWidth()) {
            NavigationBarItem(
                selected = tab.value == 0,
                onClick = { tab.value = 0 },
                label = { Text("Avatar") },
                icon = {},
            )
            NavigationBarItem(
                selected = tab.value == 1,
                onClick = { tab.value = 1 },
                label = { Text("Wardrobe") },
                icon = {},
            )
            NavigationBarItem(
                selected = tab.value == 2,
                onClick = { tab.value = 2 },
                label = { Text("Outfits") },
                icon = {},
            )
            NavigationBarItem(
                selected = tab.value == 3,
                onClick = { tab.value = 3 },
                label = { Text("Design") },
                icon = {},
            )
        }
    }
}
