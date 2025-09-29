package com.flux.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

// Common constants
private const val DEFAULT_FADE_DURATION = 300
private const val DEFAULT_SCALE_DURATION = 400
private const val DEFAULT_SLIDE_DURATION = 400
private const val DEFAULT_INITIAL_SCALE = 0.8f

// --- Fade + Scale (Default) ---
fun defaultScreenEnterAnimation(): EnterTransition {
    return fadeIn(animationSpec = tween(DEFAULT_FADE_DURATION)) +
            scaleIn(
                initialScale = DEFAULT_INITIAL_SCALE,
                animationSpec = tween(DEFAULT_SCALE_DURATION)
            )
}

fun defaultScreenExitAnimation(): ExitTransition {
    return fadeOut(animationSpec = tween(DEFAULT_FADE_DURATION)) +
            scaleOut(
                targetScale = 1f,
                animationSpec = tween(DEFAULT_SCALE_DURATION)
            )
}

// --- Slide Left / Right ---
fun slideScreenEnterAnimation(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DEFAULT_SLIDE_DURATION)
    )
}

fun slideScreenExitAnimation(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DEFAULT_SLIDE_DURATION)
    )
}

// --- Slide From/To Bottom ---
fun slideFromBottomEnter(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(300)
    )
}

fun slideToBottomExit(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(300)
    )
}
