package com.kyoten.kymusic.player.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun AudioVisualizer(
    audioData: ByteArray?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barWidth: Float = 3f,
    barSpacing: Float = 2f,
    colors: List<Color> = listOf(
        Color(0xFF00E5FF),
        Color(0xFF00BCD4),
        Color(0xFF009688),
        Color(0xFF4CAF50)
    )
) {
    var phase by remember { mutableFloatStateOf(0f) }
    val barHeights = remember { mutableStateListOf<Float>() }
    val targetHeights = remember { mutableStateListOf<Float>() }

    // Animación suave para los colores
    val colorTransition = rememberInfiniteTransition()
    val colorShift by colorTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(barCount) {
        if (barHeights.size != barCount) {
            barHeights.clear()
            targetHeights.clear()
            repeat(barCount) {
                barHeights.add(0f)
                targetHeights.add(0f)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.05f
            if (phase > 2 * PI.toFloat()) phase = 0f

            if (isPlaying && audioData != null && audioData.isNotEmpty()) {
                // Usar datos reales del audio
                val step = max(1, audioData.size / (barCount * 2))
                for (i in 0 until barCount) {
                    val dataIndex = (i * step * 2).coerceIn(0, audioData.size - 2)
                    val byte1 = audioData[dataIndex].toInt() and 0xFF
                    val byte2 = audioData[dataIndex + 1].toInt() and 0xFF
                    val amplitude = ((byte1 + byte2) / 2) / 255f
                    // Mejor respuesta a frecuencias altas
                    val freqBoost = 1f + (i.toFloat() / barCount) * 0.5f
                    targetHeights[i] = (amplitude * freqBoost).coerceIn(0.1f, 1.2f)
                }
            } else {
                // Efecto de onda cuando está pausado o sin datos
                for (i in 0 until barCount) {
                    val wave = if (isPlaying) {
                        // Ondas vivas cuando suena
                        (sin(phase * 3f + i * 0.5f) * 0.25f + 0.45f) * 0.9f + 0.1f
                    } else {
                        // Latido suave cuando está pausado
                        (sin(phase * 1.2f + i * 0.15f) * 0.08f + 0.12f) * 0.8f
                    }
                    targetHeights[i] = wave.coerceIn(0.05f, 0.5f)
                }
            }

            // Interpolación suave
            for (i in 0 until barCount) {
                barHeights[i] = barHeights[i] + (targetHeights[i] - barHeights[i]) * 0.2f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalWidth = barCount * (barWidth + barSpacing) - barSpacing
        var startX = (canvasWidth - totalWidth) / 2f

        for (i in 0 until barCount) {
            if (i < barHeights.size) {
                val barHeight = barHeights[i] * canvasHeight * 0.85f
                val minHeight = 2f
                val finalHeight = max(barHeight, minHeight)

                // Color degradado basado en altura y tiempo
                val hueShift = (colorShift * 360 + i * 5).toInt() % 360
                val saturation = 0.8f
                val lightness = 0.5f + barHeights[i] * 0.3f

                val color = when {
                    barHeights[i] > 0.8f -> Color(0xFF00E5FF)      // Cyan brillante
                    barHeights[i] > 0.5f -> Color(0xFF00BCD4)      // Cyan medio
                    barHeights[i] > 0.3f -> Color(0xFF009688)      // Verde azulado
                    else -> Color(0xFF006064)                       // Cyan oscuro
                }

                // Efecto de brillo en la parte superior de la barra
                val glowColor = color.copy(alpha = 0.8f)
                val barEndY = canvasHeight - finalHeight

                // Dibujar barra con degradado vertical
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x = startX, y = barEndY),
                    size = Size(width = barWidth, height = finalHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                )

                // Añadir un pequeño punto brillante en la parte superior cuando está alto
                if (barHeights[i] > 0.6f && isPlaying) {
                    drawCircle(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                        radius = barWidth / 2.5f,
                        center = Offset(x = startX + barWidth / 2, y = barEndY)
                    )
                }

                startX += barWidth + barSpacing
            }
        }

        // Línea de base sutil
        val baselineY = canvasHeight - 2f
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
            start = Offset(x = 0f, y = baselineY),
            end = Offset(x = canvasWidth, y = baselineY),
            strokeWidth = 1f
        )
    }
}

// Versión más pequeña para el mini player
@Composable
fun MiniAudioVisualizer(
    audioData: ByteArray?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    color: Color = Color(0xFF00E5FF)
) {
    var phase by remember { mutableFloatStateOf(0f) }
    val barHeights = remember { mutableStateListOf<Float>() }
    val targetHeights = remember { mutableStateListOf<Float>() }

    LaunchedEffect(barCount) {
        if (barHeights.size != barCount) {
            barHeights.clear()
            targetHeights.clear()
            repeat(barCount) {
                barHeights.add(0f)
                targetHeights.add(0f)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.08f
            if (phase > 2 * PI.toFloat()) phase = 0f

            if (isPlaying && audioData != null && audioData.isNotEmpty()) {
                val step = max(1, audioData.size / (barCount * 2))
                for (i in 0 until barCount) {
                    val dataIndex = (i * step * 2).coerceIn(0, audioData.size - 2)
                    val byte1 = audioData[dataIndex].toInt() and 0xFF
                    val byte2 = audioData[dataIndex + 1].toInt() and 0xFF
                    val amplitude = ((byte1 + byte2) / 2) / 255f
                    targetHeights[i] = amplitude.coerceIn(0.05f, 1f)
                }
            } else {
                for (i in 0 until barCount) {
                    val wave = if (isPlaying) {
                        (sin(phase * 3f + i * 0.4f) * 0.2f + 0.3f) * 0.8f + 0.1f
                    } else {
                        (sin(phase * 1.2f + i * 0.15f) * 0.06f + 0.1f) * 0.6f
                    }
                    targetHeights[i] = wave.coerceIn(0.03f, 0.3f)
                }
            }

            for (i in 0 until barCount) {
                barHeights[i] = barHeights[i] + (targetHeights[i] - barHeights[i]) * 0.3f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = (canvasWidth / barCount) - 2f
        val barSpacing = 2f

        for (i in 0 until barCount) {
            if (i < barHeights.size) {
                val barHeight = barHeights[i] * canvasHeight * 0.85f
                val x = i * (barWidth + barSpacing)

                drawRoundRect(
                    color = color.copy(alpha = 0.5f + barHeights[i] * 0.5f),
                    topLeft = Offset(x = x, y = canvasHeight - barHeight),
                    size = Size(width = barWidth, height = max(barHeight, 2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                )
            }
        }
    }
}