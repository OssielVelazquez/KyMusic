package com.kyoten.kymusic.data.model

import android.net.Uri
import java.io.Serializable
import java.util.Arrays

data class Song(
    val id: Long,
    val title: String,
    val artist: String?,
    val album: String,
    val path: String,
    val duration: Long,
    val size: Long = 0,
    val dateAdded: Long = 0,
    val albumArtUri: Uri? = null,
    val albumArtBytes: ByteArray? = null
) : Serializable {

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val isAudioMessage: Boolean
        get() = title.contains("AUD-", ignoreCase = true) ||
                title.contains("WhatsApp", ignoreCase = true) ||
                path.contains("WhatsApp", ignoreCase = true)

    val fileName: String
        get() = java.io.File(path).name

    val fileExtension: String
        get() = path.substringAfterLast(".", "")


    val displayAlbum: String
        get() = album.takeIf { it.isNotBlank() && it != "Álbum desconocido" && it != "<unknown>" }
            ?: "Descargas"

    // Song.kt - Añade estas propiedades
    // Song.kt - Reemplaza placeholderColor y placeholderInitials

    // Song.kt - Reemplaza placeholderColor y placeholderInitials

    // Song.kt - Reemplaza placeholderColor

    val placeholderColor: Int
        get() {
            // ✅ Colores como Int (con el sufijo .toInt())
            val colors = listOf(
                0xFF00E5FF.toInt(),  // Cyan
                0xFF7C4DFF.toInt(),  // Morado
                0xFF00BCD4.toInt(),  // Cyan medio
                0xFF4CAF50.toInt(),  // Verde
                0xFFFF5722.toInt(),  // Naranja
                0xFFE91E63.toInt(),  // Rosa
                0xFF9C27B0.toInt(),  // Púrpura
                0xFF2196F3.toInt(),  // Azul
                0xFFFF9800.toInt(),  // Naranja oscuro
                0xFF8BC34A.toInt(),  // Verde claro
                0xFF00ACC1.toInt(),  // Cyan oscuro
                0xFFF44336.toInt(),  // Rojo
                0xFF3F51B5.toInt(),  // Índigo
                0xFF009688.toInt(),  // Verde azulado
                0xFFFFC107.toInt(),  // Ámbar
                0xFF673AB7.toInt(),  // Morado oscuro
                0xFFCDDC39.toInt(),  // Lima
                0xFFFF4081.toInt(),  // Rosa claro
                0xFF69F0AE.toInt(),  // Menta
                0xFF448AFF.toInt(),  // Azul claro
                0xFFAA00FF.toInt()   // Púrpura brillante
            )

            // Algoritmo mejorado para distribuir colores
            val baseString = (artist?.takeIf { it != "Artista desconocido" && it != "<unknown>" }
                ?: title).lowercase()

            var hash = 0
            for (char in baseString) {
                hash = (hash * 31 + char.code) and 0x7FFFFFFF
            }

            // Usar fibonacci hashing para mejor distribución
            val goldenRatio = 0x9E3779B9.toInt()
            var finalHash = hash
            finalHash = finalHash xor (finalHash ushr 16)
            finalHash = finalHash * goldenRatio
            finalHash = finalHash xor (finalHash ushr 16)

            // ✅ Índice seguro como Int
            val index = (finalHash and 0x7FFFFFFF) % colors.size
            return colors[index]
        }
    val placeholderInitials: String
        get() {
            // Priorizar artista si existe
            val nameForInitials = when {
                artist != null && artist != "Artista desconocido" && artist != "<unknown>" -> artist!!
                title.contains(" - ") -> title.substringBefore(" - ")
                else -> title
            }

            // Limpiar caracteres especiales
            val cleanName = nameForInitials
                .replace(Regex("[^a-zA-ZáéíóúñÑüÜ\\s]"), "")
                .trim()

            val words = cleanName.split(" ", "-", "_").filter { it.isNotEmpty() }

            return when {
                words.size >= 2 -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
                cleanName.length >= 2 -> cleanName.take(2).uppercase()
                cleanName.isNotEmpty() -> cleanName.take(1).uppercase()
                else -> "??"
            }
        }

    val displayArtist: String
        get() = artist?.takeIf {
            it.isNotBlank() &&
                    it != "<unknown>" &&
                    it != "Artista desconocido" &&
                    !it.contains("unknown", ignoreCase = true)
        } ?: "Artista desconocido"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Song

        if (id != other.id) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (path != other.path) return false
        if (duration != other.duration) return false
        if (!Arrays.equals(albumArtBytes, other.albumArtBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + album.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + (albumArtBytes?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}