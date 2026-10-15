package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EarthColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1E1000),
    primaryContainer = AmberDark,
    onPrimaryContainer = Color(0xFFFFECC8),
    secondary = AmberLight,
    onSecondary = Color(0xFF281400),
    secondaryContainer = Color(0xFF472700),
    onSecondaryContainer = Color(0xFFFFDCB0),
    tertiary = RiskSafeGreen,
    onTertiary = Color(0xFF003010),
    background = EarthDark,
    onBackground = TextWarmPrimary,
    surface = EarthSurface,
    onSurface = TextWarmPrimary,
    surfaceVariant = EarthSurfaceVariant,
    onSurfaceVariant = TextWarmSecondary,
    outline = EarthBorder,
    outlineVariant = Color(0xFF2E2721),
    error = RiskDangerRed,
    onError = Color.White
)

@Composable
fun PhishGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EarthColorScheme,
        typography = Typography,
        content = content
    )
}
