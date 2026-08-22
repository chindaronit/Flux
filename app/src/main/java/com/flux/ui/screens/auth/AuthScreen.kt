package com.flux.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.BiometricAuthenticator

@Composable
fun AuthScreen(
    navController: NavController,
    isBiometricEnabled: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as FragmentActivity
    val showAuth = remember { mutableStateOf(true) }
    val authFailed = stringResource(R.string.Auth_Error)

    fun navigateToWorkspace() {
        showAuth.value = false

        navController.navigate(NavRoutes.Workspace.route) {
            popUpTo(NavRoutes.AuthScreen.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun startAuthentication() {
        val biometricAuthenticator = BiometricAuthenticator(
            activity = activity,
            onSuccess = {
                navigateToWorkspace()
            },
            onError = {},
            onFailed = {
                Toast.makeText(
                    context,
                    authFailed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        if (biometricAuthenticator.isAvailable()) {
            biometricAuthenticator.authenticate()
        } else {
            navigateToWorkspace()
        }
    }

    LaunchedEffect(isBiometricEnabled) {
        if (!isBiometricEnabled) {
            navController.navigate(NavRoutes.StorageSelection.route) {
                popUpTo(NavRoutes.AuthScreen.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    if (isBiometricEnabled && showAuth.value) {
        LaunchedEffect(Unit) {
            startAuthentication()
        }

        AuthContent(
            onAuthenticate = {
                startAuthentication()
            }
        )
    }
}
@Composable
private fun AuthContent(
    onAuthenticate: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
    }

    /*
     * ------------------------------------------------------------
     * Ambient background
     * ------------------------------------------------------------
     */
    val ambientTransition = rememberInfiniteTransition(
        label = "auth_ambient"
    )

    val gradientProgress by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth_gradient_progress"
    )

    /*
     * ------------------------------------------------------------
     * Flux gradient animation
     * ------------------------------------------------------------
     *
     * This is the same animated gradient used for the FLUX
     * wordmark. It continuously sweeps across the text.
     */
    val brandingTransition = rememberInfiniteTransition(
        label = "auth_branding"
    )

    val textGradientProgress by brandingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "auth_text_gradient"
    )

    val gradientWidth = 900f
    val gradientStart = textGradientProgress * gradientWidth

    val fluxGradient = Brush.linearGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.error,
            colorScheme.secondary,
            colorScheme.primary
        ),
        start = Offset(
            x = gradientStart - gradientWidth,
            y = 0f
        ),
        end = Offset(
            x = gradientStart,
            y = 0f
        )
    )

    /*
     * ------------------------------------------------------------
     * Fingerprint animation
     * ------------------------------------------------------------
     */
    val biometricTransition = rememberInfiniteTransition(
        label = "auth_biometric"
    )

    val fingerprintPulse by biometricTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fingerprint_pulse"
    )

    val fingerprintGlow by biometricTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fingerprint_glow"
    )

    /*
     * ------------------------------------------------------------
     * Screen
     * ------------------------------------------------------------
     */
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.14f),
                            colorScheme.tertiary.copy(alpha = 0.08f),
                            colorScheme.surfaceContainerLow
                        ),
                        center = Offset(
                            x = 350f + (500f * gradientProgress),
                            y = 100f
                        ),
                        radius = 1000f
                    )
                )
                .padding(innerPadding)
        ) {

            /*
             * ----------------------------------------------------
             * Ambient glow at the top
             * ----------------------------------------------------
             */
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-120).dp)
                    .alpha(0.18f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorScheme.primary,
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                /*
                 * ====================================================
                 * WELCOME HEADLINE
                 * ====================================================
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 800,
                            delayMillis = 300
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 800,
                            delayMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffsetY = { it / 2 }
                    )
                ) {

                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.welcome_back)+"\n")

                            pushStyle(
                                SpanStyle(
                                    brush = fluxGradient,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-2).sp
                                )
                            )

                            append("FLUX")

                            pop()
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                    )
                }

                /*
                 * ----------------------------------------------------
                 * Description
                 * ----------------------------------------------------
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 450
                        )
                    )
                ) {

                    Text(
                        text = stringResource(
                            R.string.Auth_with_Biometric
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = 20.dp
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(26.dp)
                )

                /*
                 * ====================================================
                 * BIOMETRIC VISUAL
                 * ====================================================
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 550
                        )
                    ) + scaleIn(
                        initialScale = 0.75f,
                        animationSpec = tween(
                            durationMillis = 800,
                            delayMillis = 550,
                            easing = FastOutSlowInEasing
                        )
                    )
                ) {

                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        /*
                         * Outer glow
                         */
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(fingerprintPulse)
                                .alpha(fingerprintGlow)
                                .background(
                                    color = colorScheme.primary,
                                    shape = CircleShape
                                )
                        )

                        /*
                         * Secondary ring
                         */
                        Box(
                            modifier = Modifier
                                .size(118.dp)
                                .border(
                                    width = 1.dp,
                                    color = colorScheme.primary.copy(
                                        alpha = 0.30f
                                    ),
                                    shape = CircleShape
                                )
                        )

                        /*
                         * Fingerprint surface
                         */
                        Surface(
                            modifier = Modifier.size(92.dp),
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            tonalElevation = 6.dp,
                            shadowElevation = 4.dp
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                /*
                 * ====================================================
                 * SECURITY CARD
                 * ====================================================
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 700
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 700,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {

                    SecurityInfoCard()
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                /*
                 * ====================================================
                 * AUTHENTICATE BUTTON
                 * ====================================================
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 850
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 850,
                            easing = FastOutSlowInEasing
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {

                    AuthButton(
                        onAuthenticate = onAuthenticate
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 1000
                        )
                    )
                ) {

                    Text(
                        text = stringResource(R.string.biometric_data_never_leaves),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthButton(
    onAuthenticate: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val transition = rememberInfiniteTransition(
        label = "auth_button"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth_button_glow"
    )

    val glowScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth_button_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(glowScale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(
                            alpha = glowAlpha
                        ),
                        colorScheme.tertiary.copy(
                            alpha = glowAlpha
                        )
                    )
                )
            )
            .padding(3.dp)
    ) {

        Button(
            onClick = onAuthenticate,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {

            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(23.dp)
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = stringResource(R.string.authenticate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SecurityInfoCard() {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.private_secure),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(R.string.biometric_unlock_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}