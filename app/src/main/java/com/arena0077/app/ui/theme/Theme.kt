package com.arena0077.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ArenaPurple,
    onPrimary = ArenaTextPrimary,
    primaryContainer = ArenaPurpleDark,
    onPrimaryContainer = ArenaTextPrimary,
    secondary = ArenaGreen,
    onSecondary = ArenaTextPrimary,
    secondaryContainer = ArenaGreen,
    onSecondaryContainer = ArenaTextPrimary,
    tertiary = ArenaPurpleDark,
    onTertiary = ArenaTextPrimary,
    background = ArenaBgDark,
    onBackground = ArenaTextPrimary,
    surface = ArenaSurfaceDark,
    onSurface = ArenaTextPrimary,
    surfaceVariant = ArenaCardDark,
    onSurfaceVariant = ArenaTextSecondary,
    surfaceTint = ArenaPurple,
    inverseSurface = ArenaCardDark,
    inverseOnSurface = ArenaTextPrimary,
    error = ArenaError,
    onError = ArenaTextPrimary,
    errorContainer = ArenaError,
    onErrorContainer = ArenaTextPrimary,
    outline = ArenaDividerDark,
    outlineVariant = ArenaDividerDark,
    scrim = ArenaBgDark
)

private val LightColorScheme = lightColorScheme(
    primary = ArenaPurple,
    onPrimary = ArenaTextPrimary,
    secondary = ArenaGreen,
    onSecondary = ArenaTextPrimary,
    background = ArenaBgLight,
    onBackground = ArenaTextPrimaryLight,
    surface = ArenaSurfaceLight,
    onSurface = ArenaTextPrimaryLight,
    surfaceVariant = ArenaCardLight,
    onSurfaceVariant = ArenaTextSecondaryLight,
    error = ArenaError,
    outline = ArenaDividerLight
)

@Composable
fun ArenaTheme(
    darkTheme: Boolean = true,  // arena.ai defaults to dark
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = ArenaTypography,
        content = content
    )
}
