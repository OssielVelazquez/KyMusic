package com.kyoten.kymusic

import android.app.Application
import android.os.Build
import androidx.media3.common.util.UnstableApi

@UnstableApi
class KyMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Para API 36, inicializar cualquier configuración específica
        if (Build.VERSION.SDK_INT >= 36) {
            // Configuraciones específicas para Android 16
            android.util.Log.d("KyMusicApp", "Ejecutando en Android 16 (API 36)")
        }
    }
}