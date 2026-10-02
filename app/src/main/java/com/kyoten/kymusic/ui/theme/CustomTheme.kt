package com.kyoten.kymusic.ui.theme


import androidx.compose.ui.graphics.Color

data class CustomThemeColors(
    val logoColor: Color = Color(0xFF00E5FF),       // 🎵 Logo y acentos
    val buttonColor: Color = Color(0xFF00E5FF),     // 🔘 Botones
    val backgroundColor: Color = Color(0xFF0A0A0F), // 📱 Fondo
    val textColor: Color = Color.White,             // 📝 Texto
    val surfaceColor: Color = Color(0xFF1A1A2E),    // 🖼️ Superficies (cards)
    val accentColor: Color = Color(0xFF7C4DFF),     // ✨ Acentos secundarios
) {
    companion object {
        // ✅ Tema oscuro por defecto (el que ya tienes)
        val DARK = CustomThemeColors(
            logoColor = Color(0xFF00E5FF),
            buttonColor = Color(0xFF00E5FF),
            backgroundColor = Color(0xFF0A0A0F),
            textColor = Color(0xFFFFFFFF),
            surfaceColor = Color(0xFF1E1E2E),
            accentColor = Color(0xFF00E5FF),
        )

        // ✅ Tema claro
        val LIGHT = CustomThemeColors(
            logoColor = Color(0xFF00BCD4),
            buttonColor = Color(0xFF00BCD4),
            backgroundColor = Color(0xFFF5F5F5),
            textColor = Color.Black,
            surfaceColor = Color.White,
            accentColor = Color(0xFF7C4DFF),
        )

        // ✅ Tema rosa
        val PINK = CustomThemeColors(
            logoColor = Color(0xFFFF4081),
            buttonColor = Color(0xFFFF4081),
            backgroundColor = Color(0xFFFFF4F7),
            textColor = Color(0xFF5D4037),
            surfaceColor = Color.White,
            accentColor = Color(0xFFFF80AB),
        )

        // ✅ Tema azul
        val BLUE = CustomThemeColors(
            logoColor = Color(0xFF2196F3),
            buttonColor = Color(0xFF2196F3),
            backgroundColor = Color(0xFFF0F8FF),
            textColor = Color.Black,
            surfaceColor = Color.White,
            accentColor = Color(0xFF42A5F5),
        )

        // ✅ Tema morado
        val PURPLE = CustomThemeColors(
            logoColor = Color(0xFF9C27B0),
            buttonColor = Color(0xFF9C27B0),
            backgroundColor = Color(0xFFF3E5F5),
            textColor = Color(0xFF4A148C),
            surfaceColor = Color.White,
            accentColor = Color(0xFFE1BEE7),
        )
    }
}

// ✅ Función auxiliar para saber si un color es oscuro
fun Color.isDark(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5
}