package com.kyoten.kymusic

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.kyoten.kymusic.player.ui.KMusicScreen
import com.kyoten.kymusic.ui.theme.AppTheme
import com.kyoten.kymusic.ui.theme.KyMusicTheme
import com.kyoten.kymusic.utils.AdsManager
import com.kyoten.kymusic.viewmodel.MusicViewModel
import com.kyoten.kymusic.viewmodel.MusicViewModelHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {

    // ✅ Obtener el ViewModel en MainActivity
    private val viewModel: MusicViewModel by viewModels()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AdsManager.initialize(this)

        // Limpia canal viejo si existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel("KyMusicChannel")
        }

        // ✅ Compartir el ViewModel con MusicViewModelHolder
        MusicViewModelHolder.viewModel = viewModel

        setContent {
            // ✅ Observar el tema y los colores personalizados
            val currentTheme by viewModel.currentTheme.collectAsState()
            val customColors by viewModel.customThemeColors.collectAsState()

            KyMusicTheme(
                theme = currentTheme,
                customColors = customColors
            ) {
                // ✅ Pasar el ViewModel a KMusicScreen
                KMusicScreen(viewModel = viewModel)
            }
        }

        lifecycleScope.launch {
            delay(300)
            requestAudioPermissions()
        }
    }

    // ✅ VERSIÓN SIMPLIFICADA - SOLO PERMISOS DE AUDIO
    private fun requestAudioPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Android 12 y anteriores
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            Log.d("MainActivity", "📢 Solicitando permisos de audio: $missing")
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            Log.d("MainActivity", "✅ Permisos de audio ya concedidos")
            reloadMusic()
        }
    }

    // ✅ LAUNCHER PARA PERMISOS DE AUDIO
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->

            val granted = result.values.all { it }

            if (granted) {
                Log.d("MainActivity", "✅ Permisos de audio concedidos")
                reloadMusic()
            } else {
                val deniedPermissions = result.filter { !it.value }.keys
                Log.e("MainActivity", "❌ Permisos denegados: $deniedPermissions")
                Toast.makeText(this, "Sin permisos no se pueden cargar las canciones", Toast.LENGTH_LONG).show()
                // Intentar cargar igual (mostrará lista vacía)
                reloadMusic()
            }
        }

    // ✅ FUNCIÓN SIMPLIFICADA - SOLO CARGAR MÚSICA
    @OptIn(UnstableApi::class)
    private fun reloadMusic() {
        MusicViewModelHolder.viewModel?.let { vm ->
            Log.d("MainActivity", "🔄 Recargando canciones...")
            vm.loadSongs()
        } ?: run {
            Log.e("MainActivity", "⚠️ ViewModel no disponible, reintentando...")
            lifecycleScope.launch {
                delay(500)
                MusicViewModelHolder.viewModel?.loadSongs()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Limpiar el ViewModel del holder
        MusicViewModelHolder.viewModel = null
        Log.d("MainActivity", "🧹 MainActivity destruida")
    }
}