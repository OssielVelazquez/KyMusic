package com.kyoten.kymusic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== TEMA OSCURO (DEFAULT - EL QUE YA TENÍAS) ====================
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF7C4DFF),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB39DDB),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF00BCD4),
    onTertiary = Color.Black,
    background = Color(0xFF0A0A0F),
    onBackground = Color.White,
    surface = Color(0xFF1E1E2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D3A),
    onSurfaceVariant = Color(0xFFB0B0C0),
    surfaceTint = Color(0xFF00E5FF),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color.White,
    outline = Color(0xFF404048),
    outlineVariant = Color(0xFF303038),
    scrim = Color.Black,
    inversePrimary = Color(0xFF90CAF9)
)

// ==================== TEMA CLARO ====================
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00BCD4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7C4DFF),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB39DDB),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF00ACC1),
    onTertiary = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF404040),
    surfaceTint = Color(0xFF00BCD4),
    inverseSurface = Color(0xFF1E1E2E),
    inverseOnSurface = Color.White,
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFCF6679),
    onErrorContainer = Color.Black,
    outline = Color(0xFFC0C0C0),
    outlineVariant = Color(0xFFD0D0D0),
    scrim = Color.Black,
    inversePrimary = Color(0xFF00BCD4)
)

// ==================== TEMA ROSA ====================
private val PinkColorScheme = lightColorScheme(
    primary = Color(0xFFFF4081),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFF80AB),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE91E63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color.Black,
    background = Color(0xFFFFF4F7),
    onBackground = Color(0xFF5D4037),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF5D4037),
    surfaceVariant = Color(0xFFFFF0F3),
    onSurfaceVariant = Color(0xFF8B6B61),
    surfaceTint = Color(0xFFFF4081),
    inverseSurface = Color(0xFF1E1E2E),
    inverseOnSurface = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFCF6679),
    onErrorContainer = Color.Black,
    outline = Color(0xFFFFCDD2),
    outlineVariant = Color(0xFFFFE4E7),
    scrim = Color.Black,
    inversePrimary = Color(0xFFFF80AB)
)

// ==================== TEMA AZUL ====================
private val BlueColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF42A5F5),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF1976D2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF03A9F4),
    onTertiary = Color.Black,
    background = Color(0xFFF0F8FF),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF546E7A),
    surfaceTint = Color(0xFF2196F3),
    inverseSurface = Color(0xFF1E1E2E),
    inverseOnSurface = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFCF6679),
    onErrorContainer = Color.Black,
    outline = Color(0xFFBBDEFB),
    outlineVariant = Color(0xFFE3F2FD),
    scrim = Color.Black,
    inversePrimary = Color(0xFF42A5F5)
)

// ==================== TEMA MORADO ====================
private val PurpleColorScheme = lightColorScheme(
    primary = Color(0xFF9C27B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF7B1FA2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1C4E9),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color.Black,
    background = Color(0xFFF3E5F5),
    onBackground = Color(0xFF4A148C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4A148C),
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF6A1B9A),
    surfaceTint = Color(0xFF9C27B0),
    inverseSurface = Color(0xFF1E1E2E),
    inverseOnSurface = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFCF6679),
    onErrorContainer = Color.Black,
    outline = Color(0xFFCE93D8),
    outlineVariant = Color(0xFFE1BEE7),
    scrim = Color.Black,
    inversePrimary = Color(0xFFCE93D8)
)

// ==================== TIPOS DE TEMA ====================
enum class AppTheme {
    DARK, LIGHT, PINK, BLUE, PURPLE
}

// ==================== FUNCIÓN PRINCIPAL ====================
@Composable
fun KyMusicTheme(
    theme: AppTheme = AppTheme.DARK,
    customColors: CustomThemeColors? = null,  // ✅ NUEVO: Colores personalizados
    content: @Composable () -> Unit
) {
    // ✅ Si hay colores personalizados, usarlos. Si no, usar el tema predefinido
    val colorScheme = if (customColors != null) {
        lightColorScheme(
            primary = customColors.buttonColor,
            onPrimary = if (customColors.buttonColor.isDark()) Color.White else Color.Black,
            primaryContainer = customColors.accentColor,
            onPrimaryContainer = if (customColors.accentColor.isDark()) Color.White else Color.Black,
            secondary = customColors.logoColor,
            onSecondary = if (customColors.logoColor.isDark()) Color.White else Color.Black,
            secondaryContainer = customColors.logoColor.copy(alpha = 0.3f),
            onSecondaryContainer = if (customColors.logoColor.isDark()) Color.White else Color.Black,
            tertiary = customColors.accentColor,
            onTertiary = if (customColors.accentColor.isDark()) Color.White else Color.Black,
            background = customColors.backgroundColor,
            onBackground = customColors.textColor,
            surface = customColors.surfaceColor,
            onSurface = customColors.textColor,
            surfaceVariant = customColors.surfaceColor.copy(alpha = 0.7f),
            onSurfaceVariant = customColors.textColor.copy(alpha = 0.7f),
            surfaceTint = customColors.buttonColor,
            inverseSurface = customColors.textColor,
            inverseOnSurface = customColors.backgroundColor,
            error = Color(0xFFCF6679),
            onError = Color.Black,
            errorContainer = Color(0xFFB00020),
            onErrorContainer = Color.White,
            outline = customColors.textColor.copy(alpha = 0.3f),
            outlineVariant = customColors.textColor.copy(alpha = 0.15f),
            scrim = Color.Black,
            inversePrimary = customColors.buttonColor.copy(alpha = 0.7f)
        )
    } else {
        when (theme) {
            AppTheme.DARK -> DarkColorScheme
            AppTheme.LIGHT -> LightColorScheme
            AppTheme.PINK -> PinkColorScheme
            AppTheme.BLUE -> BlueColorScheme
            AppTheme.PURPLE -> PurpleColorScheme
        }
    }

    // ✅ Color de la barra de estado
    val barColor = if (customColors != null) {
        customColors.backgroundColor
    } else {
        when (theme) {
            AppTheme.DARK -> Color(0xFF0A0A0F)
            AppTheme.LIGHT -> Color(0xFFF5F5F5)
            AppTheme.PINK -> Color(0xFFFFF4F7)
            AppTheme.BLUE -> Color(0xFFF0F8FF)
            AppTheme.PURPLE -> Color(0xFFF3E5F5)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !barColor.isDark()

            if (Build.VERSION.SDK_INT >= 36) {
                insetsController.isAppearanceLightNavigationBars = !barColor.isDark()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}