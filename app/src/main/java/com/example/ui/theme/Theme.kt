package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandPrimary,
    onPrimary = Color(0xFF00354E),
    primaryContainer = DarkBrandPrimaryContainer,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF2DD4BF),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = BrandBlueOnPrimary,
    primaryContainer = BrandBluePrimaryContainer,
    onPrimaryContainer = BrandBlueOnPrimaryContainer,
    secondary = BrandNavySecondary,
    onSecondary = BrandNavyOnSecondary,
    secondaryContainer = BrandNavySecondaryContainer,
    onSecondaryContainer = BrandNavyOnSecondaryContainer,
    tertiary = BrandTealTertiary,
    onTertiary = BrandTealOnTertiary,
    tertiaryContainer = BrandTealTertiaryContainer,
    onTertiaryContainer = BrandTealOnTertiaryContainer,
    background = AppBackgroundLight,
    surface = AppSurfaceLight,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppTextSecondary,
    outline = AppOutlineLight,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
