package com.example.clothingsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.clothingsystem.ui.AvatarScreen
import com.example.clothingsystem.ui.WardrobeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(0x20, 0x24, 0x2B)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
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
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Button(onClick = { tab.value = 0 }, modifier = Modifier.weight(1f)) {
                Text("Avatar")
            }
            Button(onClick = { tab.value = 1 }, modifier = Modifier.weight(1f)) {
                Text("Wardrobe")
            }
        }
        if (tab.value == 0) AvatarScreen() else WardrobeScreen()
    }
}
