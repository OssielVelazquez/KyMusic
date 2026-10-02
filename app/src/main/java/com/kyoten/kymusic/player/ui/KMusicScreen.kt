@file:OptIn(ExperimentalMaterial3Api::class)

package com.kyoten.kymusic.player.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyoten.kymusic.R
import com.kyoten.kymusic.data.model.Playlist
import com.kyoten.kymusic.data.model.Song
import com.kyoten.kymusic.ui.theme.AppTheme
import com.kyoten.kymusic.ui.theme.CustomThemeColors
import com.kyoten.kymusic.viewmodel.MusicViewModel
import com.kyoten.kymusic.viewmodel.MusicViewModelHolder
import com.kyoten.kymusic.viewmodel.SortType
import kotlin.collections.forEach

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KMusicScreen(viewModel: MusicViewModel) {

    val context = LocalContext.current
    val view = LocalView.current
    var dominantColors by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    DisposableEffect(Unit) {
        MusicViewModelHolder.viewModel = viewModel
        onDispose {
            MusicViewModelHolder.viewModel = null
        }
    }

    val allSongs by viewModel.allSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentPlaylistSongsState by viewModel.currentPlaylistSongs.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val customColors by viewModel.customThemeColors.collectAsState()

    val filteredSongs by viewModel.filteredSongs.collectAsState()

    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSongForMenu by rememberSaveable { mutableStateOf<Song?>(null) }
    var isMenuFromPlaylist by rememberSaveable { mutableStateOf(false) }
    var currentPlaylistForMenu by rememberSaveable { mutableStateOf<Playlist?>(null) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var showAddToPlaylistDialog by rememberSaveable { mutableStateOf<Song?>(null) }
    var pendingSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    // ✅ SELECCIÓN MÚLTIPLE
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedSongs by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }

    // ✅ NUEVO: Selector de colores
    var showFullColorPicker by rememberSaveable { mutableStateOf(false) }

    // ✅ NUEVO: Diálogo para agregar múltiples a playlist
    var showAddMultipleDialog by rememberSaveable { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val animatedBars by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    BackHandler(enabled = showPlayer || showEqualizer) {
        when {
            showEqualizer -> showEqualizer = false
            showPlayer -> showPlayer = false
        }
    }

    // ==================== FONDO DINÁMICO CON TEMA ====================
    val backgroundColor = MaterialTheme.colorScheme.background
    val dynamicGradient = if (dominantColors != null && currentSong != null) {
        Brush.verticalGradient(
            colors = listOf(
                Color(dominantColors!!.first, dominantColors!!.second, 50),
                backgroundColor
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.95f),
                backgroundColor.copy(alpha = 0.9f)
            )
        )
    }

    LaunchedEffect(currentSong) {
        if (currentSong?.albumArtBytes != null) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(
                    currentSong?.albumArtBytes, 0, currentSong?.albumArtBytes?.size ?: 0
                )
                bitmap?.let {
                    val pixel = it.getPixel(it.width / 2, it.height / 2)
                    dominantColors = Pair(
                        android.graphics.Color.red(pixel),
                        android.graphics.Color.green(pixel)
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp,
                color = Color.Transparent
            ) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Buscar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    ),
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Buscar canciones...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 16.sp
                                            )
                                        }
                                        inner()
                                    }
                                )
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Limpiar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Cerrar búsqueda",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Text(
                                "KyMusic",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Buscar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.refreshSongs() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Refrescar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showCreatePlaylistDialog = true }) {
                            Icon(
                                Icons.Rounded.PlaylistAdd,
                                contentDescription = "Nueva lista",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Ordenar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // ✅ NUEVO: Botón para personalizar colores
                        IconButton(onClick = { showFullColorPicker = true }) {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = "Personalizar colores",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // ✅ BOTÓN DE SELECCIÓN MÚLTIPLE
                        if (!showPlayer && !showEqualizer) {
                            IconButton(onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) {
                                    selectedSongs = emptySet()
                                }
                            }) {
                                Icon(
                                    if (selectionMode) Icons.Rounded.CheckCircle else Icons.Rounded.SelectAll,
                                    contentDescription = if (selectionMode) "Salir de selección" else "Seleccionar múltiples",
                                    tint = if (selectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // ✅ BOTONES EN MODO SELECCIÓN
                        if (selectionMode && selectedSongs.isNotEmpty() && !showPlayer && !showEqualizer) {

                            // 1️⃣ AGREGAR A PLAYLIST
                            IconButton(onClick = {
                                if (playlists.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "No hay playlists. Crea una primero.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    showAddMultipleDialog = true
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.PlaylistAdd,
                                    contentDescription = "Agregar a playlist",
                                    tint = Color(0xFF4CAF50)
                                )
                            }

                            // 2️⃣ COMPARTIR
                            IconButton(onClick = {
                                viewModel.shareMultipleSongs(selectedSongs, context) {
                                    selectedSongs = emptySet()
                                    selectionMode = false
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = "Compartir",
                                    tint = Color(0xFF00E5FF)
                                )
                            }

                            // 3️⃣ AGREGAR A COLA
                            IconButton(onClick = {
                                viewModel.addMultipleToQueue(selectedSongs) {
                                    selectedSongs = emptySet()
                                    selectionMode = false
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.QueueMusic,
                                    contentDescription = "Agregar a cola",
                                    tint = Color(0xFFFF9800)
                                )
                            }

                            // 4️⃣ REPRODUCIR DESPUÉS
                            IconButton(onClick = {
                                viewModel.addMultipleToQueueNext(selectedSongs) {
                                    selectedSongs = emptySet()
                                    selectionMode = false
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.PlaylistPlay,
                                    contentDescription = "Reproducir después",
                                    tint = Color(0xFF9C27B0)
                                )
                            }

                            // 5️⃣ ELIMINAR
                            IconButton(onClick = {
                                val songsToDelete = selectedSongs
                                viewModel.deleteMultipleSongs(songsToDelete, context) {
                                    selectedSongs = emptySet()
                                    selectionMode = false
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Eliminar seleccionadas",
                                    tint = Color(0xFFCF6679)
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicGradient)
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                val tabTitles = buildList {
                    add("Todas")
                    addAll(playlists.map { it.name })
                }

                val validSelectedTab = if (selectedTab >= tabTitles.size) 0 else selectedTab

                if (tabTitles.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = validSelectedTab,
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[validSelectedTab]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabTitles.forEachIndexed { i, title ->
                            Tab(
                                selected = validSelectedTab == i,
                                onClick = {
                                    selectedTab = i
                                    if (i == 0) {
                                        viewModel.selectPlaylist(null)
                                    } else {
                                        val playlistIndex = i - 1
                                        if (playlistIndex in playlists.indices) {
                                            viewModel.selectPlaylist(playlists[playlistIndex].id)
                                        }
                                    }
                                    selectionMode = false
                                    selectedSongs = emptySet()
                                },
                                text = {
                                    Text(
                                        title,
                                        color = if (validSelectedTab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = if (validSelectedTab == i) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                when {
                    isLoading -> LoadingIndicator()
                    error != null -> ErrorMessage(error = error!!)
                    else -> {
                        val isAllSongsTab = validSelectedTab == 0
                        val isPlaylistTab = !isAllSongsTab && (validSelectedTab - 1) in playlists.indices

                        if (isAllSongsTab) {
                            val displayedSongs = if (searchQuery.isBlank()) {
                                filteredSongs
                            } else {
                                filteredSongs.filter {
                                    it.title.contains(searchQuery, true) ||
                                            (it.artist?.contains(searchQuery, true) == true) ||
                                            it.album.contains(searchQuery, true)
                                }
                            }
                            val isSearchEmpty = searchQuery.isNotBlank() && displayedSongs.isEmpty()

                            SongList(
                                songs = displayedSongs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                animatedBars = animatedBars,
                                isEmptyBySearch = isSearchEmpty,
                                isSelectionMode = selectionMode,
                                selectedSongs = selectedSongs,
                                onSongClick = { song ->
                                    if (selectionMode) {
                                        selectedSongs = if (selectedSongs.contains(song.id)) {
                                            selectedSongs - song.id
                                        } else {
                                            selectedSongs + song.id
                                        }
                                    } else {
                                        viewModel.playSong(song)
                                        showPlayer = true
                                        selectionMode = false
                                        selectedSongs = emptySet()
                                    }
                                },
                                onSongLongClick = { song ->
                                    if (selectionMode) {
                                        selectedSongs = if (selectedSongs.contains(song.id)) {
                                            selectedSongs - song.id
                                        } else {
                                            selectedSongs + song.id
                                        }
                                    } else {
                                        selectedSongForMenu = song
                                        isMenuFromPlaylist = false
                                        currentPlaylistForMenu = null
                                        showBottomSheet = true
                                    }
                                },
                                viewModel = viewModel,
                                isPlaylistView = false
                            )
                        } else if (isPlaylistTab) {
                            val playlist = playlists[validSelectedTab - 1]

                            val displayedSongs = if (searchQuery.isBlank()) {
                                currentPlaylistSongsState
                            } else {
                                currentPlaylistSongsState.filter {
                                    it.title.contains(searchQuery, true) ||
                                            (it.artist?.contains(searchQuery, true) == true) ||
                                            it.album.contains(searchQuery, true)
                                }
                            }
                            val isSearchEmpty = searchQuery.isNotBlank() && displayedSongs.isEmpty()

                            Column {
                                PlaylistHeader(
                                    playlist = playlist,
                                    onDelete = {
                                        viewModel.deletePlaylist(playlist.id)
                                        selectedTab = 0
                                        viewModel.selectPlaylist(null)
                                    },
                                    onRename = { newName ->
                                        viewModel.renamePlaylist(playlist.id, newName)
                                    }
                                )
                                SongList(
                                    songs = displayedSongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    animatedBars = animatedBars,
                                    isEmptyBySearch = isSearchEmpty,
                                    isSelectionMode = selectionMode,
                                    selectedSongs = selectedSongs,
                                    onSongClick = { song ->
                                        if (selectionMode) {
                                            selectedSongs = if (selectedSongs.contains(song.id)) {
                                                selectedSongs - song.id
                                            } else {
                                                selectedSongs + song.id
                                            }
                                        } else {
                                            viewModel.playSongFromPlaylist(song, playlist.id)
                                            showPlayer = true
                                            selectionMode = false
                                            selectedSongs = emptySet()
                                        }
                                    },
                                    onSongLongClick = { song ->
                                        if (selectionMode) {
                                            selectedSongs = if (selectedSongs.contains(song.id)) {
                                                selectedSongs - song.id
                                            } else {
                                                selectedSongs + song.id
                                            }
                                        } else {
                                            selectedSongForMenu = song
                                            isMenuFromPlaylist = true
                                            currentPlaylistForMenu = playlist
                                            showBottomSheet = true
                                        }
                                    },
                                    onRemoveFromPlaylist = { song ->
                                        viewModel.removeSongFromPlaylist(song, playlist.id)
                                        Toast.makeText(
                                            context,
                                            "Quitada de ${playlist.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    viewModel = viewModel,
                                    isPlaylistView = true
                                )
                            }
                        }
                    }
                }
            }

            // Mini player
            AnimatedVisibility(
                visible = currentSong != null && !showPlayer && !showEqualizer,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                currentSong?.let { song ->
                    NowPlayingBar(
                        song = song,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.playPreviousSong() },
                        onNext = { viewModel.playNextSong() },
                        onClick = { showPlayer = true }
                    )
                }
            }

            // Full player
            AnimatedVisibility(
                visible = showPlayer && currentSong != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                currentSong?.let {
                    PlayerScreen(
                        song = it,
                        viewModel = viewModel,
                        onBack = {
                            showPlayer = false
                            selectionMode = false
                            selectedSongs = emptySet()
                        },
                        onShowEqualizer = { showEqualizer = true }
                    )
                }
            }

            // Equalizer
            AnimatedVisibility(
                visible = showEqualizer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EqualizerScreen(
                    viewModel = viewModel,
                    onBack = {
                        showEqualizer = false
                        selectionMode = false
                        selectedSongs = emptySet()
                    }
                )
            }

            // ==================== MODAL BOTTOM SHEET ====================
            if (showBottomSheet && selectedSongForMenu != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                        selectedSongForMenu = null
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    val song = selectedSongForMenu!!

                    val imageModel: ImageRequest = remember(song.albumArtBytes, song.albumArtUri) {
                        if (song.albumArtBytes != null) {
                            ImageRequest.Builder(context)
                                .data(song.albumArtBytes)
                                .crossfade(true)
                                .build()
                        } else {
                            ImageRequest.Builder(context)
                                .data(song.albumArtUri)
                                .crossfade(true)
                                .build()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                                placeholder = painterResource(R.drawable.music_placeholder),
                                error = painterResource(R.drawable.music_placeholder)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    song.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    song.displayArtist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        val actions = mutableListOf<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>()
                        actions.add(Triple("play", "Reproducir", Icons.Rounded.PlayArrow))
                        actions.add(Triple("play_next", "Reproducir después", Icons.Rounded.PlaylistAdd))
                        actions.add(Triple("add_to_queue", "Agregar a cola", Icons.Rounded.QueueMusic))
                        actions.add(Triple("share", "Compartir", Icons.Rounded.Share))
                        actions.add(Triple("add_to_playlist", "Agregar a lista", Icons.Rounded.PlaylistAdd))
                        if (isMenuFromPlaylist && currentPlaylistForMenu != null) {
                            actions.add(Triple("remove_from_playlist", "Quitar de lista", Icons.Rounded.Remove))
                        } else {
                            actions.add(Triple("delete", "Eliminar", Icons.Rounded.Delete))
                        }
                        actions.add(Triple("details", "Información", Icons.Rounded.Info))

                        actions.forEach { (action, displayName, icon) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showBottomSheet = false
                                        when (action) {
                                            "play" -> {
                                                viewModel.playSong(song)
                                                showPlayer = true
                                                selectionMode = false
                                                selectedSongs = emptySet()
                                            }
                                            "play_next" -> {
                                                viewModel.addToQueueNext(song)
                                                Toast.makeText(context, "Agregado a continuación", Toast.LENGTH_SHORT).show()
                                            }
                                            "add_to_queue" -> {
                                                viewModel.addToQueue(song)
                                                Toast.makeText(context, "Agregado a la cola", Toast.LENGTH_SHORT).show()
                                            }
                                            "share" -> viewModel.shareSong(song, context)
                                            "add_to_playlist" -> showAddToPlaylistDialog = song
                                            "remove_from_playlist" -> {
                                                currentPlaylistForMenu?.let { playlist ->
                                                    viewModel.removeSongFromPlaylist(song, playlist.id)
                                                    Toast.makeText(context, "Quitada de ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            "delete" -> {
                                                viewModel.deleteSong(song, context) {}
                                            }
                                            "details" -> {
                                                AlertDialog.Builder(context)
                                                    .setTitle("Detalles de la canción")
                                                    .setMessage(viewModel.showSongDetails(song))
                                                    .setPositiveButton("Cerrar", null)
                                                    .show()
                                            }
                                        }
                                        selectedSongForMenu = null
                                        isMenuFromPlaylist = false
                                        currentPlaylistForMenu = null
                                    },
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (action == "delete" || action == "remove_from_playlist")
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        displayName,
                                        color = if (action == "delete" || action == "remove_from_playlist")
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== DIÁLOGOS ====================

    // Diálogo seleccionar tema
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Selecciona un tema", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    val themes = listOf(
                        AppTheme.DARK to "🌙 Oscuro (Serio/Elegante)",
                        AppTheme.LIGHT to "☀️ Claro (Normal)",
                        AppTheme.PINK to "🌸 Rosa (Femenino/Dulce)",
                        AppTheme.BLUE to "💙 Azul (Masculino/Energético)",
                        AppTheme.PURPLE to "💜 Morado (Elegante/Místico)"
                    )

                    themes.forEach { (theme, label) ->
                        val isSelected = viewModel.currentTheme.value == theme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Diálogo ordenar canciones
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Ordenar canciones", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    val sortOptions = listOf(
                        SortType.TITLE_ASC to "Nombre (A-Z)",
                        SortType.TITLE_DESC to "Nombre (Z-A)",
                        SortType.DURATION_ASC to "Duración (corta a larga)",
                        SortType.DURATION_DESC to "Duración (larga a corta)",
                        SortType.DATE_ADDED_ASC to "Más antiguas primero",
                        SortType.DATE_ADDED_DESC to "Más recientes primero"
                    )

                    sortOptions.forEach { (type, label) ->
                        val isSelected = viewModel.sortType.value == type
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSortType(type)
                                    showSortDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Diálogo crear playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                pendingSongForPlaylist = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Nueva lista", color = MaterialTheme.colorScheme.primary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            view.postDelayed({
                                val updatedPlaylists = viewModel.playlists.value
                                val newPlaylist = updatedPlaylists.find { it.name == newPlaylistName }
                                if (newPlaylist != null) {
                                    pendingSongForPlaylist?.let { song ->
                                        viewModel.addSongToPlaylist(song, newPlaylist.id)
                                        pendingSongForPlaylist = null
                                    }
                                }
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                                Toast.makeText(context, "Lista creada", Toast.LENGTH_SHORT).show()
                            }, 150)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    pendingSongForPlaylist = null
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Diálogo agregar a playlist (individual)
    showAddToPlaylistDialog?.let { song ->
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Agregar a lista", color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Selecciona una lista:", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    if (playlists.isEmpty()) {
                        Text(
                            "No hay listas. Crea una nueva.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        playlists.forEach { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.addSongToPlaylist(song, playlist.id)
                                        showAddToPlaylistDialog = null
                                        Toast.makeText(
                                            context,
                                            "Agregada a ${playlist.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            playlist.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${playlist.songIds.size} canciones",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingSongForPlaylist = song
                    showAddToPlaylistDialog = null
                    showCreatePlaylistDialog = true
                }) {
                    Text("Nueva lista", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ✅ NUEVO: Diálogo para agregar múltiples canciones a playlist
    if (showAddMultipleDialog) {
        AlertDialog(
            onDismissRequest = { showAddMultipleDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    "Agregar ${selectedSongs.size} canciones a playlist",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Selecciona una lista:",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    playlists.forEach { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.addMultipleSongsToPlaylist(
                                        selectedSongs,
                                        playlist.id
                                    ) {
                                        selectedSongs = emptySet()
                                        selectionMode = false
                                    }
                                    showAddMultipleDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        playlist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${playlist.songIds.size} canciones",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMultipleDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // ✅ NUEVO: Diálogo de personalización de colores
    if (showFullColorPicker) {
        FullColorPickerDialog(
            currentColors = customColors ?: CustomThemeColors.DARK,
            onColorSelected = { newColors ->
                viewModel.setCustomThemeColors(newColors)
            },
            onDismiss = { showFullColorPicker = false }
        )
    }
}
// ==================== COMPONENTES ====================

@Composable
fun PlaylistHeader(
    playlist: Playlist,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    playlist.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${playlist.songIds.size} canciones",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Renombrar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar lista", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar playlist", color = MaterialTheme.colorScheme.primary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName)
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                ) {
                    Text("Renombrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cargando canciones...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorMessage(error: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SongList(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    animatedBars: Float,
    isEmptyBySearch: Boolean = false,
    isSelectionMode: Boolean = false,        // ✅ NUEVO
    selectedSongs: Set<Long> = emptySet(),   // ✅ NUEVO
    onSongClick: (Song) -> Unit,
    onSongLongClick: ((Song) -> Unit)? = null,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    viewModel: MusicViewModel,
    isPlaylistView: Boolean = false
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<Song?>(null) }

    val bannerAdUnitId = "ca-app-pub-2117519148690766/6114143828"

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    when {
                        isEmptyBySearch -> "No se encontraron resultados"
                        isPlaylistView -> "No hay canciones en esta lista"
                        else -> "Tu biblioteca está vacía"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp
                )
                if (!isEmptyBySearch && !isPlaylistView) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Agrega música para comenzar",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            songs,
            key = { _, item -> item.id }
        ) { index, song ->
            SongItem(
                song = song,
                index = index,
                totalSongs = songs.size,
                isCurrentAndPlaying = currentSong?.id == song.id && isPlaying,
                animatedBars = animatedBars,
                isSelectionMode = isSelectionMode,           // ✅ NUEVO
                isSelected = selectedSongs.contains(song.id), // ✅ NUEVO
                onSongClick = {
                    if (isSelectionMode) {
                        // ✅ En modo selección, el click selecciona/deselecciona
                        onSongLongClick?.invoke(song)
                    } else {
                        onSongClick(song)
                    }
                },
                onSongLongClick = onSongLongClick?.let { clickHandler ->
                    { clickHandler(song) }
                },
                onRemoveFromPlaylist = if (isPlaylistView) onRemoveFromPlaylist?.let { { it(song) } } else null,
                onShare = { viewModel.shareSong(song, context) },
                onDelete = if (!isPlaylistView) { { showDeleteDialog = song } } else null,
                onDetails = {
                    AlertDialog.Builder(context)
                        .setTitle("Detalles de la canción")
                        .setMessage(viewModel.showSongDetails(song))
                        .setPositiveButton("Cerrar", null)
                        .show()
                },
                isPlaylistView = isPlaylistView
            )
        }

        item {
            AndroidView(
                factory = { ctx ->
                    com.google.android.gms.ads.AdView(ctx).apply {
                        setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                        adUnitId = bannerAdUnitId
                        loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(vertical = 8.dp)
            )
        }
    }

    showDeleteDialog?.let { songToDelete ->
        AlertDialog.Builder(context)
            .setTitle("Eliminar canción")
            .setMessage("¿Eliminar \"${songToDelete.title}\" permanentemente?\n\n⚠️ Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteSong(songToDelete, context) {
                    Toast.makeText(context, "Canción eliminada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
@Composable
fun SongItem(
    song: Song,
    index: Int = 0,
    totalSongs: Int = 0,
    isCurrentAndPlaying: Boolean,
    animatedBars: Float,
    isSelectionMode: Boolean = false,   // ✅ NUEVO
    isSelected: Boolean = false,        // ✅ NUEVO
    onSongClick: () -> Unit,
    onSongLongClick: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onShare: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDetails: () -> Unit,
    isPlaylistView: Boolean = false
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
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSongClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> colorScheme.primary.copy(alpha = 0.25f)  // ✅ Resaltar selección
                isCurrentAndPlaying -> colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Checkbox en modo selección
            if (isSelectionMode) {
                Icon(
                    if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Seleccionado" else "No seleccionado",
                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            // Imagen o placeholder
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    placeholder = painterResource(R.drawable.music_placeholder),
                    error = painterResource(R.drawable.music_placeholder)
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(placeholderColor.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        song.placeholderInitials,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Información de la canción
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = if (isCurrentAndPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentAndPlaying)
                        colorScheme.primary
                    else
                        colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Text(
                    text = song.displayArtist,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                if (isCurrentAndPlaying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((8 + animatedBars * 8 * (index + 1)).dp)
                                    .background(colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                        }
                        Text(
                            "Reproduciendo",
                            fontSize = 11.sp,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = song.formattedDuration,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // ✅ Botón de opciones (solo visible si NO está en modo selección)
            if (!isSelectionMode) {
                if (isPlaylistView && onRemoveFromPlaylist != null) {
                    IconButton(onClick = onRemoveFromPlaylist) {
                        Icon(
                            Icons.Rounded.RemoveCircle,
                            "Quitar de lista",
                            tint = colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            android.util.Log.d("SongItem", "🔘 3 puntitos clickeado - ${song.title}")
                            onSongLongClick?.invoke()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "Opciones",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val placeholderColor = Color(song.placeholderColor)

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
            .padding(12.dp)
            .shadow(elevation = if (isPlaying) 12.dp else 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).shadow(4.dp),
                            placeholder = painterResource(R.drawable.music_placeholder),
                            error = painterResource(R.drawable.music_placeholder)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
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
                    Column {
                        Text(
                            song.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Text(
                            song.displayArtist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Rounded.SkipNext, "Siguiente", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    formatTime((progress * song.duration).toLong()),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}