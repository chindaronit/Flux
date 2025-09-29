package com.flux.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.flux.ui.state.Settings

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun getColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    amoledScreen: Boolean,
    contrast: Int,
    themeNumber: Int
): ColorScheme {
    val context = LocalContext.current

    return when {
        dynamicColor -> {
            val base =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledScreen) base.copy(
                surface = Color.Black,
                surfaceContainerLow = Color.Black
            ) else base
        }

        else -> {
            val base = when (contrast) {
                1 -> if (darkTheme) mediumContrastDarkColorSchemes[themeNumber] else mediumContrastLightColorSchemes[themeNumber]
                2 -> if (darkTheme) highContrastDarkColorSchemes[themeNumber] else highContrastLightColorSchemes[themeNumber]
                else -> if (darkTheme) darkSchemes[themeNumber] else lightSchemes[themeNumber]
            }

            if (darkTheme && amoledScreen) base.copy(
                surface = Color.Black,
                surfaceContainerLow = Color.Black
            ) else base
        }
    }
}

val darkSchemes = listOf(
    theme1DarkScheme,
    theme2DarkScheme,
    theme3DarkScheme,
    theme4DarkScheme,
    theme5DarkScheme,
    theme6DarkScheme
)

val lightSchemes = listOf(
    theme1LightScheme,
    theme2LightScheme,
    theme3LightScheme,
    theme4LightScheme,
    theme5LightScheme,
    theme6LightScheme
)

val mediumContrastLightColorSchemes = listOf(
    theme1MediumContrastLightColorScheme,
    theme2MediumContrastLightColorScheme,
    theme3MediumContrastLightColorScheme,
    theme4MediumContrastLightColorScheme,
    theme5MediumContrastLightColorScheme,
    theme6MediumContrastLightColorScheme
)

val mediumContrastDarkColorSchemes = listOf(
    theme1MediumContrastDarkColorScheme,
    theme2MediumContrastDarkColorScheme,
    theme3MediumContrastDarkColorScheme,
    theme4MediumContrastDarkColorScheme,
    theme5MediumContrastDarkColorScheme,
    theme6MediumContrastDarkColorScheme
)

val highContrastLightColorSchemes = listOf(
    theme1HighContrastLightColorScheme,
    theme2HighContrastLightColorScheme,
    theme3HighContrastLightColorScheme,
    theme4HighContrastLightColorScheme,
    theme5HighContrastLightColorScheme,
    theme6HighContrastLightColorScheme
)

val highContrastDarkColorSchemes = listOf(
    theme1HighContrastDarkColorScheme,
    theme2HighContrastDarkColorScheme,
    theme3HighContrastDarkColorScheme,
    theme4HighContrastDarkColorScheme,
    theme5HighContrastDarkColorScheme,
    theme6HighContrastDarkColorScheme
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluxTheme(
    settings: Settings,
    content: @Composable () -> Unit
) {
    val data = settings.data

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when {
        isSystemInDarkTheme && data.isAutomaticTheme -> true
        !isSystemInDarkTheme && data.isAutomaticTheme -> false
        else -> data.isDarkMode
    }

    val contrast = settings.data.contrast
    val amoledTheme = settings.data.amoledTheme
    val themeNumber = settings.data.themeNumber
    val activity = LocalView.current.context as Activity
    WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
        isAppearanceLightStatusBars = !darkTheme
    }

    if (data.isScreenProtection) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }


    MaterialTheme(
        colorScheme = getColorScheme(darkTheme, data.dynamicTheme, amoledTheme, contrast, themeNumber),
        typography = AppTypography,
        content = content
    )
}
