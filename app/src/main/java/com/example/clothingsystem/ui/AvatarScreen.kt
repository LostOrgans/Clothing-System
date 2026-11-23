package com.example.clothingsystem.ui

import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clothingsystem.R
import com.example.clothingsystem.avatar.AppearanceColor
import com.example.clothingsystem.avatar.AppearanceColors
import com.example.clothingsystem.avatar.AvatarModel
import com.example.clothingsystem.avatar.AvatarRenderer
import com.example.clothingsystem.avatar.AvatarViewModel
import com.example.clothingsystem.avatar.Measurements
import com.example.clothingsystem.avatar.MorphMapping
import com.example.clothingsystem.avatar.SkinTone
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
    val viewerRef = remember { mutableStateOf<ModelViewer?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF20242B))) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SurfaceView(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val viewer = ModelViewer(this)
                        viewerRef.value = viewer
                        AvatarRenderer.configure(viewer)
                        model.value = AvatarRenderer.loadModel(viewer, context)
                        setOnTouchListener(viewer)
                        val choreographer = Choreographer.getInstance()
                        val callback = object : Choreographer.FrameCallback {
                            override fun doFrame(frameTimeNanos: Long) {
                                choreographer.postFrameCallback(this)
                                model.value?.let {
                                    AvatarRenderer.applyWeights(viewer, it, latestConfig.value)
                                    AvatarRenderer.applySkinTone(viewer, latestConfig.value.skinTone)
                                    AvatarRenderer.applyAppearance(viewer, latestConfig.value)
                                }
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
            TextButton(
                onClick = {
                    viewerRef.value?.let { viewer ->
                        viewer.resetToDefaultState()
                        viewer.cameraFocalLength = 75f
                    }
                },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Text("Reset view", color = Color.White)
            }
            Text(
                text = stringResource(R.string.avatar_hint),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Skin tone", color = Color.White, modifier = Modifier.padding(start = 24.dp, top = 8.dp))
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                for (tone in SkinTone.entries) {
                    Button(
                        onClick = { viewModel.setSkinTone(tone) },
                        modifier = Modifier.size(48.dp).padding(end = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(tone.hex)),
                    ) {}
                }
            }
            ColourRow("Hair", AppearanceColors.HAIR, config.hairColor, viewModel::setHairColor)
            ColourRow("Eyes", AppearanceColors.EYE, config.eyeColor, viewModel::setEyeColor)
            ColourRow("Nipples", AppearanceColors.NIPPLE, config.nippleColor, viewModel::setNippleColor)
            MeasurementsForm { measurements ->
                val current = model.value ?: return@MeasurementsForm
                val defaults = current.morphNames.zip(current.defaultWeights.toTypedArray()).toMap()
                val controls = MorphMapping.controls(current.morphNames)
                val mapped: Map<String, Float> = measurements.toControlValues().entries
                    .flatMap { entry ->
                        val control = controls.firstOrNull { it.key == entry.key }
                        if (control != null) MorphMapping.weights(control, entry.value).entries
                        else emptySet<Map.Entry<String, Float>>()
                    }
                    .associate { it.key to it.value }
                viewModel.setWeights(defaults + mapped)
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
private fun ColourRow(
    label: String,
    options: List<AppearanceColor>,
    selected: AppearanceColor,
    onSelect: (AppearanceColor) -> Unit,
) {
    Text(label, color = Color.White, modifier = Modifier.padding(start = 24.dp, top = 8.dp))
    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        for (option in options) {
            Button(
                onClick = { onSelect(option) },
                modifier = Modifier.size(48.dp).padding(end = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(option.hex)),
            ) {}
        }
    }
}

@Composable
private fun MeasurementsForm(onApply: (Measurements) -> Unit) {
    val values = remember { mutableStateOf(Measurements.FIELDS.map { it.get(Measurements()).toString() }) }

    Text(
        text = "Body measurements",
        color = Color.White,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp),
    )
    Measurements.FIELDS.forEachIndexed { index, field ->
        OutlinedTextField(
            value = values.value[index],
            onValueChange = { input ->
                values.value = values.value.toMutableList().also { it[index] = input }
            },
            label = { Text("${field.label} (${field.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 3.dp),
        )
    }
    Button(
        onClick = {
            val numbers = values.value.map { it.toFloatOrNull() }
            if (numbers.all { it != null }) {
                onApply(
                    Measurements(
                        heightCm = numbers[0]!!, weightKg = numbers[1]!!, chestCm = numbers[2]!!,
                        waistCm = numbers[3]!!, hipsCm = numbers[4]!!, shoulderCm = numbers[5]!!,
                        ageYears = numbers[6]!!, muscleTone = numbers[7]!!, firmness = numbers[8]!!,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 8.dp, end = 24.dp),
    ) {
        Text("Apply measurements")
    }
    Text(
        text = "Approximate mapping to body shape. Edit any value and apply again.",
        color = Color.White.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}
