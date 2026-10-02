package com.kyoten.kymusic.player.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import com.kyoten.kymusic.MainActivity
import com.kyoten.kymusic.R
import com.kyoten.kymusic.viewmodel.MusicViewModel
import com.kyoten.kymusic.viewmodel.MusicViewModelHolder

@UnstableApi
class MediaPlaybackService : Service() {

    companion object {
        const val ACTION_UPDATE = "com.kyoten.kymusic.UPDATE_NOTIFICATION"
        const val ACTION_PLAY = "com.kyoten.kymusic.PLAY"
        const val ACTION_PAUSE = "com.kyoten.kymusic.PAUSE"
        const val ACTION_NEXT = "com.kyoten.kymusic.NEXT"
        const val ACTION_PREVIOUS = "com.kyoten.kymusic.PREVIOUS"
        const val ACTION_STOP = "com.kyoten.kymusic.STOP"

        private const val CHANNEL_ID = "ky_music_playback_channel"
        const val NOTIFICATION_ID = 101
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private var isForegroundStarted = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "KyMusicSession").apply { isActive = true }
        Log.d("MediaService", "✅ Service creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MediaService", "📩 Acción recibida: ${intent?.action}")

        val action = intent?.action
        val vm = MusicViewModelHolder.viewModel

        if (vm == null) {
            Log.e("MediaService", "❌ ViewModel es null. Deteniendo servicio.")
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_PLAY -> vm.play()
            ACTION_PAUSE -> vm.pause()
            ACTION_NEXT -> vm.playNextSong()
            ACTION_PREVIOUS -> vm.playPreviousSong()
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                // Solo actualizamos la notificación
            }
            else -> Log.w("MediaService", "Acción desconocida: $action")
        }

        // ✅ Iniciar foreground solo si no está ya iniciado
        if (!isForegroundStarted) {
            startForegroundServiceSafely(vm)
        } else {
            // Solo actualizar notificación
            updateNotification(vm)
        }

        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundServiceSafely(vm: MusicViewModel) {
        try {
            val notification = buildNotification(vm)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            Log.d("MediaService", "✅ Foreground service iniciado correctamente")
        } catch (e: Exception) {
            Log.e("MediaService", "❌ Error iniciando foreground service: ${e.message}", e)
            // Intentar de nuevo sin tipo específico
            try {
                val notification = buildNotification(vm)
                startForeground(NOTIFICATION_ID, notification)
                isForegroundStarted = true
            } catch (e2: Exception) {
                Log.e("MediaService", "❌ Error en segundo intento: ${e2.message}")
            }
        }
    }

    private fun updateNotification(vm: MusicViewModel) {
        try {
            val notification = buildNotification(vm)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("MediaService", "❌ Error actualizando notificación: ${e.message}")
        }
    }

    @UnstableApi
    private fun buildNotification(vm: MusicViewModel): Notification {
        val song = vm.currentSong.value
        val isPlaying = vm.isPlaying.value

        val playPauseIntent = getPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cargar carátula (optimizada para reducir memoria)
        var largeIcon: Bitmap? = null
        song?.albumArtUri?.let { uri ->1
            try {
                // ✅ PRIMERO: Decodificar solo los límites para conocer el tamaño real
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                // ✅ SEGUNDO: Calcular el inSampleSize (potencia de 2)
                val targetSize = 256 // Tamaño para la notificación (256x256)
                var sampleSize = 1
                val maxDimension = maxOf(options.outWidth, options.outHeight)

                while (maxDimension / sampleSize > targetSize) {
                    sampleSize *= 2
                }

                // ✅ TERCERO: Decodificar con el sampleSize calculado
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                contentResolver.openInputStream(uri)?.use {
                    largeIcon = BitmapFactory.decodeStream(it, null, decodeOptions)
                }

                Log.d("MediaService", "🖼️ Carátula cargada con sampleSize=$sampleSize (original: ${options.outWidth}x${options.outHeight})")
            } catch (e: Exception) {
                Log.e("MediaService", "Error cargando carátula", e)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "KyMusic")
            .setContentText(song?.artist ?: "Reproduciendo música")
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setColor(0xFF64B5F6.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setProgress(0, 0, false)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "Anterior",
                    getPendingIntent(ACTION_PREVIOUS)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (isPlaying) "Pausar" else "Reproducir",
                    playPauseIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "Siguiente",
                    getPendingIntent(ACTION_NEXT)
                )
            )

        // ✅ Para API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        // ✅ MediaStyle
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(false)

        return builder.setStyle(mediaStyle).build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                NotificationManager.IMPORTANCE_LOW
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de música",
                importance
            ).apply {
                description = "Control de reproducción de música"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setShowBadge(false)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        isForegroundStarted = false
        Log.d("MediaService", "❌ Service destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}