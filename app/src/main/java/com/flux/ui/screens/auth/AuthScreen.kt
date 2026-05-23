package com.flux.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.BiometricAuthenticator
import com.flux.ui.screens.settings.CircleWrapper

@Composable
fun AuthScreen(
    navController: NavController,
    isBiometricEnabled: Boolean
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val showAuth = remember { mutableStateOf(true) }
    val authFailed = stringResource(R.string.Auth_Error)

    // Navigate immediately if biometric is not enabled (inside LaunchedEffect)
    LaunchedEffect(isBiometricEnabled) {
        if (!isBiometricEnabled) {
            navController.navigate(NavRoutes.StorageSelection.route) {
                popUpTo(NavRoutes.AuthScreen.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // If biometric is enabled, show authentication UI
    if (isBiometricEnabled && showAuth.value) {
        fun startAuthentication() {
            val biometricAuthenticator = BiometricAuthenticator(
                activity = activity,
                onSuccess = {
                    showAuth.value = false
                    navController.navigate(NavRoutes.Workspace.route) {
                        popUpTo(NavRoutes.AuthScreen.route) { inclusive = true }
                        launchSingleTop = true
                    }
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
                navController.navigate(NavRoutes.Workspace.route) {
                    popUpTo(NavRoutes.AuthScreen.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        LaunchedEffect(Unit) { startAuthentication() }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { },
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircleWrapper(
                                    size = 0.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(64.dp))
                    Text(
                        stringResource(R.string.Auth_with_Biometric),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(24.dp))
                    IconButton(
                        onClick = { startAuthentication() },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        )
                    ) {
                        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(42.dp))
                    }
                }
            }
        }
    }
}
