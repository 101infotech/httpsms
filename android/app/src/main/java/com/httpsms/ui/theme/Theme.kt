package com.httpsms.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Saubhagya Group family palette (shared with Griha Pravesh):
// cream canvas, one maroon primary, Himalayan gold accent.
val SgPrimary = Color(0xFFAB1F3D)
val SgPrimarySoft = Color(0xFFF3E3E7)
val SgCanvas = Color(0xFFFAF6F1)
val SgInk = Color(0xFF1F1A1C)
val SgMuted = Color(0xFF6E6467)
val SgGold = Color(0xFFF6D9A0)
val SgGoldSoft = Color(0xFFF5E7CC)
val SgGoldInk = Color(0xFF7A5410)
val SgSuccess = Color(0xFF0A7A4F)
val SgSuccessSoft = Color(0xFFE5F0EA)
val SgLine = Color(0xFFEFE7E1)

/** Colours that the Material scheme has no slot for. */
data class SgExtraColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val gold: Color,
    val onGold: Color,
)

private val LightExtras = SgExtraColors(
    hero = SgPrimary,
    onHero = Color.White,
    onHeroMuted = Color(0xFFF7D3DB),
    gold = SgGold,
    onGold = SgGoldInk,
)

private val DarkExtras = SgExtraColors(
    hero = Color(0xFF7A1730),
    onHero = Color.White,
    onHeroMuted = Color(0xFFF2C2CD),
    gold = SgGold,
    onGold = SgGoldInk,
)

val LocalSgColors = staticCompositionLocalOf { LightExtras }

private val LightColorScheme = lightColorScheme(
    primary = SgPrimary,
    onPrimary = Color.White,
    primaryContainer = SgPrimarySoft,
    onPrimaryContainer = SgPrimary,
    secondary = SgSuccess,
    onSecondary = Color.White,
    secondaryContainer = SgSuccessSoft,
    onSecondaryContainer = SgSuccess,
    tertiary = SgGoldInk,
    onTertiary = Color.White,
    tertiaryContainer = SgGoldSoft,
    onTertiaryContainer = SgGoldInk,
    background = SgCanvas,
    onBackground = SgInk,
    surface = Color.White,
    onSurface = SgInk,
    surfaceVariant = Color(0xFFF1ECE8),
    onSurfaceVariant = SgMuted,
    surfaceContainerHighest = Color(0xFFE6DDD8),
    outline = Color(0xFFD9CDC8),
    outlineVariant = SgLine,
    error = Color(0xFFC62828),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF28BA2),
    onPrimary = Color(0xFF3A0712),
    primaryContainer = Color(0xFF5A1426),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFF7FD4AC),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF0F3A28),
    onSecondaryContainer = Color(0xFF9EE0C0),
    tertiary = SgGold,
    onTertiary = Color(0xFF3F2B05),
    tertiaryContainer = Color(0xFF4A3510),
    onTertiaryContainer = SgGold,
    background = Color(0xFF161112),
    onBackground = Color(0xFFF3ECEE),
    surface = Color(0xFF221A1C),
    onSurface = Color(0xFFF3ECEE),
    surfaceVariant = Color(0xFF2E2427),
    onSurfaceVariant = Color(0xFFBFAFB3),
    surfaceContainerHighest = Color(0xFF3A2E31),
    outline = Color(0xFF5A4A4E),
    outlineVariant = Color(0xFF3A2E31),
    error = Color(0xFFFFB4AB),
)

@Composable
fun HttpSmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Brand colours are fixed: Material You dynamic colour is off on purpose.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) DarkExtras else LightExtras

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalSgColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
