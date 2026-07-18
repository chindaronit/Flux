package com.flux.ui.theme

import com.flux.R

/**
 * Maps the filenames referenced in WebFontConfig (used to build @font-face CSS)
 * to the actual compiled font resource IDs in res/font.
 *
 * Used by ReadView's WebViewClient.shouldInterceptRequest to serve font bytes
 * directly from resources.openRawResource(resId), since res/font is not reachable
 * via file:///android_asset/ paths.
 */
object FontResourceMap {
    val byFileName: Map<String, Int> = mapOf(
        // Poppins
        "poppins_light.ttf" to R.font.poppins_light,
        "poppins_regular.ttf" to R.font.poppins_regular,
        "poppins_italic.ttf" to R.font.poppins_italic,
        "poppins_bold.ttf" to R.font.poppins_bold,
        "poppins_light_italic.ttf" to R.font.poppins_light_italic,
        "poppins_bold_italic.ttf" to R.font.poppins_bold_italic,

        // Sansation
        "sansation_light.ttf" to R.font.sansation_light,
        "sansation_regular.ttf" to R.font.sansation_regular,
        "sansation_italic.ttf" to R.font.sansation_italic,
        "sansation_bold.ttf" to R.font.sansation_bold,
        "sansation_light_italic.ttf" to R.font.sansation_light_italic,
        "sansation_bold_italic.ttf" to R.font.sansation_bold_italic,

        // Newsreader
        "newsreader_light.ttf" to R.font.newsreader_light,
        "newsreader_regular.ttf" to R.font.newsreader_regular,
        "newsreader_italic.ttf" to R.font.newsreader_italic,
        "newsreader_bold.ttf" to R.font.newsreader_bold,
        "newsreader_bold_italic.ttf" to R.font.newsreader_bold_italic,
        "newsreader_light_italic.ttf" to R.font.newsreader_light_italic,

        // Open Sans
        "opensans_light.ttf" to R.font.opensans_light,
        "opensans_regular.ttf" to R.font.opensans_regular,
        "opensans_italic.ttf" to R.font.opensans_italic,
        "opensans_bold.ttf" to R.font.opensans_bold,
        "opensans_bold_italic.ttf" to R.font.opensans_bold_italic,
        "opensans_light_italic.ttf" to R.font.opensans_light_italic,

        // Quantico (no distinct light weight in this family per WebFontConfig)
        "quantico_regular.ttf" to R.font.quantico_regular,
        "quantico_italic.ttf" to R.font.quantico_italic,
        "quantico_bold.ttf" to R.font.quantico_bold,
        "quantico_bold_italic.ttf" to R.font.quantico_bold_italic
    )
}