package com.flux.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.ensureStorageRoot
import com.flux.ui.viewModel.SettingsViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp

@Composable
fun StorageSelectionScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    isStorageRootSelected: Boolean
) {
    LaunchedEffect(isStorageRootSelected) {
        if (isStorageRootSelected) {
            navController.navigate(NavRoutes.Workspace.route) {
                popUpTo(NavRoutes.StorageSelection.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    val rootPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        settingsViewModel.saveRootUri(uri)
    }

    val scope = rememberCoroutineScope()

    if (!isStorageRootSelected) {
        StorageSelectionContent(
            onSelectFolder = {
                ensureStorageRoot(
                    scope,
                    settingsViewModel,
                    rootPicker
                ) {
                    navController.navigate(NavRoutes.Workspace.route) {
                        popUpTo(NavRoutes.StorageSelection.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

@Composable
private fun StorageSelectionContent(
    onSelectFolder: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "ambient_background"
    )

    val gradientProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_progress"
    )

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
        ) {

            /*
             * Ambient glow at the top.
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
                    .padding(innerPadding)
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                /*
                * Flux branding
                */
                val brandingTransition = rememberInfiniteTransition(
                    label = "flux_branding"
                )

                // Gentle breathing scale for the logo badge
                val logoPulse by brandingTransition.animateFloat(
                    initialValue = 0.96f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2200,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_pulse"
                )

                // Vertical float/bob
                val logoFloat by brandingTransition.animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2600,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_float"
                )

                // Subtle tilt wobble for a livelier feel
                val logoTilt by brandingTransition.animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 3400,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_tilt"
                )

                // Halo breathing
                val haloAlpha by brandingTransition.animateFloat(
                    initialValue = 0.10f,
                    targetValue = 0.30f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1800,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_halo"
                )

                // Continuously spinning ring behind the badge — one direction, no reverse
                val ringRotation by brandingTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 7000,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ring_rotation"
                )

                /*
                 * Animated gradient for the Flux wordmark.
                 *
                 * The gradient sweeps continuously across the letters using
                 * RepeatMode.Restart. The color list starts and ends on the
                 * same color (primary) so the loop point is seamless — no
                 * visible jump when it restarts.
                 */
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
                    label = "flux_text_gradient"
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

                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        animationSpec = tween(700)
                    ) + scaleIn(
                        initialScale = 0.72f,
                        animationSpec = tween(
                            durationMillis = 900,
                            easing = FastOutSlowInEasing
                        )
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        /*
                         * Logo halo + spinning ring
                         */
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                colorScheme.primary.copy(
                                                    alpha = haloAlpha
                                                ),
                                                colorScheme.tertiary.copy(
                                                    alpha = haloAlpha * 0.45f
                                                ),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = size.minDimension / 2f
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {

                            // Spinning sweep ring sitting behind the badge
                            Box(
                                modifier = Modifier
                                    .size(126.dp)
                                    .graphicsLayer {
                                        rotationZ = ringRotation
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                colorScheme.primary.copy(alpha = 0.55f),
                                                colorScheme.tertiary.copy(alpha = 0.05f),
                                                colorScheme.secondary.copy(alpha = 0.55f),
                                                colorScheme.primary.copy(alpha = 0.05f),
                                                colorScheme.primary.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )

                            /*
                             * Animated logo container
                             */
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .graphicsLayer {
                                        scaleX = logoPulse
                                        scaleY = logoPulse
                                        translationY = logoFloat
                                        rotationZ = logoTilt
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                colorScheme.primary,
                                                colorScheme.secondary,
                                                colorScheme.tertiary
                                            )
                                        )
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = colorScheme.surface
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                R.mipmap.ic_launcher_foreground
                                            ),
                                            contentDescription = stringResource(
                                                R.string.app_name
                                            ),
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(92.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        /*
                         * Flux wordmark
                         */
                        AnimatedVisibility(
                            visible = started,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 700,
                                    delayMillis = 180
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 180,
                                    easing = FastOutSlowInEasing
                                ),
                                initialOffsetY = { it / 3 }
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-2).sp,
                                    brush = fluxGradient
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                /*
                 * Hero headline
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        tween(
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
                            append("Your data.\n")

                            pushStyle(
                                SpanStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            colorScheme.primary,
                                            colorScheme.secondary,
                                            colorScheme.tertiary
                                        )
                                    ),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )

                            append("Your storage.")

                            pop()
                        },
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                /*
                 * Description
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        tween(
                            durationMillis = 700,
                            delayMillis = 450
                        )
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.select_folder_description
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                /*
                 * Benefits
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        tween(
                            durationMillis = 700,
                            delayMillis = 600
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 600
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    StorageBenefitsCard()
                }

                Spacer(modifier = Modifier.height(28.dp))

                /*
                 * CTA
                 */
                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        tween(
                            durationMillis = 700,
                            delayMillis = 750
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = 750
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    StorageSelectionButton(
                        onClick = onSelectFolder
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = started,
                    enter = fadeIn(
                        tween(
                            durationMillis = 500,
                            delayMillis = 900
                        )
                    )
                ) {
                    Text(
                        text = "You can change this later in Settings",
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
private fun StorageBenefitsCard() {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StorageBenefitRow(
                icon = Icons.Default.Folder,
                title = "You choose the location",
                description = "Flux stores your data where you decide."
            )

            StorageBenefitRow(
                icon = Icons.Default.Lock,
                title = "Your data stays yours",
                description = "No cloud account is required for your local data."
            )

            StorageBenefitRow(
                icon = Icons.Default.Security,
                title = "Easy to back up",
                description = "Your storage folder can be backed up like any other file."
            )
        }
    }
}

@Composable
private fun StorageBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StorageSelectionButton(
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val infiniteTransition = rememberInfiniteTransition(
        label = "button_glow"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(glowScale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = glowAlpha),
                        colorScheme.tertiary.copy(alpha = glowAlpha)
                    )
                )
            )
            .padding(3.dp)
    ) {
        Button(
            onClick = onClick,
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
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.select_folder),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}