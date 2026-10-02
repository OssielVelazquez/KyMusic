package com.kyoten.kymusic.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyoten.kymusic.ui.theme.CustomThemeColors
import com.kyoten.kymusic.ui.theme.isDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullColorPickerDialog(
    currentColors: CustomThemeColors,
    onColorSelected: (CustomThemeColors) -> Unit,
    onDismiss: () -> Unit
) {
    // ✅ Estado de cada color (solo los 4 que funcionan)
    var selectedButtonColor by remember { mutableStateOf(currentColors.buttonColor) }
    var selectedBackgroundColor by remember { mutableStateOf(currentColors.backgroundColor) }
    var selectedTextColor by remember { mutableStateOf(currentColors.textColor) }
    var selectedSurfaceColor by remember { mutableStateOf(currentColors.surfaceColor) }

    // ✅ PALETAS MEJORADAS - Colores que combinan bien entre sí
    // Cada paleta tiene: [Color principal, Fondo, Superficie, Texto]

    // Paleta 1: Cyan oscuro (la actual mejorada)
    val palette1 = listOf(
        Color(0xFF00E5FF), // Botones - Cyan brillante
        Color(0xFF0A0A0F), // Fondo - Negro profundo
        Color(0xFF1E1E2E), // Superficie - Gris oscuro
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 2: Morado elegante
    val palette2 = listOf(
        Color(0xFFBB86FC), // Botones - Morado claro
        Color(0xFF121212), // Fondo - Negro suave
        Color(0xFF1E1E1E), // Superficie - Gris
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 3: Rosa moderno
    val palette3 = listOf(
        Color(0xFFFF4081), // Botones - Rosa
        Color(0xFF1A0F14), // Fondo - Negro rosado
        Color(0xFF2D1F26), // Superficie - Gris rosado
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 4: Azul océano
    val palette4 = listOf(
        Color(0xFF4FC3F7), // Botones - Azul claro
        Color(0xFF0A1929), // Fondo - Azul oscuro
        Color(0xFF1E3A5F), // Superficie - Azul medio
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 5: Verde esmeralda
    val palette5 = listOf(
        Color(0xFF4CAF50), // Botones - Verde
        Color(0xFF0D1F12), // Fondo - Verde oscuro
        Color(0xFF1B3A1F), // Superficie - Verde medio
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 6: Naranja atardecer
    val palette6 = listOf(
        Color(0xFFFF9800), // Botones - Naranja
        Color(0xFF1F1408), // Fondo - Marrón oscuro
        Color(0xFF3D2814), // Superficie - Marrón medio
        Color(0xFFFFFFFF), // Texto - Blanco
    )

    // Paleta 7: Claro minimalista
    val palette7 = listOf(
        Color(0xFF2196F3), // Botones - Azul
        Color(0xFFF5F5F5), // Fondo - Gris claro
        Color(0xFFFFFFFF), // Superficie - Blanco
        Color(0xFF212121), // Texto - Negro
    )

    // Paleta 8: Claro cálido
    val palette8 = listOf(
        Color(0xFFE91E63), // Botones - Rosa
        Color(0xFFFFF8F0), // Fondo - Crema
        Color(0xFFFFFFFF), // Superficie - Blanco
        Color(0xFF3E2723), // Texto - Marrón oscuro
    )

    // ✅ Lista de paletas predefinidas con nombre
    val presetPalettes = listOf(
        "🌙 Oscuro Cyan" to palette1,
        "💜 Morado Elegante" to palette2,
        "🌸 Rosa Moderno" to palette3,
        "🌊 Azul Océano" to palette4,
        "🌿 Verde Esmeralda" to palette5,
        "🌅 Naranja Atardecer" to palette6,
        "☀️ Claro Minimalista" to palette7,
        "🍦 Claro Cálido" to palette8,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = currentColors.surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 650.dp),
        title = {
            Text(
                "🎨 Personalizar colores",
                color = currentColors.textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // ✅ Vista previa en vivo
                ColorPreview(
                    buttonColor = selectedButtonColor,
                    backgroundColor = selectedBackgroundColor,
                    textColor = selectedTextColor,
                    surfaceColor = selectedSurfaceColor
                )

                Spacer(Modifier.height(16.dp))

                // ✅ SECCIÓN 1: Paletas predefinidas (combinaciones que se ven bien)
                Text(
                    "🎨 Combinaciones recomendadas",
                    color = currentColors.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                presetPalettes.forEach { (name, colors) ->
                    val isSelected = selectedButtonColor == colors[0] &&
                            selectedBackgroundColor == colors[1] &&
                            selectedSurfaceColor == colors[2] &&
                            selectedTextColor == colors[3]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedButtonColor = colors[0]
                                selectedBackgroundColor = colors[1]
                                selectedSurfaceColor = colors[2]
                                selectedTextColor = colors[3]
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                currentColors.buttonColor.copy(alpha = 0.2f)
                            else
                                currentColors.surfaceColor.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(2.dp, currentColors.buttonColor)
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ Preview de la paleta (4 círculos)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                name,
                                color = currentColors.textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = currentColors.buttonColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ✅ SECCIÓN 2: Personalización individual
                Text(
                    "🎨 Personalizar individualmente",
                    color = currentColors.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                // ✅ Selector para BOTONES
                ColorPickerSection(
                    label = "🔘 Botones",
                    selectedColor = selectedButtonColor,
                    onColorChange = { selectedButtonColor = it },
                    colorPalette = colorPalette,
                    textColor = currentColors.textColor
                )

                Spacer(Modifier.height(12.dp))

                // ✅ Selector para FONDO
                ColorPickerSection(
                    label = "📱 Fondo",
                    selectedColor = selectedBackgroundColor,
                    onColorChange = { selectedBackgroundColor = it },
                    colorPalette = colorPalette,
                    textColor = currentColors.textColor
                )

                Spacer(Modifier.height(12.dp))

                // ✅ Selector para SUPERFICIE
                ColorPickerSection(
                    label = "🖼️ Superficies (tarjetas)",
                    selectedColor = selectedSurfaceColor,
                    onColorChange = { selectedSurfaceColor = it },
                    colorPalette = colorPalette,
                    textColor = currentColors.textColor
                )

                Spacer(Modifier.height(12.dp))

                // ✅ Selector para TEXTO
                ColorPickerSection(
                    label = "📝 Texto",
                    selectedColor = selectedTextColor,
                    onColorChange = { selectedTextColor = it },
                    colorPalette = colorPalette,
                    textColor = currentColors.textColor
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ✅ Botón de reinicio
                TextButton(
                    onClick = {
                        selectedButtonColor = CustomThemeColors.DARK.buttonColor
                        selectedBackgroundColor = CustomThemeColors.DARK.backgroundColor
                        selectedTextColor = CustomThemeColors.DARK.textColor
                        selectedSurfaceColor = CustomThemeColors.DARK.surfaceColor
                    }
                ) {
                    Text("↺ Reiniciar", color = currentColors.textColor)
                }

                Button(
                    onClick = {
                        val newColors = CustomThemeColors(
                            logoColor = selectedButtonColor, // Usamos el mismo que botones
                            buttonColor = selectedButtonColor,
                            backgroundColor = selectedBackgroundColor,
                            textColor = selectedTextColor,
                            surfaceColor = selectedSurfaceColor,
                            accentColor = selectedButtonColor, // Usamos el mismo que botones
                        )
                        onColorSelected(newColors)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedButtonColor,
                        contentColor = if (selectedButtonColor.isDark()) Color.White else Color.Black
                    )
                ) {
                    Text("Aplicar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = currentColors.textColor)
            }
        }
    )
}

// ✅ Paleta de colores disponibles para personalización individual
val colorPalette = listOf(
    // Colores vibrantes
    Color(0xFF00E5FF), Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF4FC3F7),
    Color(0xFF7C4DFF), Color(0xFFBB86FC), Color(0xFF9C27B0),
    Color(0xFFFF4081), Color(0xFFE91E63),
    Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFC107),
    Color(0xFF4CAF50), Color(0xFF009688), Color(0xFF8BC34A),

    // Neutros
    Color(0xFF000000), Color(0xFF0A0A0F), Color(0xFF121212),
    Color(0xFF1A1A1A), Color(0xFF1E1E1E), Color(0xFF2D2D2D),
    Color(0xFF424242), Color(0xFF757575), Color(0xFFBDBDBD),
    Color(0xFFF5F5F5), Color(0xFFFFFFFF),

    // Tonos pastel
    Color(0xFFFFF8F0), Color(0xFFE3F2FD), Color(0xFFF3E5F5),
    Color(0xFFE8F5E9), Color(0xFFFFF3E0),
)

@Composable
fun ColorPickerSection(
    label: String,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    colorPalette: List<Color>,
    textColor: Color
) {
    Column {
        Text(
            label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))

        // ✅ Grid de colores con scroll horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 3.dp else 1.dp,
                            color = if (selectedColor == color) Color.White else Color.Gray.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }
    }
}

@Composable
fun ColorPreview(
    buttonColor: Color,
    backgroundColor: Color,
    textColor: Color,
    surfaceColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // ✅ Fila superior: texto y botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Vista previa",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = if (buttonColor.isDark()) Color.White else Color.Black
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Botón", fontSize = 11.sp)
                }
            }

            // ✅ Fila inferior: superficie de muestra
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Canción de ejemplo",
                        color = textColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}