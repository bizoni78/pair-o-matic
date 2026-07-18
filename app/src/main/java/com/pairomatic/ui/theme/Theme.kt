package com.pairomatic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// --- Paleta marki (żywa, spójna niezależnie od tapety systemu) ---
val BrandPurple = Color(0xFF7C3AED)
val BrandIndigo = Color(0xFF4F46E5)
val BrandPink = Color(0xFFEC4899)
val BrandTeal = Color(0xFF14B8A6)
val BrandBlue = Color(0xFF3B82F6)
val BrandAmber = Color(0xFFF59E0B)
val BrandGreen = Color(0xFF22C55E)
val BrandRed = Color(0xFFEF4444)

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF2A0E66),
    secondary = BrandPink,
    onSecondary = Color.White,
    tertiary = BrandTeal,
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1B1726),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1726),
    surfaceVariant = Color(0xFFEFEAF7),
    onSurfaceVariant = Color(0xFF5B5570)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB79CFF),
    onPrimary = Color(0xFF230A5B),
    primaryContainer = Color(0xFF3B2A78),
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = Color(0xFFF9A8D4),
    tertiary = Color(0xFF5EEAD4),
    background = Color(0xFF120F1C),
    onBackground = Color(0xFFEDE9F7),
    surface = Color(0xFF1A1626),
    onSurface = Color(0xFFEDE9F7),
    surfaceVariant = Color(0xFF2A2340),
    onSurfaceVariant = Color(0xFFC7BFDD)
)

// Przytulne, mocno zaokrąglone kształty (pola tekstowe, karty, menu, dialogi).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PairomaticTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Celowo bez dynamicznych kolorów (Material You) — chcemy spójny wygląd marki.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}

// --- Gradienty i kolory pomocnicze ---

/** Główny gradient marki (nagłówki, hero). */
fun brandGradient(): Brush = Brush.linearGradient(listOf(BrandIndigo, BrandPurple, BrandPink))

private val avatarPalette = listOf(
    BrandPurple, BrandPink, BrandTeal, BrandBlue, BrandAmber, BrandGreen, BrandIndigo
)

/** Stabilny kolor „awatara" dla danej pary liter (dla wizualnej różnorodności listy). */
fun letterColor(key: String): Color = avatarPalette[abs(key.hashCode()) % avatarPalette.size]

/** Gradient tła całej aplikacji (pod ekranami) — wyraźnie widoczny, ale pastelowy. */
fun appBackgroundGradient(darkTheme: Boolean): Brush = if (darkTheme) {
    Brush.verticalGradient(listOf(Color(0xFF2A1E4A), Color(0xFF171126), Color(0xFF2A1330)))
} else {
    Brush.verticalGradient(listOf(Color(0xFFD9C9FA), Color(0xFFE9DFFB), Color(0xFFF6D3E8)))
}
