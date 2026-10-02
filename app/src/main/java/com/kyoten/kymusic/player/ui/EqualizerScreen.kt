@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.kyoten.kymusic.player.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.kyoten.kymusic.audio.EqualizerEngine
import com.kyoten.kymusic.viewmodel.MusicViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {

    val equalizerReady by viewModel.equalizerReady.collectAsState()
    val isEnabled by viewModel.equalizerEnabled.collectAsState()
    val bands by viewModel.equalizerBands.collectAsState()
    val numberOfBands by viewModel.numberOfBands.collectAsState()
    val preamp by viewModel.preamp.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val virtualizer by viewModel.virtualizer.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showBandInfo by remember { mutableStateOf(false) }

    val equalizer = viewModel.equalizerEngine?.equalizer
    val activeBands = numberOfBands

    val bandLabels = remember(equalizer, activeBands) {
        if (equalizer != null && activeBands > 0) {
            (0 until activeBands).map { i ->
                val freqHz = equalizer.getCenterFreq(i.toShort()) / 1000f
                when {
                    freqHz >= 1000 -> String.format("%.1fk", freqHz / 1000f)
                    else -> String.format("%.0f", freqHz)
                }
            }
        } else {
            emptyList()
        }
    }

    val bandsList = remember(bands, activeBands) {
        bands.toSortedMap().values.toList().take(activeBands)
    }

    val levelRange = viewModel.getBandLevelRange()
    val rangeMin = levelRange.first / 100f
    val rangeMax = levelRange.second / 100f

    fun mBtoDB(mB: Int): String = String.format("%+.1f", mB / 100.0)

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp,
                color = colorScheme.background
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "ECUALIZADOR",
                                color = colorScheme.primary,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeBands > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable { showBandInfo = true }
                                ) {
                                    Text(
                                        "${activeBands}",
                                        color = colorScheme.primary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background),
                    actions = {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.primary,
                                checkedTrackColor = colorScheme.primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = colorScheme.surfaceVariant
                            )
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            if (equalizerReady && activeBands > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Presets Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { showPresetDialog = true }
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "PRESET",
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    currentPreset,
                                    color = colorScheme.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Rounded.ArrowForward,
                                null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Preamp, Bass Boost, Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EqControlCard(
                            title = "PRE",
                            value = preamp.toFloat(),
                            valueText = mBtoDB(preamp),
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f),
                            onValueChange = { viewModel.setPreamp(it.toInt()) }
                        )
                        EqControlCard(
                            title = "BASS",
                            value = bassBoost.toFloat(),
                            valueText = "${bassBoost / 10}%",
                            color = colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onValueChange = { viewModel.setBassBoost(it.toInt()) }
                        )
                        EqControlCard(
                            title = "VIRT",
                            value = virtualizer.toFloat(),
                            valueText = "${virtualizer / 10}%",
                            color = colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onValueChange = { viewModel.setVirtualizer(it.toInt()) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "ECUALIZADOR GRÁFICO (${activeBands} BANDAS)",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isEnabled) {
                            Text(
                                "ACTIVO",
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ==================== BARRAS DEL ECUALIZADOR ====================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val barWidth = when {
                            activeBands <= 5 -> 42.dp
                            activeBands <= 8 -> 32.dp
                            else -> 26.dp
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(280.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            bandLabels.forEachIndexed { index, label ->
                                val levelMB = if (index < bandsList.size) bandsList[index] else 0
                                val levelDB = (levelMB / 100.0).toFloat()
                                val percent = ((levelDB - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)

                                EqualizerBar(
                                    label = label,
                                    value = levelDB,
                                    percent = percent,
                                    barWidth = barWidth,
                                    rangeMin = rangeMin,
                                    rangeMax = rangeMax,
                                    isEnabled = isEnabled,
                                    onValueChange = { newPercent ->
                                        val newValue = rangeMin + (rangeMax - rangeMin) * newPercent
                                        val newValueMB = (newValue * 100).toInt()
                                        viewModel.setEqualizerBandLevel(index.toShort(), newValueMB.toShort())
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.resetEqualizer() },
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(4.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.surface,
                            contentColor = colorScheme.primary,
                            disabledContainerColor = colorScheme.surfaceVariant,
                            disabledContentColor = colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("RESTABLECER", color = colorScheme.primary, fontSize = 14.sp, letterSpacing = 1.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            } else if (equalizerReady && activeBands == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Equalizer,
                            null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Ecualizador no disponible",
                            color = colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                        Text(
                            "Tu dispositivo no soporta esta función",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Inicializando ecualizador...",
                            color = colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo de información de bandas
    if (showBandInfo) {
        AlertDialog(
            onDismissRequest = { showBandInfo = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.primary,
            textContentColor = colorScheme.onSurface,
            title = { Text("Bandas del ecualizador", color = colorScheme.primary) },
            text = {
                Column {
                    Text("Tu dispositivo soporta ${activeBands} bandas:", color = colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    bandLabels.forEachIndexed { index, label ->
                        Text(
                            "• Banda ${index + 1}: ${label} Hz",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Los presets se adaptan automáticamente al número de bandas.",
                        color = colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBandInfo = false }) {
                    Text("Cerrar", color = colorScheme.primary)
                }
            }
        )
    }

    // Diálogo de presets
    if (showPresetDialog) {
        val dialogContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.primary,
            textContentColor = colorScheme.onSurface,
            title = { Text("Presets", color = colorScheme.primary) },
            text = {
                Column {
                    EqualizerEngine.Preset.values().forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.applyPreset(preset)
                                    showPresetDialog = false
                                    if (preset.gains.size > activeBands) {
                                        Toast.makeText(
                                            dialogContext,
                                            "Preset adaptado a las ${activeBands} bandas de tu dispositivo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            color = if (currentPreset == preset.presetName)
                                colorScheme.primary.copy(alpha = 0.15f)
                            else
                                Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        preset.presetName,
                                        color = if (currentPreset == preset.presetName)
                                            colorScheme.primary
                                        else
                                            colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    if (preset.gains.size > activeBands) {
                                        Text(
                                            "Adaptado a ${activeBands} bandas",
                                            color = colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                if (currentPreset == preset.presetName) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu dispositivo tiene ${activeBands} bandas. Los presets se adaptan automáticamente.",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Cerrar", color = colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun EqControlCard(
    title: String,
    value: Float,
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(value) {
        sliderValue = value
    }

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                valueText,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onValueChange(it)
                },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = colorScheme.primary,
                    activeTrackColor = colorScheme.primary,
                    inactiveTrackColor = colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun EqualizerBar(
    label: String,
    value: Float,
    percent: Float,
    barWidth: Dp,
    rangeMin: Float,
    rangeMax: Float,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    var dragPercent by remember { mutableFloatStateOf(percent) }
    var isDragging by remember { mutableStateOf(false) }
    var barHeightPx by remember { mutableStateOf(0f) }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(percent) {
        if (!isDragging) {
            dragPercent = percent
        }
    }

    val currentValue = rangeMin + (rangeMax - rangeMin) * dragPercent
    val barTotalHeight = 160f
    val centerY = barTotalHeight / 2

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(barWidth + 20.dp)
    ) {
        Text(
            String.format("%+.1f", currentValue),
            color = when {
                currentValue > 0 -> Color(0xFF4CAF50)
                currentValue < 0 -> Color(0xFFF44336)
                else -> colorScheme.onSurfaceVariant
            },
            fontSize = 10.sp,
            modifier = Modifier.height(18.dp)
        )

        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barTotalHeight.dp)
                .onSizeChanged { barHeightPx = it.height.toFloat() }
                .clip(RoundedCornerShape((barWidth / 2).value.dp))
                .background(colorScheme.surface)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput

                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            isDragging = true

                            val initialY = down.position.y.coerceIn(0f, barHeightPx)
                            var newPercent = (1f - (initialY / barHeightPx)).coerceIn(0f, 1f)
                            dragPercent = newPercent
                            onValueChange(newPercent)

                            do {
                                val event = awaitPointerEvent()
                                val touchY = event.changes.firstOrNull()?.position?.y ?: continue
                                val clampedY = touchY.coerceIn(0f, barHeightPx)
                                newPercent = (1f - (clampedY / barHeightPx)).coerceIn(0f, 1f)
                                dragPercent = newPercent
                                onValueChange(newPercent)
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })

                            isDragging = false
                        }
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().background(colorScheme.background))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(centerY.dp)
                    .align(Alignment.TopCenter)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(centerY.dp)
                    .align(Alignment.BottomCenter)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(colorScheme.primary.copy(alpha = 0.8f))
            )

            val currentY = barTotalHeight * (1f - dragPercent)

            if (dragPercent > 0.5f) {
                val barHeight = centerY - currentY
                if (barHeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight.dp)
                            .offset(y = currentY.dp)
                            .background(Color(0xFF4CAF50))
                    )
                }
            } else if (dragPercent < 0.5f) {
                val barHeight = currentY - centerY
                if (barHeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight.dp)
                            .offset(y = centerY.dp)
                            .background(Color(0xFFF44336))
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(label, color = colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}