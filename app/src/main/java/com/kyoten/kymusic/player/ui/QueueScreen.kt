package com.kyoten.kymusic.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyoten.kymusic.R
import com.kyoten.kymusic.data.model.Song
import com.kyoten.kymusic.viewmodel.MusicViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: MusicViewModel,
    queue: List<Song>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit
) {
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

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
                        Text(
                            "Cola de reproducción",
                            color = colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.clearQueue() }) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                "Limpiar cola",
                                tint = colorScheme.error
                            )
                        }
                        if (shuffleEnabled) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Aleatorio activado",
                                tint = colorScheme.primary
                            )
                        }
                        when (repeatMode) {
                            MusicViewModel.RepeatMode.ONE -> Icon(
                                Icons.Rounded.RepeatOne,
                                null,
                                tint = colorScheme.primary
                            )
                            MusicViewModel.RepeatMode.ALL -> Icon(
                                Icons.Rounded.Repeat,
                                null,
                                tint = colorScheme.primary
                            )
                            else -> {}
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
        ) {
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay canciones en la cola",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                        Text(
                            "Agrega canciones desde el menú contextual",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        queue,
                        key = { index, item -> "${item.id}_$index" }
                    ) { index, song ->
                        QueueItem(
                            song = song,
                            index = index + 1,
                            isCurrent = currentSong?.id == song.id,
                            onPlay = { onPlaySong(song) },
                            onRemove = { onRemoveFromQueue(song) },
                            onMoveUp = if (index > 0) {
                                { viewModel.moveInQueue(index, index - 1) }
                            } else null,
                            onMoveDown = if (index < queue.size - 1) {
                                { viewModel.moveInQueue(index, index + 1) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItem(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val placeholderColor = Color(song.placeholderColor)
    val colorScheme = MaterialTheme.colorScheme

    val imageModel: ImageRequest? = remember(song.albumArtBytes, song.albumArtUri) {
        if (song.albumArtBytes != null && song.albumArtBytes.isNotEmpty()) {
            ImageRequest.Builder(context)
                .data(song.albumArtBytes)
                .crossfade(true)
                .build()
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                colorScheme.primary.copy(alpha = 0.15f)
            else
                colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Reproduciendo ahora",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = "$index",
                    color = colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp)
                )
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = painterResource(R.drawable.music_placeholder),
                    error = painterResource(R.drawable.music_placeholder)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(placeholderColor.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        song.placeholderInitials,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Text(
                    text = song.displayArtist,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp) {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        "Mover arriba",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        "Mover abajo",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Quitar de cola",
                    tint = colorScheme.error
                )
            }
        }
    }
}