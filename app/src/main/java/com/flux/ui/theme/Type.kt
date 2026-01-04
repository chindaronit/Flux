package com.flux.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flux.R

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.poppins_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

val Sansation = FontFamily(
    Font(R.font.sansation_light, FontWeight.Light),
    Font(R.font.sansation_regular, FontWeight.Normal),
    Font(R.font.sansation_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.sansation_bold, FontWeight.Bold),
    Font(R.font.sansation_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.sansation_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

val Newsreader = FontFamily(
    Font(R.font.newsreader_light, FontWeight.Light),                        // Light
    Font(R.font.newsreader_regular, FontWeight.Normal),                     // Regular
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),    // Italic
    Font(R.font.newsreader_bold, FontWeight.Bold),                          // Bold
    Font(R.font.newsreader_bold_italic, FontWeight.Bold, FontStyle.Italic),   // BoldItalic
    Font(R.font.newsreader_light_italic, FontWeight.Light, FontStyle.Italic),
)

val OpenSans = FontFamily(
    Font(R.font.opensans_light, FontWeight.Light),                        // Light
    Font(R.font.opensans_regular, FontWeight.Normal),                     // Regular
    Font(R.font.opensans_italic, FontWeight.Normal, FontStyle.Italic),    // Italic
    Font(R.font.opensans_bold, FontWeight.Bold),                          // Bold
    Font(R.font.opensans_bold_italic, FontWeight.Bold, FontStyle.Italic),   // BoldItalic
    Font(R.font.opensans_light_italic, FontWeight.Light, FontStyle.Italic),
)

val Quantico = FontFamily(
    Font(R.font.quantico_regular, FontWeight.Light),                        // Light
    Font(R.font.quantico_regular, FontWeight.Normal),                     // Regular
    Font(R.font.quantico_italic, FontWeight.Normal, FontStyle.Italic),    // Italic
    Font(R.font.quantico_bold, FontWeight.Bold),                          // Bold
    Font(R.font.quantico_bold_italic, FontWeight.Bold, FontStyle.Italic),   // BoldItalic
    Font(R.font.quantico_italic, FontWeight.Light, FontStyle.Italic),
)

fun createTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 47.sp, lineHeight = 54.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 35.sp, lineHeight = 42.sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),

        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),

        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),

        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 0.35.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.15.sp),

        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.25.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.05.sp)
    )
}

