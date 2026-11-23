package com.example.clothingsystem.ui

import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.R
import com.example.clothingsystem.avatar.AvatarModel
import com.example.clothingsystem.avatar.AvatarRenderer
import com.example.clothingsystem.avatar.AvatarViewModel
import com.example.clothingsystem.avatar.Measurements
import com.example.clothingsystem.avatar.MorphControl
import com.example.clothingsystem.avatar.MorphMapping
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
    val model = remember { mutableStateOf<AvatarModel?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SurfaceView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val viewer = ModelViewer(this)
                        AvatarRenderer.configure(viewer)
                        model.value = AvatarRenderer.loadModel(viewer, context)
                        setOnTouchListener(viewer)
                        val choreographer = Choreographer.getInstance()
                        val callback = object : Choreographer.FrameCallback {
                            override fun doFrame(frameTimeNanos: Long) {
                                choreographer.postFrameCallback(this)
                                model.value?.let { AvatarRenderer.applyWeights(viewer, it, latestConfig.value) }
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
            if (model.value == null) {
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
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            val current = model.value
            if (current != null) {
                val defaults = current.morphNames.zip(current.defaultWeights.toTypedArray()).toMap()
                val controls = MorphMapping.controls(current.morphNames)
                for (control in controls) {
                    val value = MorphMapping.value(control, config.weights, defaults)
                    MorphSlider(control, value) { viewModel.setWeights(MorphMapping.weights(control, it)) }
                }
                MeasurementsPanel { measurements ->
                    val mapped: Map<String, Float> = measurements.toControlValues().entries
                        .flatMap { entry ->
                            val control = controls.firstOrNull { it.key == entry.key }
                            if (control != null) MorphMapping.weights(control, entry.value).entries
                            else emptySet<Map.Entry<String, Float>>()
                        }
                        .associate { it.key to it.value }
                    viewModel.setWeights(mapped)
                }
            }
            TextButton(
                onClick = viewModel::reset,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
            ) {
                Text(text = "Reset", color = Color.White)
            }
        }
    }
}

@Composable
private fun MorphSlider(control: MorphControl, value: Float, onChange: (Float) -> Unit) {
    Text(
        text = control.label,
        color = Color.White,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp),
    )
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 0f..1f,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@Composable
private fun MeasurementsPanel(onApply: (Measurements) -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    val fields = listOf("Height (cm)", "Weight (kg)", "Chest (cm)", "Waist (cm)", "Hips (cm)")
    val values = remember { mutableStateOf(List(5) { "" }) }

    TextButton(onClick = { expanded.value = !expanded.value }, modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = if (expanded.value) "Hide measurements" else "Enter measurements", color = Color.White)
    }
    if (expanded.value) {
        fields.forEachIndexed { index, label ->
            OutlinedTextField(
                value = values.value[index],
                onValueChange = { input ->
                    values.value = values.value.toMutableList().also { it[index] = input }
                },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        Button(
            onClick = {
                val numbers = values.value.map { it.toFloatOrNull() }
                if (numbers.all { it != null }) {
                    onApply(
                        Measurements(
                            heightCm = numbers[0]!!, weightKg = numbers[1]!!,
                            chestCm = numbers[2]!!, waistCm = numbers[3]!!, hipsCm = numbers[4]!!,
                        ),
                    )
                }
            },
            modifier = Modifier.padding(start = 24.dp, top = 8.dp),
        ) {
            Text("Apply measurements")
        }
        Text(
            text = "Approximate: height, weight and proportions are mapped to body shape. Refine with the sliders above.",
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}
