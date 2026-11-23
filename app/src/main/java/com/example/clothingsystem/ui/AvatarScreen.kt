package com.example.clothingsystem.ui

import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.R
import com.example.clothingsystem.avatar.AvatarRenderer
import com.example.clothingsystem.avatar.AvatarViewModel
import com.google.android.filament.utils.ModelViewer

private data class FrameHandle(
    val choreographer: Choreographer,
    val callback: Choreographer.FrameCallback,
)

@Composable
fun AvatarScreen(viewModel: AvatarViewModel = viewModel()) {
    val config by viewModel.config.collectAsState()
    val latestConfig = remember { mutableStateOf(config) }
    latestConfig.value = config
    val modelReady = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF20242B)),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SurfaceView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val viewer = ModelViewer(this)
                        AvatarRenderer.configure(viewer)
                        modelReady.value = AvatarRenderer.loadModel(viewer, context)
                        setOnTouchListener(viewer)
                        val choreographer = Choreographer.getInstance()
                        val callback = object : Choreographer.FrameCallback {
                            override fun doFrame(frameTimeNanos: Long) {
                                choreographer.postFrameCallback(this)
                                AvatarRenderer.applyWeights(viewer, latestConfig.value)
                                viewer.render(frameTimeNanos)
                            }
                        }
                        choreographer.postFrameCallback(callback)
                        tag = FrameHandle(choreographer, callback)
                    }
                },
                onRelease = { surfaceView ->
                    (surfaceView.tag as? FrameHandle)?.let {
                        it.choreographer.removeFrameCallback(it.callback)
                    }
                    surfaceView.tag = null
                },
            )
            if (!modelReady.value) {
                Text(
                    text = stringResource(R.string.avatar_missing),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.avatar_hint),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
        SliderRow("Slimness", config.slim, viewModel::setSlim)
        SliderRow("Height", config.tall, viewModel::setTall)
        SliderRow("Muscle", config.muscle, viewModel::setMuscle)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Text(text = "$label: ${(value * 100).toInt()}%", color = Color.White)
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}
