package com.example.foodapp.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VAL_BRAND_PRIMARY,
    secondary = BrandSecondary,
    tertiary = VAL_BRAND_PRIMARY,
    background = VAL_BACKGROUND,
    surface = SurfaceWhite,
    onPrimary = VAL_BRAND_ON_PRIMARY,
    onSecondary = SurfaceWhite,
    onTertiary = VAL_BRAND_ON_PRIMARY,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BgMain,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = DividerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = VAL_BRAND_PRIMARY,
    secondary = BrandSecondary,
    tertiary = VAL_BRAND_PRIMARY,
    background = BgMainDark,
    surface = SurfaceDark,
    onPrimary = VAL_BRAND_ON_PRIMARY,
    onSecondary = SurfaceWhite,
    onTertiary = VAL_BRAND_ON_PRIMARY,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed,
    outline = DividerColorDark
)

@Composable
fun FoodAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to keep brand identity
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
