package com.tbterminal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DDBB3),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF00513C),
    onPrimaryContainer = Color(0xFF8AF8CF),
    secondary = Color(0xFFB4CCC0),
    tertiary = Color(0xFFFFC04C),
    background = TbDarkBackground,
    onBackground = TbDarkText,
    surface = TbDarkSurface,
    onSurface = TbDarkText,
    surfaceVariant = TbDarkSurfaceMuted,
    onSurfaceVariant = Color(0xFFC1CDC6),
    outline = Color(0xFF899790),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = TbGreen,
    onPrimary = Color.White,
    primaryContainer = TbGreenLight,
    onPrimaryContainer = TbGreenDark,
    secondary = Color(0xFF53645B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE7E1),
    onSecondaryContainer = Color(0xFF0A3527),
    tertiary = TbAmber,
    onTertiary = Color.White,
    tertiaryContainer = TbAmberLight,
    onTertiaryContainer = Color(0xFF3B2600),
    background = TbBackground,
    onBackground = TbText,
    surface = TbSurface,
    onSurface = TbText,
    surfaceVariant = TbSurfaceMuted,
    onSurfaceVariant = TbTextMuted,
    outline = Color(0xFF77827C),
    outlineVariant = TbOutline,
    error = TbError
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TbterminalappTheme(
    darkTheme: Boolean = false,
    // Disabled by default so brand and status colors stay predictable on every device.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
