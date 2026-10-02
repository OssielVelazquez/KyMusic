package com.kyoten.kymusic.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.util.Log

class EqualizerEngine(private val audioSessionId: Int) {

    var equalizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var virtualizer: Virtualizer? = null
    var visualizer: Visualizer? = null

    private var preampGain = 0

    enum class Preset(val presetName: String, val gains: List<Float>) {
        FLAT("Plano", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        ROCK("Rock", listOf(4f, 3f, 2f, 1f, 0f, -1f, -2f, -1f, 1f, 2f)),
        POP("Pop", listOf(2f, 2f, 1f, 1f, 0f, -1f, -1f, 0f, 1f, 1f)),
        JAZZ("Jazz", listOf(3f, 3f, 2f, 1f, 0f, -1f, -2f, -2f, -1f, 0f)),
        CLASSICAL("Clásica", listOf(2f, 2f, 1f, 0f, 0f, -1f, -1f, -1f, 0f, 1f)),
        BASS_BOOST("Bass Boost", listOf(8f, 7f, 5f, 3f, 1f, 0f, 0f, -1f, -2f, -3f)),
        VOCAL_BOOST("Vocal Boost", listOf(-1f, -1f, 0f, 1f, 2f, 3f, 4f, 3f, 2f, 1f)),
        ELECTRONIC("Electrónica", listOf(5f, 4f, 3f, 2f, 0f, -1f, -2f, -1f, 1f, 3f)),
        METAL("Metal", listOf(6f, 5f, 3f, 1f, -1f, -2f, -3f, -2f, 0f, 2f)),
        PODCAST("Podcast", listOf(-2f, -2f, -1f, 0f, 1f, 3f, 5f, 4f, 2f, 0f))
    }

    // En EqualizerEngine.kt - Actualiza la función initialize()

    fun initialize() {
        try {
            // ✅ Para API 36, asegurar que el ecualizador se inicializa correctamente
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            // ✅ Para API 36, verificar que el BassBoost funciona
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }

            // ✅ Para API 36, verificar Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }

            // ✅ Para API 36, Visualizer con manejo de errores mejorado
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    val range = Visualizer.getCaptureSizeRange()
                    captureSize = range[1]
                    enabled = true
                }
            } catch (e: Exception) {
                Log.e("EqualizerEngine", "Visualizer no disponible en este dispositivo", e)
                visualizer = null
            }

            Log.d("EqualizerEngine", "✅ Inicializado con sessionId: $audioSessionId")
        } catch (e: Exception) {
            Log.e("EqualizerEngine", "Error initializing", e)
            // ✅ Intentar recuperar parcialmente
            try {
                if (equalizer == null) {
                    equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
                }
            } catch (e2: Exception) {
                Log.e("EqualizerEngine", "No se pudo inicializar el ecualizador", e2)
            }
        }
    }


    fun setPreamp(gainMB: Int) {
        preampGain = gainMB
    }

    fun setBassBoost(level: Short) {
        bassBoost?.setStrength(level)
    }

    fun setVirtualizer(level: Short) {
        virtualizer?.setStrength(level)
    }

    fun reset() {
        equalizer?.let { eq ->
            for (i in 0 until eq.numberOfBands) {
                setBandLevel(i, 0)
            }
        }
        setPreamp(0)
        setBassBoost(0)
        setVirtualizer(0)
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        visualizer?.release()
    }

    fun setBandLevel(band: Int, levelMB: Int) {
        // ✅ Validar que la banda existe
        val eq = equalizer ?: return
        if (band < 0 || band >= eq.numberOfBands) {
            Log.w("EqualizerEngine", "Banda $band fuera de rango (0-${eq.numberOfBands - 1})")
            return
        }

        // ✅ Validar que el nivel esté dentro del rango permitido
        val range = eq.bandLevelRange
        val minLevel = range[0]
        val maxLevel = range[1]
        val clampedLevel = levelMB.coerceIn(minLevel.toInt(), maxLevel.toInt())

        if (clampedLevel != levelMB) {
            Log.w("EqualizerEngine", "Nivel $levelMB fuera de rango [$minLevel, $maxLevel], ajustado a $clampedLevel")
        }

        eq.setBandLevel(band.toShort(), clampedLevel.toShort())
    }

    fun applyPreset(preset: Preset) {
        val eq = equalizer ?: return
        val realBands = eq.numberOfBands.toInt()

        // ✅ Solo aplicar las bandas que existen
        for (i in 0 until minOf(preset.gains.size, realBands)) {
            val gainDB = preset.gains[i]
            val gainMB = (gainDB * 100).toInt()
            setBandLevel(i, gainMB)
        }
    }
}