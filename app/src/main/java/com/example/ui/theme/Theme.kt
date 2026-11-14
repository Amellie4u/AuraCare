package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AuraCareLightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = SageSecondary,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    error = CoralEmergency,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = OnCoralContainer,
    background = CreamBackground,
    onBackground = SlateInk,
    surface = PureSurface,
    onSurface = SlateInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = SubtleOutline
)

private val AuraCareDarkColors = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = Color(0xFF003735),
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = Color(0xFFB4F2EC),
    secondary = Color(0xFF9AC2BE),
    onSecondary = Color(0xFF152E2C),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInk,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFF5C3409),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = DarkCoral,
    onError = Color(0xFF450A0A),
    errorContainer = DarkCoralContainer,
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkCreamBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF3A4E4C)
)

val AuraCareShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AuraCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AuraCareDarkColors else AuraCareLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraCareTypography,
        shapes = AuraCareShapes,
        content = content
    )
}
