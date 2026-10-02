@file:kotlin.OptIn(UnstableApi::class)

package com.kyoten.kymusic.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kyoten.kymusic.audio.EqualizerEngine
import com.kyoten.kymusic.data.model.Playlist
import com.kyoten.kymusic.data.model.Song
import com.kyoten.kymusic.player.service.MediaPlaybackService
import com.kyoten.kymusic.ui.theme.AppTheme
import com.kyoten.kymusic.ui.theme.CustomThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID
import kotlin.math.min

// ==================== ENUMS ====================
enum class SortType {
    TITLE_ASC,      // Nombre A-Z
    TITLE_DESC,     // Nombre Z-A
    DURATION_ASC,   // Duración corta a larga
    DURATION_DESC,  // Duración larga a corta
    DATE_ADDED_ASC, // Más antiguas primero
    DATE_ADDED_DESC // Más recientes primero
}

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // ==================== PREFERENCIAS ====================
    private val prefs: SharedPreferences =
        application.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
    private val playlistsPrefs: SharedPreferences =
        application.getSharedPreferences("playlists", Context.MODE_PRIVATE)

    // ==================== CONSTANTES ECUALIZADOR ====================
    private val PREFS_EQUALIZER_ENABLED = "equalizer_enabled"
    private val PREFS_EQUALIZER_BANDS = "equalizer_bands"

    // ==================== COLA DE REPRODUCCIÓN ====================
    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue: StateFlow<List<Song>> = _playbackQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private var originalQueueOrder: List<Song> = emptyList()

    // ==================== MODOS REPRODUCCIÓN ====================
    enum class RepeatMode {
        NONE,      // No repetir
        ONE,       // Repetir una canción
        ALL        // Repetir toda la lista
    }

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // ==================== TEMA ====================
    private val _currentTheme = MutableStateFlow(AppTheme.DARK)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // ✅ NUEVO: Colores personalizados
    private val _customThemeColors = MutableStateFlow<CustomThemeColors?>(null)
    val customThemeColors: StateFlow<CustomThemeColors?> = _customThemeColors.asStateFlow()

    // ==================== CANCIONES ====================
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // ==================== PLAYLISTS ====================
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentPlaylistId = MutableStateFlow<String?>(null)
    val currentPlaylistId: StateFlow<String?> = _currentPlaylistId.asStateFlow()

    // ==================== ORDENAMIENTO ====================
    private val _sortType = MutableStateFlow(SortType.TITLE_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // ==================== ECUALIZADOR ====================
    private val _numberOfBands = MutableStateFlow(0)
    val numberOfBands: StateFlow<Int> = _numberOfBands.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(true)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerBands = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val equalizerBands: StateFlow<Map<Int, Int>> = _equalizerBands.asStateFlow()

    private val _equalizerReady = MutableStateFlow(false)
    val equalizerReady: StateFlow<Boolean> = _equalizerReady.asStateFlow()

    private val _preamp = MutableStateFlow(0)
    val preamp: StateFlow<Int> = _preamp.asStateFlow()

    private val _bassBoost = MutableStateFlow(0)
    val bassBoost: StateFlow<Int> = _bassBoost.asStateFlow()

    private val _virtualizer = MutableStateFlow(0)
    val virtualizer: StateFlow<Int> = _virtualizer.asStateFlow()

    private val _currentPreset = MutableStateFlow<String>("Plano")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    // ==================== ESTADO GENERAL ====================
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioData = MutableStateFlow<ByteArray?>(null)
    val audioData: StateFlow<ByteArray?> = _audioData.asStateFlow()

    // ==================== PLAYER ====================
    val player: ExoPlayer = ExoPlayer.Builder(application).build()
    var equalizerEngine: EqualizerEngine? = null

    private var visualizer: Visualizer? = null
    private var playbackRestored = false

    // ==================== CONTENT OBSERVER ====================
    private var mediaObserver: ContentObserver? = null
    private var isObserving = false

    // ==================== DERIVADOS ====================
    val currentPlaylist: StateFlow<Playlist?> = combine(
        _currentPlaylistId,
        _playlists
    ) { playlistId, playlists ->
        playlists.find { it.id == playlistId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private fun getSortedSongs(songs: List<Song>): List<Song> {
        return when (_sortType.value) {
            SortType.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortType.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortType.DURATION_ASC -> songs.sortedBy { it.duration }
            SortType.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SortType.DATE_ADDED_ASC -> songs.sortedBy { it.dateAdded }
            SortType.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
        }
    }

    val currentPlaylistSongs: StateFlow<List<Song>> = combine(
        _currentPlaylistId,
        _allSongs,
        _playlists,
        _sortType
    ) { playlistId, allSongs, playlists, _ ->
        if (playlistId == null) return@combine emptyList()
        val playlist = playlists.find { it.id == playlistId } ?: return@combine emptyList()
        val songs = allSongs.filter { playlist.songIds.contains(it.id) }
        getSortedSongs(songs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val filteredSongs: StateFlow<List<Song>> = combine(
        _allSongs,
        _sortType
    ) { allSongs, _ ->
        getSortedSongs(allSongs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // ==================== LISTENER ====================
    private val playlistListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            savePlaybackState()
            updateMediaPlaybackService()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                playNextSong()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongFromPlayer()
            saveCurrentSong()
            updateMediaPlaybackService()
        }
    }

    // ==================== INIT ====================
    init {
        loadThemePreference()
        loadCustomThemeColors()  // ✅ NUEVO
        Log.d("MusicViewModel", "🚀 Init ViewModel")
        player.addListener(playlistListener)
        loadPlaylists()
        startMediaObserver()

        viewModelScope.launch {
            startPlaybackUpdater()
            delay(100)
            initEqualizer()
        }
    }

    // ==================== MANEJO DE LA COLA ====================
    fun setQueue(newQueue: List<Song>, startIndex: Int = 0) {
        if (newQueue.isEmpty()) {
            clearQueue()
            return
        }
        _playbackQueue.value = newQueue
        val safeIndex = startIndex.coerceIn(0, newQueue.size - 1)
        _currentQueueIndex.value = safeIndex
        originalQueueOrder = newQueue
        playSongAtIndex(safeIndex)
        if (_shuffleEnabled.value) {
            applyShuffleToQueue()
        }
        savePlaybackState()
    }

    fun addToQueue(song: Song) {
        _playbackQueue.value = _playbackQueue.value + song
        if (!_shuffleEnabled.value) {
            originalQueueOrder = originalQueueOrder + song
        }
        savePlaybackState()
    }

    fun addToQueueNext(song: Song) {
        val currentQueue = _playbackQueue.value.toMutableList()
        val insertPos = _currentQueueIndex.value + 1
        currentQueue.add(insertPos, song)
        _playbackQueue.value = currentQueue
        if (!_shuffleEnabled.value) {
            val newOriginal = originalQueueOrder.toMutableList()
            val currentSongObj = _currentSong.value
            val originalIndex = originalQueueOrder.indexOfFirst { it.id == currentSongObj?.id }
            if (originalIndex != -1) {
                newOriginal.add(originalIndex + 1, song)
            } else {
                newOriginal.add(song)
            }
            originalQueueOrder = newOriginal
        }
        savePlaybackState()
    }

    fun removeFromQueue(song: Song) {
        val currentQueue = _playbackQueue.value.toMutableList()
        val indexToRemove = currentQueue.indexOfFirst { it.id == song.id }
        if (indexToRemove == -1) return
        currentQueue.removeAt(indexToRemove)
        _playbackQueue.value = currentQueue

        if (indexToRemove < _currentQueueIndex.value) {
            _currentQueueIndex.value -= 1
        } else if (indexToRemove == _currentQueueIndex.value) {
            if (currentQueue.isNotEmpty()) {
                val newIndex = if (indexToRemove < currentQueue.size) indexToRemove else currentQueue.size - 1
                _currentQueueIndex.value = newIndex
                playSongAtIndex(newIndex)
            } else {
                stopPlayback()
            }
        }

        if (!_shuffleEnabled.value) {
            originalQueueOrder = originalQueueOrder.filter { it.id != song.id }
        }
        savePlaybackState()
    }

    fun clearQueue() {
        _playbackQueue.value = emptyList()
        _currentQueueIndex.value = 0
        originalQueueOrder = emptyList()
        stopPlayback()
        savePlaybackState()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from == to) return
        val currentQueue = _playbackQueue.value.toMutableList()
        val item = currentQueue.removeAt(from)
        currentQueue.add(to, item)
        _playbackQueue.value = currentQueue

        when {
            from == _currentQueueIndex.value -> _currentQueueIndex.value = to
            from < _currentQueueIndex.value && to >= _currentQueueIndex.value -> _currentQueueIndex.value -= 1
            from > _currentQueueIndex.value && to <= _currentQueueIndex.value -> _currentQueueIndex.value += 1
        }

        if (!_shuffleEnabled.value) {
            val newOriginal = originalQueueOrder.toMutableList()
            val itemOrig = newOriginal.removeAt(from)
            newOriginal.add(to, itemOrig)
            originalQueueOrder = newOriginal
        }
        savePlaybackState()
    }

    fun playSongAtIndex(index: Int) {
        val queue = _playbackQueue.value
        if (index !in queue.indices) return
        val song = queue[index]
        _currentSong.value = song
        _currentQueueIndex.value = index
        player.setMediaItem(MediaItem.fromUri(Uri.parse(song.path)))
        player.prepare()
        player.play()
        saveCurrentSong()
        setupVisualizer()
        startMediaPlaybackService()
    }

    // ==================== REPRODUCCIÓN PRINCIPAL ====================
    fun playSong(song: Song) {
        Log.d("MusicViewModel", "▶️ playSong: ${song.title}")
        val sourceList = if (_currentPlaylistId.value != null) currentPlaylistSongs.value else filteredSongs.value
        val index = sourceList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            val newQueue = sourceList.drop(index)
            setQueue(newQueue, 0)
        } else {
            setQueue(listOf(song), 0)
        }
    }

    fun playSongFromPlaylist(song: Song, playlistId: String) {
        selectPlaylist(playlistId)
        playSong(song)
    }

    fun play() {
        if (!player.isPlaying) player.play()
    }

    fun pause() {
        if (player.isPlaying) player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun playNextSong() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        val currentIdx = _currentQueueIndex.value
        val nextIdx = when (_repeatMode.value) {
            RepeatMode.ONE -> currentIdx
            RepeatMode.ALL -> if (currentIdx + 1 < queue.size) currentIdx + 1 else 0
            RepeatMode.NONE -> if (currentIdx + 1 < queue.size) currentIdx + 1 else -1
        }

        if (nextIdx != -1 && nextIdx < queue.size) {
            playSongAtIndex(nextIdx)
        } else if (_repeatMode.value == RepeatMode.NONE) {
            pause()
        }
    }

    fun playPreviousSong() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        val currentIdx = _currentQueueIndex.value
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else {
            if (_repeatMode.value == RepeatMode.ALL) queue.lastIndex else -1
        }

        if (prevIdx != -1) {
            playSongAtIndex(prevIdx)
        } else if (_repeatMode.value != RepeatMode.ALL) {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        if (_shuffleEnabled.value) {
            applyShuffleToQueue()
        } else {
            restoreOriginalOrder()
        }
        savePlaybackState()
    }

    private fun applyShuffleToQueue() {
        val currentQueue = _playbackQueue.value
        if (currentQueue.isEmpty()) return
        val currentSongObj = _currentSong.value ?: return
        val currentIdx = _currentQueueIndex.value

        val before = currentQueue.subList(0, currentIdx)
        val current = listOf(currentSongObj)
        val after = currentQueue.subList(currentIdx + 1, currentQueue.size).shuffled()
        val newQueue = before + current + after

        _playbackQueue.value = newQueue
        _currentQueueIndex.value = before.size
        originalQueueOrder = currentQueue
    }

    private fun restoreOriginalOrder() {
        if (originalQueueOrder.isNotEmpty()) {
            val currentSongObj = _currentSong.value
            val newIndex = originalQueueOrder.indexOfFirst { it.id == currentSongObj?.id }
            _playbackQueue.value = originalQueueOrder
            _currentQueueIndex.value = if (newIndex != -1) newIndex else 0
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        savePlaybackState()
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    private fun stopPlayback() {
        player.stop()
        _isPlaying.value = false
        _currentSong.value = null
        stopMediaPlaybackService()
    }

    // ==================== ECUALIZADOR ====================
    private fun saveEqualizerState() {
        try {
            val bandsString = _equalizerBands.value
                .toSortedMap()
                .values
                .joinToString(",")

            prefs.edit()
                .putBoolean(PREFS_EQUALIZER_ENABLED, _equalizerEnabled.value)
                .putString(PREFS_EQUALIZER_BANDS, bandsString)
                .putInt("preamp", _preamp.value)
                .putInt("bass_boost", _bassBoost.value)
                .putInt("virtualizer", _virtualizer.value)
                .putString("current_preset", _currentPreset.value)
                .apply()

            Log.d("MusicViewModel", "💾 Estado del ecualizador guardado")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error guardando ecualizador", e)
        }
    }

    fun getBandLevelRange(): Pair<Int, Int> {
        return try {
            val eq = equalizerEngine?.equalizer ?: return (-1500 to 1500)
            val range = eq.bandLevelRange
            range[0].toInt() to range[1].toInt()
        } catch (e: Exception) {
            (-1500 to 1500)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        equalizerEngine?.equalizer?.enabled = enabled
        saveEqualizerState()
    }

    fun setEqualizerBandLevel(band: Short, level: Short) {
        try {
            equalizerEngine?.setBandLevel(band.toInt(), level.toInt())
            _equalizerBands.update { current ->
                current.toMutableMap().apply {
                    this[band.toInt()] = level.toInt()
                }
            }
            saveEqualizerState()
        } catch (e: Exception) {
            Log.e("MusicViewModel", "setEqualizerBandLevel error", e)
        }
    }

    fun setPreamp(gainMB: Int) {
        _preamp.value = gainMB
        equalizerEngine?.setPreamp(gainMB)
        saveEqualizerState()
    }

    fun setBassBoost(level: Int) {
        _bassBoost.value = level
        equalizerEngine?.setBassBoost(level.toShort())
        saveEqualizerState()
    }

    fun setVirtualizer(level: Int) {
        _virtualizer.value = level
        equalizerEngine?.setVirtualizer(level.toShort())
        saveEqualizerState()
    }

    fun applyPreset(preset: com.kyoten.kymusic.audio.EqualizerEngine.Preset) {
        _currentPreset.value = preset.presetName

        val realBands = equalizerEngine?.equalizer?.numberOfBands?.toInt() ?: 0
        for (i in 0 until minOf(preset.gains.size, realBands)) {
            val gainDB = preset.gains[i]
            val gainMB = (gainDB * 100).toInt()
            equalizerEngine?.setBandLevel(i, gainMB)
            _equalizerBands.update { current ->
                current.toMutableMap().apply { this[i] = gainMB }
            }
        }
        saveEqualizerState()
    }

    fun resetEqualizer() {
        try {
            equalizerEngine?.reset()
            val bandsCount = equalizerEngine?.equalizer?.numberOfBands?.toInt() ?: 0
            val newMap = mutableMapOf<Int, Int>()
            for (i in 0 until bandsCount) {
                newMap[i] = 0
            }
            _equalizerBands.value = newMap
            _preamp.value = 0
            _bassBoost.value = 0
            _virtualizer.value = 0
            _currentPreset.value = "Plano"
            saveEqualizerState()
            Log.d("MusicViewModel", "✅ Ecualizador reseteado completamente")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "resetEqualizer error", e)
        }
    }

    private fun initEqualizer() {
        try {
            val sessionId = player.audioSessionId
            if (sessionId == 0) {
                Log.d("MusicViewModel", "⏳ Esperando sessionId para ecualizador...")
                return
            }
            equalizerEngine = EqualizerEngine(sessionId).apply {
                initialize()
            }
            _numberOfBands.value = equalizerEngine?.equalizer?.numberOfBands?.toInt() ?: 0
            loadSavedEqualizerSettings()
            _equalizerReady.value = true
            Log.d("MusicViewModel", "✅ Ecualizador inicializado correctamente con ${_numberOfBands.value} bandas")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error inicializando ecualizador", e)
            _equalizerReady.value = false
        }
    }

    private fun loadSavedEqualizerSettings() {
        try {
            val eq = equalizerEngine?.equalizer
            val realNumberOfBands = eq?.numberOfBands?.toInt() ?: 0
            val savedBands = prefs.getString(PREFS_EQUALIZER_BANDS, null)
            val bandsMap = mutableMapOf<Int, Int>()
            if (savedBands != null) {
                val bandsList = savedBands.split(",").mapNotNull { it.toIntOrNull() }
                bandsList.forEachIndexed { index, value ->
                    if (index < realNumberOfBands) {
                        eq?.setBandLevel(index.toShort(), value.toShort())
                        bandsMap[index] = value
                    }
                }
            } else {
                for (i in 0 until realNumberOfBands) {
                    val currentLevel = eq?.getBandLevel(i.toShort())?.toInt() ?: 0
                    bandsMap[i] = currentLevel
                }
            }
            _equalizerBands.value = bandsMap
            _equalizerEnabled.value = prefs.getBoolean(PREFS_EQUALIZER_ENABLED, true)
            eq?.enabled = _equalizerEnabled.value
            _preamp.value = prefs.getInt("preamp", 0)
            _bassBoost.value = prefs.getInt("bass_boost", 0)
            _virtualizer.value = prefs.getInt("virtualizer", 0)
            _currentPreset.value = prefs.getString("current_preset", "Plano") ?: "Plano"
            equalizerEngine?.setPreamp(_preamp.value)
            equalizerEngine?.setBassBoost(_bassBoost.value.toShort())
            equalizerEngine?.setVirtualizer(_virtualizer.value.toShort())
            Log.d("MusicViewModel", "✅ Configuración cargada: ${bandsMap.size} bandas, preamp=${_preamp.value}, bass=${_bassBoost.value}, virt=${_virtualizer.value}")
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error cargando configuración", e)
        }
    }

    // ==================== TEMA ====================
    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    private fun loadThemePreference() {
        val themeName = prefs.getString("app_theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
        _currentTheme.value = try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.DARK
        }
    }

    // ✅ NUEVO: Guardar colores personalizados
    fun setCustomThemeColors(colors: CustomThemeColors?) {
        _customThemeColors.value = colors
        if (colors != null) {
            prefs.edit()
                .putInt("custom_logo", colors.logoColor.toArgb())
                .putInt("custom_button", colors.buttonColor.toArgb())
                .putInt("custom_background", colors.backgroundColor.toArgb())
                .putInt("custom_text", colors.textColor.toArgb())
                .putInt("custom_surface", colors.surfaceColor.toArgb())
                .putInt("custom_accent", colors.accentColor.toArgb())
                .putBoolean("has_custom_colors", true)
                .apply()
            Log.d("MusicViewModel", "🎨 Colores personalizados guardados")
        } else {
            prefs.edit().putBoolean("has_custom_colors", false).apply()
            Log.d("MusicViewModel", "🎨 Colores personalizados eliminados")
        }
    }

    // ✅ NUEVO: Cargar colores personalizados
    fun loadCustomThemeColors() {
        val hasCustomColors = prefs.getBoolean("has_custom_colors", false)
        if (hasCustomColors) {
            try {
                val colors = CustomThemeColors(
                    logoColor = Color(prefs.getInt("custom_logo", CustomThemeColors.DARK.logoColor.toArgb())),
                    buttonColor = Color(prefs.getInt("custom_button", CustomThemeColors.DARK.buttonColor.toArgb())),
                    backgroundColor = Color(prefs.getInt("custom_background", CustomThemeColors.DARK.backgroundColor.toArgb())),
                    textColor = Color(prefs.getInt("custom_text", CustomThemeColors.DARK.textColor.toArgb())),
                    surfaceColor = Color(prefs.getInt("custom_surface", CustomThemeColors.DARK.surfaceColor.toArgb())),
                    accentColor = Color(prefs.getInt("custom_accent", CustomThemeColors.DARK.accentColor.toArgb())),
                )
                _customThemeColors.value = colors
                Log.d("MusicViewModel", "🎨 Colores personalizados cargados")
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error cargando colores personalizados", e)
                _customThemeColors.value = null
            }
        } else {
            _customThemeColors.value = null
        }
    }

    // ==================== PLAYLISTS ====================
    private fun parsePlaylistsFromJson(json: String): List<Playlist> {
        Log.d("PlaylistDebug", "🔍 Parseando JSON: $json")
        val playlists = mutableListOf<Playlist>()
        if (json == "[]" || json.isBlank()) return emptyList()

        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val id = jsonObject.getString("id")
                val name = jsonObject.getString("name")
                val songIdsArray = jsonObject.optJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                if (songIdsArray != null) {
                    for (j in 0 until songIdsArray.length()) {
                        songIds.add(songIdsArray.getLong(j))
                    }
                }
                Log.d("PlaylistDebug", "📀 Playlist parseada: $name, songIds: $songIds")
                playlists.add(Playlist(id, name, songIds))
            }
        } catch (e: Exception) {
            Log.e("PlaylistDebug", "Error parsing playlists", e)
        }
        return playlists
    }

    private fun loadPlaylists() {
        val playlistsJson = playlistsPrefs.getString("playlists_list", "[]") ?: "[]"
        Log.d("PlaylistDebug", "📂 Playlists CARGADAS desde SharedPreferences: $playlistsJson")
        val playlistsList = parsePlaylistsFromJson(playlistsJson)
        _playlists.value = playlistsList
        Log.d("PlaylistDebug", "📋 Playlists en memoria después de cargar: ${_playlists.value.map { "${it.name}: ${it.songIds}" }}")
    }

    private fun savePlaylists() {
        val json = buildString {
            append("[")
            _playlists.value.forEachIndexed { index, playlist ->
                if (index > 0) append(",")
                append("{\"id\":\"${playlist.id}\",\"name\":\"${playlist.name}\",\"songIds\":[${playlist.songIds.joinToString(",")}]}")
            }
            append("]")
        }
        playlistsPrefs.edit().putString("playlists_list", json).apply()
        Log.d("PlaylistDebug", "💾 Playlists GUARDADAS en SharedPreferences: $json")
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            songIds = emptyList()
        )
        _playlists.value = _playlists.value + newPlaylist
        savePlaylists()
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        if (_currentPlaylistId.value == playlistId) {
            _currentPlaylistId.value = null
        }
        savePlaylists()
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        if (newName.isBlank()) return

        val updatedPlaylists = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(name = newName)
            } else {
                playlist
            }
        }
        _playlists.value = updatedPlaylists
        savePlaylists()
    }

    fun addSongToPlaylist(song: Song, playlistId: String) {
        Log.d("PlaylistDebug", "➕ addSongToPlaylist - Canción: ${song.title}, ID: ${song.id}, PlaylistID: $playlistId")

        val currentPlaylist = _playlists.value.find { it.id == playlistId } ?: run {
            Log.e("PlaylistDebug", "❌ Playlist no encontrada: $playlistId")
            return
        }

        Log.d("PlaylistDebug", "📋 Playlist actual: ${currentPlaylist.name}, Canciones: ${currentPlaylist.songIds}")

        if (currentPlaylist.songIds.contains(song.id)) {
            Log.d("PlaylistDebug", "⚠️ La canción ya está en la playlist")
            return
        }

        val updatedPlaylist = currentPlaylist.copy(
            songIds = currentPlaylist.songIds + song.id
        )
        val updatedPlaylists = _playlists.value.map {
            if (it.id == playlistId) updatedPlaylist else it
        }
        _playlists.value = updatedPlaylists

        Log.d("PlaylistDebug", "✅ Playlist actualizada: ${updatedPlaylist.name}, Canciones: ${updatedPlaylist.songIds}")

        savePlaylists()
    }

    fun removeSongFromPlaylist(song: Song, playlistId: String) {
        val currentPlaylist = _playlists.value.find { it.id == playlistId } ?: return
        val updatedPlaylist = currentPlaylist.copy(
            songIds = currentPlaylist.songIds.filter { it != song.id }
        )
        val updatedPlaylists = _playlists.value.map {
            if (it.id == playlistId) updatedPlaylist else it
        }
        _playlists.value = updatedPlaylists
        savePlaylists()
        Log.d("MusicViewModel", "➖ Canción ${song.title} eliminada de playlist ${currentPlaylist.name}")
    }

    fun moveSongInPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
        Log.d("PlaylistDebug", "🔄 moveSongInPlaylist - playlistId: $playlistId, from: $fromIndex, to: $toIndex")

        if (fromIndex == toIndex) return

        val playlist = _playlists.value.find { it.id == playlistId } ?: return
        Log.d("PlaylistDebug", "📋 Playlist antes: ${playlist.songIds}")

        val songIds = playlist.songIds.toMutableList()
        val songId = songIds.removeAt(fromIndex)
        songIds.add(toIndex, songId)

        val updatedPlaylist = playlist.copy(songIds = songIds)
        val updatedPlaylists = _playlists.value.map {
            if (it.id == playlistId) updatedPlaylist else it
        }
        _playlists.value = updatedPlaylists
        savePlaylists()

        Log.d("PlaylistDebug", "📋 Playlist después: ${updatedPlaylist.songIds}")
    }

    fun selectPlaylist(playlistId: String?) {
        _currentPlaylistId.value = playlistId
    }

    // ==================== CONTENT OBSERVER ====================
    private fun startMediaObserver() {
        if (isObserving) return
        val handler = Handler(Looper.getMainLooper())
        mediaObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                handler.postDelayed({ refreshSongs() }, 1000)
            }
        }
        val context = getApplication<Application>()
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver!!
        )
        isObserving = true
    }

    private fun stopMediaObserver() {
        mediaObserver?.let {
            val context = getApplication<Application>()
            context.contentResolver.unregisterContentObserver(it)
            mediaObserver = null
            isObserving = false
        }
    }

    fun refreshSongs() {
        loadSongs()
    }

    // ==================== SONGS ====================
    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                val list = loadSongsFromDevice()
                withContext(Dispatchers.Main) {
                    _allSongs.value = list
                    cleanInvalidPlaylistSongs()
                    _isLoading.value = false
                    if (!playbackRestored) {
                        restorePlaybackState()
                        playbackRestored = true
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "❌ Error cargando canciones", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _error.value = e.message
                }
            }
        }
    }

    private fun cleanInvalidPlaylistSongs() {
        var changed = false
        val updatedPlaylists = _playlists.value.map { playlist ->
            val validSongIds = playlist.songIds.filter { songId ->
                _allSongs.value.any { it.id == songId }
            }
            if (validSongIds.size != playlist.songIds.size) {
                changed = true
                playlist.copy(songIds = validSongIds)
            } else {
                playlist
            }
        }
        if (changed) {
            _playlists.value = updatedPlaylists
            savePlaylists()
        }
    }

    private suspend fun loadSongsFromDevice(): List<Song> {
        val context = getApplication<Application>()
        val list = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor = context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val albumId = it.getLong(albumIdCol)
                val filePath = it.getString(dataCol) ?: ""
                val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
                val rawTitle = it.getString(titleCol) ?: fileName

                val cleanTitle = rawTitle
                    .replace(Regex("\\[.*?\\]"), "")
                    .replace(Regex("\\(.*?\\)"), "")
                    .replace("ytmp3", "", ignoreCase = true)
                    .replace("youtube", "", ignoreCase = true)
                    .replace("mp3", "", ignoreCase = true)
                    .trim()
                    .takeIf { it.isNotBlank() } ?: "Sin título"

                var extractedArtist: String? = null
                var extractedTitle = cleanTitle
                if (cleanTitle.contains(" - ")) {
                    val parts = cleanTitle.split(" - ")
                    if (parts.size >= 2) {
                        extractedArtist = parts[0].trim()
                        extractedTitle = parts.drop(1).joinToString(" - ").trim()
                    }
                }

                val rawArtist = it.getString(artistCol)
                val finalArtist = when {
                    rawArtist.isNullOrBlank() || rawArtist.equals("<unknown>", ignoreCase = true) -> extractedArtist
                    else -> rawArtist.trim()
                }

                val rawAlbum = it.getString(albumCol)
                val finalAlbum = when {
                    rawAlbum.isNullOrBlank() || rawAlbum.equals("<unknown>", ignoreCase = true) -> "Descargas"
                    else -> rawAlbum.trim()
                }

                var albumArtBytes: ByteArray? = null
                val albumArtUri = if (albumId > 0) {
                    Uri.parse("content://media/external/audio/albumart/$albumId")
                } else null

                if (albumArtUri != null) {
                    try {
                        context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                            albumArtBytes = stream.readBytes()
                        }
                    } catch (e: Exception) { }
                }

                val song = Song(
                    id = it.getLong(idCol),
                    title = extractedTitle,
                    artist = finalArtist,
                    album = finalAlbum,
                    path = filePath,
                    duration = it.getLong(durCol),
                    size = it.getLong(sizeCol),
                    dateAdded = it.getLong(dateCol),
                    albumArtUri = albumArtUri,
                    albumArtBytes = albumArtBytes
                )
                if (!song.isAudioMessage && song.path.isNotEmpty()) {
                    list.add(song)
                }
            }
        }
        return list
    }

    fun deleteSong(song: Song, context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var deleted = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            song.id
                        )

                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(uri)
                        )

                        withContext(Dispatchers.Main) {
                            pendingIntent?.let { intent ->
                                intent.send()
                                deleted = true
                                onSuccess()
                                return@withContext
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Error con createDeleteRequest", e)
                    }
                }

                if (!deleted) {
                    try {
                        val file = File(song.path)
                        if (file.exists()) {
                            deleted = file.delete()
                            if (deleted) {
                                context.contentResolver.delete(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    "${MediaStore.Audio.Media.DATA} = ?",
                                    arrayOf(song.path)
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Canción eliminada", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Error con File.delete", e)
                    }
                }

                if (!deleted) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error general", e)
            }
        }
    }


    fun deleteMultipleSongs(songIds: Set<Long>, context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songsToDelete = _allSongs.value.filter { songIds.contains(it.id) }

                if (songsToDelete.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val uris = songsToDelete.map { song ->
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                song.id
                            )
                        }

                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        )

                        withContext(Dispatchers.Main) {
                            pendingIntent?.let { intent ->
                                val songsToDeleteCopy = songsToDelete.toList()

                                // ✅ Mostrar el diálogo de Android
                                intent.send()

                                // ✅ Esperar a que el usuario confirme
                                delay(2000)

                                // ✅ Verificar cuáles canciones fueron eliminadas realmente
                                var deletedCount = 0
                                songsToDeleteCopy.forEach { song ->
                                    val file = File(song.path)
                                    if (!file.exists()) {
                                        removeSongFromList(song)
                                        deletedCount++
                                    }
                                }

                                // ✅ SI HUBO ELIMINACIONES, la lista se actualiza sola
                                // NO llamar a loadSongs() aquí

                                onComplete()
                            } ?: run {
                                deleteWithFileFallback(songsToDelete, context, onComplete)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Error con createDeleteRequest", e)
                        deleteWithFileFallback(songsToDelete, context, onComplete)
                    }
                } else {
                    // ✅ Android 10 y anteriores
                    songsToDelete.forEach { song ->
                        try {
                            val uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                song.id
                            )
                            context.contentResolver.delete(uri, null, null)
                            removeSongFromList(song)
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Error eliminando ${song.title}", e)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        // ✅ NO llamar a loadSongs() aquí
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error general", e)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    private suspend fun deleteWithFileFallback(
        songs: List<Song>,
        context: Context,
        onComplete: () -> Unit
    ) {
        songs.forEach { song ->
            try {
                val file = File(song.path)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        context.contentResolver.delete(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            "${MediaStore.Audio.Media.DATA} = ?",
                            arrayOf(song.path)
                        )
                        removeSongFromList(song)
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error con File.delete para ${song.title}", e)
            }
        }

        withContext(Dispatchers.Main) {
            // ✅ NO llamar a loadSongs() aquí
            onComplete()
        }
    }

    // Función auxiliar para eliminar una canción de la lista en memoria
    private fun removeSongFromList(song: Song) {
        // Actualizar playlists
        val updatedPlaylists = _playlists.value.map { playlist ->
            if (playlist.songIds.contains(song.id)) {
                playlist.copy(songIds = playlist.songIds.filter { it != song.id })
            } else {
                playlist
            }
        }
        _playlists.value = updatedPlaylists
        savePlaylists()

        // Actualizar lista de canciones
        val newSongsList = _allSongs.value.toMutableList()
        newSongsList.removeAll { it.id == song.id }
        _allSongs.value = newSongsList

        // Si la canción eliminada era la que se estaba reproduciendo
        if (_currentSong.value?.id == song.id) {
            if (filteredSongs.value.isNotEmpty() || currentPlaylistSongs.value.isNotEmpty()) {
                val nextSong = if (currentPlaylistSongs.value.isNotEmpty()) {
                    currentPlaylistSongs.value.first()
                } else {
                    filteredSongs.value.firstOrNull()
                }
                if (nextSong != null) {
                    playSong(nextSong)
                } else {
                    pause()
                    _currentSong.value = null
                    stopMediaPlaybackService()
                }
            } else {
                pause()
                _currentSong.value = null
                stopMediaPlaybackService()
            }
        }
    }

    // ==================== PLAYBACK HELPERS ====================
    private fun setupVisualizer() {
        try {
            visualizer?.release()
            val sessionId = player.audioSessionId
            if (sessionId == 0) return
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                try {
                    enabled = true
                } catch (e: UnsupportedOperationException) {
                    Log.e("MusicViewModel", "Visualizer no soportado")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Visualizer error", e)
            visualizer = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMediaObserver()
        player.removeListener(playlistListener)
        player.release()
        equalizerEngine?.release()
        visualizer?.release()
        stopMediaPlaybackService()
    }

    private fun startMediaPlaybackService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_UPDATE
        }
        try {
            // ✅ Para API 36, manejar correctamente el inicio del servicio
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error starting service", e)
            // ✅ Si falla, intentar de nuevo con un pequeño delay
            viewModelScope.launch {
                delay(500)
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    Log.e("MusicViewModel", "Error en segundo intento", e2)
                }
            }
        }
    }

    private fun updateMediaPlaybackService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_UPDATE
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error updating service", e)
        }
    }

    fun stopMediaPlaybackService() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, MediaPlaybackService::class.java))
    }

    private fun savePlaybackState() {
        prefs.edit()
            .putBoolean("is_playing", player.isPlaying)
            .putBoolean("shuffle", _shuffleEnabled.value)
            .putString("repeat_mode", _repeatMode.value.name)
            .apply()
    }

    private fun saveCurrentSong() {
        _currentSong.value?.let {
            prefs.edit()
                .putLong("current_song_id", it.id)
                .putString("current_song_path", it.path)
                .apply()
        }
    }

    private fun updateCurrentSongFromPlayer() {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val path = currentMediaItem.localConfiguration?.uri.toString()
            _currentSong.value = filteredSongs.value.find { it.path == path }
                ?: allSongs.value.find { it.path == path }
        }
    }

    private fun startPlaybackUpdater() {
        viewModelScope.launch {
            while (true) {
                try {
                    val pos = player.currentPosition
                    val dur = player.duration
                    _currentPosition.value = pos
                    _duration.value = dur
                    _playbackProgress.value = if (dur > 0) pos.toFloat() / dur.toFloat() else 0f
                } catch (_: Exception) { }
                delay(500)
            }
        }
    }

    private fun restorePlaybackState() {
        val savedPath = prefs.getString("current_song_path", null)
        if (savedPath != null && _allSongs.value.isNotEmpty()) {
            try {
                player.setMediaItem(MediaItem.fromUri(Uri.parse(savedPath)))
                player.prepare()
                _currentSong.value = _allSongs.value.find { it.path == savedPath }
                if (prefs.getBoolean("is_playing", false)) {
                    player.play()
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "restorePlaybackState error", e)
            }
        }

        _shuffleEnabled.value = prefs.getBoolean("shuffle", false)
        val repeatName = prefs.getString("repeat_mode", RepeatMode.NONE.name)
        _repeatMode.value = try {
            RepeatMode.valueOf(repeatName!!)
        } catch (e: Exception) {
            RepeatMode.NONE
        }

        if (_allSongs.value.isNotEmpty()) {
            val currentList = getCurrentQueue()
            if (_shuffleEnabled.value) {
                originalQueueOrder = currentList
                _playbackQueue.value = currentList.shuffled()
                val currentSongObj = _currentSong.value
                val newIndex = _playbackQueue.value.indexOfFirst { it.id == currentSongObj?.id }
                _currentQueueIndex.value = if (newIndex != -1) newIndex else 0
            } else {
                originalQueueOrder = currentList
                _playbackQueue.value = currentList
                _currentQueueIndex.value = 0
            }
        }
    }

    private fun getCurrentQueue(): List<Song> {
        return if (_currentPlaylistId.value != null) {
            currentPlaylistSongs.value
        } else {
            filteredSongs.value
        }
    }

    fun shareSong(song: Song, context: Context) {
        try {
            val audioFile = File(song.path)
            if (!audioFile.exists()) {
                _error.value = "El archivo no existe"
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error sharing song", e)
            _error.value = "No se pudo compartir: ${e.message}"
        }
    }

    fun showSongDetails(song: Song): String {
        return buildString {
            append("📌 ${song.title}\n\n")
            append("🎤 Artista: ${song.artist ?: "Desconocido"}\n")
            append("💿 Álbum: ${song.album}\n")
            append("⏱️ Duración: ${song.formattedDuration}\n")
            append("📁 Ruta: ${song.path}\n")
            append("💾 Tamaño: ${formatFileSize(song.size)}\n")
            append("📅 Añadido: ${formatDate(song.dateAdded)}")
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Desconocida"
        val date = java.util.Date(timestamp * 1000)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }


    // ✅ AGREGAR MÚLTIPLES A PLAYLIST
    fun addMultipleSongsToPlaylist(songIds: Set<Long>, playlistId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = _allSongs.value.filter { songIds.contains(it.id) }
            var addedCount = 0

            songs.forEach { song ->
                val playlist = _playlists.value.find { it.id == playlistId }
                if (playlist != null && !playlist.songIds.contains(song.id)) {
                    addSongToPlaylist(song, playlistId)
                    addedCount++
                }
            }

            withContext(Dispatchers.Main) {
                if (addedCount > 0) {
                    Toast.makeText(
                        getApplication(),
                        "$addedCount canción${if (addedCount > 1) "es" else ""} agregada${if (addedCount > 1) "s" else ""} a la playlist",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(getApplication(), "Las canciones ya están en la playlist", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
        }
    }


    // ✅ Compartir múltiples canciones (con archivos de audio reales)
    fun shareMultipleSongs(songIds: Set<Long>, context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = _allSongs.value.filter { songIds.contains(it.id) }

            withContext(Dispatchers.Main) {
                if (songs.isEmpty()) {
                    Toast.makeText(context, "No hay canciones para compartir", Toast.LENGTH_SHORT).show()
                    onComplete()
                    return@withContext
                }

                if (songs.size == 1) {
                    // ✅ Si es una sola canción, usar shareSong existente
                    shareSong(songs.first(), context)
                } else {
                    // ✅ Múltiples canciones
                    try {
                        // ✅ Crear lista de archivos a compartir
                        val files = mutableListOf<Uri>()
                        songs.forEach { song ->
                            val file = File(song.path)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                files.add(uri)
                            }
                        }

                        if (files.isEmpty()) {
                            Toast.makeText(context, "No se encontraron archivos para compartir", Toast.LENGTH_SHORT).show()
                            onComplete()
                            return@withContext
                        }

                        // ✅ Crear intent para compartir múltiples archivos
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND_MULTIPLE
                            type = "audio/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(Intent.createChooser(shareIntent, "Compartir canciones"))

                    } catch (e: Exception) {
                        Log.e("MusicViewModel", "Error compartiendo múltiples", e)
                        // ✅ Fallback: compartir como texto si falla
                        val songList = songs.joinToString("\n") { "🎵 ${it.title} - ${it.displayArtist}" }
                        val textIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "🎵 Mis canciones favoritas en KyMusic:\n\n$songList")
                        }
                        context.startActivity(Intent.createChooser(textIntent, "Compartir canciones"))
                        Toast.makeText(context, "No se pudieron compartir los archivos, enviando lista de canciones", Toast.LENGTH_SHORT).show()
                    }
                }
                onComplete()
            }
        }
    }

    // ✅ AGREGAR MÚLTIPLES A LA COLA (evitando duplicados)
    fun addMultipleToQueue(songIds: Set<Long>, onComplete: () -> Unit = {}) {
        val songs = _allSongs.value.filter { songIds.contains(it.id) }
        val currentQueue = _playbackQueue.value.toMutableList()
        var addedCount = 0

        songs.forEach { song ->
            // ✅ Verificar que la canción no esté ya en la cola
            if (!currentQueue.any { it.id == song.id }) {
                addToQueue(song)
                addedCount++
            }
        }

        if (addedCount > 0) {
            Toast.makeText(
                getApplication(),
                "$addedCount canción${if (addedCount > 1) "es" else ""} agregada${if (addedCount > 1) "s" else ""} a la cola",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(getApplication(), "Las canciones ya están en la cola", Toast.LENGTH_SHORT).show()
        }
        onComplete()
    }

    // ✅ AGREGAR MÚLTIPLES A "REPRODUCIR DESPUÉS" (evitando duplicados)
    fun addMultipleToQueueNext(songIds: Set<Long>, onComplete: () -> Unit = {}) {
        val songs = _allSongs.value.filter { songIds.contains(it.id) }
        val currentQueue = _playbackQueue.value.toMutableList()
        var addedCount = 0

        // ✅ Filtrar canciones que ya están en la cola
        val songsToAdd = songs.filter { !currentQueue.any { queueSong -> queueSong.id == it.id } }

        // ✅ Agregar en orden inverso para que el primero seleccionado quede primero
        songsToAdd.reversed().forEach { song ->
            addToQueueNext(song)
            addedCount++
        }

        if (addedCount > 0) {
            Toast.makeText(
                getApplication(),
                "$addedCount canción${if (addedCount > 1) "es" else ""} agregada${if (addedCount > 1) "s" else ""} a continuación",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(getApplication(), "Las canciones ya están en la cola", Toast.LENGTH_SHORT).show()
        }
        onComplete()
    }
}