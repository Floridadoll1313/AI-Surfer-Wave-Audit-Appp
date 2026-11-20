package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AuditExtendedColors(
    val success: Color,
    val successContainer: Color,
    val performance: Color,
    val performanceContainer: Color,
    val accessibility: Color,
    val accessibilityContainer: Color,
    val bestPractices: Color,
    val bestPracticesContainer: Color,
    val seo: Color,
    val seoContainer: Color,
    val cardBorder: Color,
)

private val LightExtendedColors = AuditExtendedColors(
    success = EmeraldSuccess,
    successContainer = EmeraldContainerLight,
    performance = PerfAmber,
    performanceContainer = PerfAmberContainerLight,
    accessibility = A11yTeal,
    accessibilityContainer = A11yTealContainerLight,
    bestPractices = BestPracticeBlue,
    bestPracticesContainer = BestPracticeBlueContainerLight,
    seo = SeoIndigo,
    seoContainer = SeoIndigoContainerLight,
    cardBorder = LightOutline,
)

private val DarkExtendedColors = AuditExtendedColors(
    success = EmeraldSuccessBright,
    successContainer = EmeraldContainerDark,
    performance = PerfAmberBright,
    performanceContainer = PerfAmberContainerDark,
    accessibility = A11yTealBright,
    accessibilityContainer = A11yTealContainerDark,
    bestPractices = BestPracticeBlueBright,
    bestPracticesContainer = BestPracticeBlueContainerDark,
    seo = SeoIndigoBright,
    seoContainer = SeoIndigoContainerDark,
    cardBorder = DarkOutline,
)

val LocalAuditColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = OceanCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = OceanCyanContainerLight,
    onPrimaryContainer = OceanNavyDeep,
    secondary = A11yTeal,
    onSecondary = Color.White,
    secondaryContainer = A11yTealContainerLight,
    onSecondaryContainer = A11yTealContainerDark,
    tertiary = SeoIndigo,
    onTertiary = Color.White,
    tertiaryContainer = SeoIndigoContainerLight,
    onTertiaryContainer = SeoIndigoContainerDark,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralContainerDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = OceanCyanBright,
    onPrimary = OceanNavyDeep,
    primaryContainer = OceanCyanContainerDark,
    onPrimaryContainer = OceanCyanContainerLight,
    secondary = A11yTealBright,
    onSecondary = OceanNavyDeep,
    secondaryContainer = A11yTealContainerDark,
    onSecondaryContainer = A11yTealContainerLight,
    tertiary = SeoIndigoBright,
    onTertiary = OceanNavyDeep,
    tertiaryContainer = SeoIndigoContainerDark,
    onTertiaryContainer = SeoIndigoContainerLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = CoralErrorBright,
    onError = OceanNavyDeep,
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralContainerLight,
)

val SurferShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun SurferAuditTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalAuditColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = SurferShapes,
            content = content,
        )
    }
}
