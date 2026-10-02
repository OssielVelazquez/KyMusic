package com.kyoten.kymusic.player.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyoten.kymusic.R
import com.kyoten.kymusic.data.model.Song
import com.kyoten.kymusic.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    song: Song,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onShowEqualizer: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val audioData by viewModel.audioData.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackQueue by viewModel.playbackQueue.collectAsState()

    var showQueue by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Colores dinámicos
    val placeholderColor = Color(song.placeholderColor)
    var dominantColor1 by remember { mutableStateOf(placeholderColor.copy(alpha = 0.3f)) }
    var dominantColor2 by remember { mutableStateOf(Color(0xFF0A0A0F)) }

    LaunchedEffect(song.albumArtBytes) {
        song.albumArtBytes?.let { bytes ->
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap?.let {
                    val pixel = it.getPixel(it.width / 2, it.height / 2)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    dominantColor1 = Color(r, g, b).copy(alpha = 0.4f)
                    dominantColor2 = Color(r / 2, g / 2, b / 2).copy(alpha = 0.9f)
                }
            } catch (e: Exception) { }
        }
    }

    val hasRealArt = song.albumArtBytes != null && song.albumArtBytes.isNotEmpty()

    val imageModel: ImageRequest? = remember(song.albumArtBytes, song.albumArtUri) {
        if (hasRealArt) {
            ImageRequest.Builder(context)
                .data(song.albumArtBytes)
                .crossfade(500)
                .build()
        } else if (song.albumArtUri != null) {
            ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .crossfade(500)
                .build()
        } else {
            null
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            dominantColor1,
            dominantColor2,
            Color(0xFF0A0A0F),
            Color(0xFF000000)
        )
    )

    LaunchedEffect(song.id) {
        scale = 0.9f
        delay(100)
        scale = 1f
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotation += 0.4f
            if (rotation >= 360f) rotation = 0f
            delay(16)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0F),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp,
                color = Color.Transparent
            ) {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    actions = {
                        // Botón de Cola movido al TopAppBar
                        IconButton(onClick = { showQueue = true }) {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                contentDescription = "Cola",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = onShowEqualizer) {
                            Icon(
                                Icons.Rounded.Equalizer,
                                contentDescription = "Ecualizador",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
        ) {
            if (showQueue) {
                QueueScreen(
                    viewModel = viewModel,
                    queue = playbackQueue,
                    onBack = { showQueue = false },
                    onPlaySong = { songToPlay ->
                        val newIndex = playbackQueue.indexOf(songToPlay)
                        if (newIndex != -1) {
                            viewModel.playSongAtIndex(newIndex)
                        }
                        showQueue = false
                    },
                    onRemoveFromQueue = { songToRemove ->
                        viewModel.removeFromQueue(songToRemove)
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        "REPRODUCIENDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )

                    // Album Art
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(260.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        Card(
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(if (isPlaying) rotation else 0f),
                            colors = CardDefaults.cardColors(
                                containerColor = placeholderColor.copy(alpha = 0.2f)
                            )
                        ) {
                            if (hasRealArt && imageModel != null) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholder = painterResource(R.drawable.music_placeholder),
                                    error = painterResource(R.drawable.music_placeholder)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(CircleShape)
                                            .background(placeholderColor.copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = song.placeholderInitials,
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (isPlaying) {
                            CircularVisualizer(
                                audioData = audioData,
                                modifier = Modifier.size(220.dp),
                                primaryColor = Color.White,
                                isPlaying = isPlaying
                            )
                        }
                    }

                    // Información de la canción
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = song.displayArtist,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.displayAlbum,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Barra de progreso y controles
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime(currentPosition),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    formatTime(duration),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Fila de controles centrada SIN el botón de cola (ahora está en el TopAppBar)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(
                                onClick = { viewModel.toggleShuffle() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = "Aleatorio",
                                    tint = if (shuffleEnabled) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Anterior
                            IconButton(
                                onClick = { viewModel.playPreviousSong() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Anterior",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }

                            // Play/Pause
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(16.dp, CircleShape)
                            ) {
                                FilledIconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier.fillMaxSize(),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.White
                                    ),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                        modifier = Modifier.size(36.dp),
                                        tint = Color(0xFF1A1A2E)
                                    )
                                }
                            }

                            // Siguiente
                            IconButton(
                                onClick = { viewModel.playNextSong() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Siguiente",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }

                            // Repeat
                            IconButton(
                                onClick = {
                                    val newMode = when (repeatMode) {
                                        MusicViewModel.RepeatMode.NONE -> MusicViewModel.RepeatMode.ALL
                                        MusicViewModel.RepeatMode.ALL -> MusicViewModel.RepeatMode.ONE
                                        MusicViewModel.RepeatMode.ONE -> MusicViewModel.RepeatMode.NONE
                                    }
                                    viewModel.setRepeatMode(newMode)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                val icon = when (repeatMode) {
                                    MusicViewModel.RepeatMode.NONE -> Icons.Rounded.Repeat
                                    MusicViewModel.RepeatMode.ALL -> Icons.Rounded.Repeat
                                    MusicViewModel.RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                }
                                Icon(
                                    icon,
                                    contentDescription = "Repetir",
                                    tint = if (repeatMode != MusicViewModel.RepeatMode.NONE) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun CircularVisualizer(
    audioData: ByteArray?,
    modifier: Modifier,
    primaryColor: Color,
    isPlaying: Boolean
) {
    var phase by remember { mutableFloatStateOf(0f) }
    val barHeights = remember { mutableStateListOf<Float>() }
    val barCount = 32

    LaunchedEffect(Unit) {
        if (barHeights.size != barCount) {
            barHeights.clear()
            repeat(barCount) { barHeights.add(0f) }
        }
    }

    LaunchedEffect(isPlaying) {
        while (true) {
            phase += 0.04f
            if (phase > 2 * PI.toFloat()) phase = 0f

            for (i in 0 until barCount) {
                val target = if (isPlaying && audioData != null && audioData.isNotEmpty()) {
                    val dataIndex = (i * audioData.size / barCount).coerceIn(0, audioData.size - 1)
                    (audioData[dataIndex].toFloat() / 128f).coerceIn(0.1f, 1.2f)
                } else {
                    (sin(phase * 2f + i * 0.3f) * 0.2f + 0.4f) * (if (isPlaying) 1f else 0.3f)
                }

                if (i < barHeights.size) {
                    barHeights[i] = barHeights[i] + (target - barHeights[i]) * 0.15f
                }
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.4f

        for (i in 0 until min(barCount, barHeights.size)) {
            val angle = (2 * PI * i / barCount).toFloat()
            val barHeight = radius * barHeights[i] * 0.5f

            val start = Offset(
                center.x + (radius - 10) * cos(angle),
                center.y + (radius - 10) * sin(angle)
            )
            val end = Offset(
                center.x + (radius + barHeight) * cos(angle),
                center.y + (radius + barHeight) * sin(angle)
            )
            val alpha = (barHeights[i] * 0.7f + 0.3f).coerceIn(0.3f, 1f)
            val color = primaryColor.copy(alpha = alpha)

            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = 3f * barHeights[i]
            )
        }

        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = radius * 0.7f,
            center = center
        )
    }
}