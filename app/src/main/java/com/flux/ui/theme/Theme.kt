package com.flux.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowManager
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

@Composable
fun getColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    amoledScreen: Boolean,
    contrast: Int,
    themeNumber: Int
): ColorScheme {
    val context = LocalContext.current

    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (contrast) {
                1 -> if (darkTheme) mediumContrastDarkColorSchemes[themeNumber]
                else mediumContrastLightColorSchemes[themeNumber]
                2 -> if (darkTheme) highContrastDarkColorSchemes[themeNumber]
                else highContrastLightColorSchemes[themeNumber]
                else -> if (darkTheme) darkSchemes[themeNumber]
                else lightSchemes[themeNumber]
            }
        }
    }

    return if (darkTheme && amoledScreen) {
        baseScheme.copy(
            surface = Color.Black,
            surfaceContainerLow = Color.Black
        )
    } else baseScheme
}

private val darkSchemes = listOf(
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

private val mediumContrastLightColorSchemes = listOf(
    theme1MediumContrastLightColorScheme,
    theme2MediumContrastLightColorScheme,
    theme3MediumContrastLightColorScheme,
    theme4MediumContrastLightColorScheme,
    theme5MediumContrastLightColorScheme,
    theme6MediumContrastLightColorScheme
)

private val mediumContrastDarkColorSchemes = listOf(
    theme1MediumContrastDarkColorScheme,
    theme2MediumContrastDarkColorScheme,
    theme3MediumContrastDarkColorScheme,
    theme4MediumContrastDarkColorScheme,
    theme5MediumContrastDarkColorScheme,
    theme6MediumContrastDarkColorScheme
)

private val highContrastLightColorSchemes = listOf(
    theme1HighContrastLightColorScheme,
    theme2HighContrastLightColorScheme,
    theme3HighContrastLightColorScheme,
    theme4HighContrastLightColorScheme,
    theme5HighContrastLightColorScheme,
    theme6HighContrastLightColorScheme
)

private val highContrastDarkColorSchemes = listOf(
    theme1HighContrastDarkColorScheme,
    theme2HighContrastDarkColorScheme,
    theme3HighContrastDarkColorScheme,
    theme4HighContrastDarkColorScheme,
    theme5HighContrastDarkColorScheme,
    theme6HighContrastDarkColorScheme
)

val FLUX_FONT = listOf(
    Poppins,
    Sansation,
    Newsreader,
    OpenSans,
    Quantico

)

val FONTS = listOf(
    "Poppins",
    "Sansation",
    "Newsreader",
    "OpenSans",
    "Quantico"
)

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

    val contrast = data.contrast
    val amoledTheme = data.amoledTheme
    val themeNumber = data.themeNumber
    val fontNumber = data.fontNumber

    val activity = LocalView.current.context as Activity
    val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    insetsController.isAppearanceLightStatusBars = !darkTheme

    if (data.isScreenProtection) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    MaterialTheme(
        colorScheme = getColorScheme(
            darkTheme = darkTheme,
            dynamicColor = data.dynamicTheme,
            amoledScreen = amoledTheme,
            contrast = contrast,
            themeNumber = themeNumber
        ),
        typography = createTypography(FLUX_FONT[fontNumber]),
        content = content
    )
}