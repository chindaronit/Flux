package com.flux

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.flux.navigation.AppNavHost
import com.flux.other.Constants.Other
import com.flux.other.createNotificationChannel
import com.flux.ui.effects.ScreenEffect
import com.flux.ui.state.States
import com.flux.ui.theme.FluxTheme
import com.flux.ui.viewModel.BackupSettingsViewModel
import com.flux.ui.viewModel.BackupViewModel
import com.flux.ui.viewModel.EventViewModel
import com.flux.ui.viewModel.HabitViewModel
import com.flux.ui.viewModel.JournalViewModel
import com.flux.ui.viewModel.LabelViewModel
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.ProgressBoardViewModel
import com.flux.ui.viewModel.SettingsViewModel
import com.flux.ui.viewModel.TodoViewModel
import com.flux.ui.viewModel.ViewModels
import com.flux.ui.viewModel.WorkspaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var keepSplashScreen = mutableStateOf(true)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        splashScreen.setKeepOnScreenCondition {
            keepSplashScreen.value
        }

        // Animated exit transition for the system splash screen.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView

            // Initial state
            iconView.alpha = 1f
            iconView.scaleX = 1f
            iconView.scaleY = 1f
            iconView.translationY = 0f

            // Subtle "settle" scale
            val scaleX = ObjectAnimator.ofFloat(
                iconView,
                "scaleX",
                1f,
                1.08f,
                0.82f
            )

            val scaleY = ObjectAnimator.ofFloat(
                iconView,
                "scaleY",
                1f,
                1.08f,
                0.82f
            )

            // Move upward while leaving
            val translationY = ObjectAnimator.ofFloat(
                iconView,
                "translationY",
                0f,
                -20f,
                -90f
            )

            // Fade slightly later than the movement begins
            val fadeOut = ObjectAnimator.ofFloat(
                iconView,
                "alpha",
                1f,
                1f,
                0f
            )

            AnimatorSet().apply {

                playTogether(
                    scaleX,
                    scaleY,
                    translationY,
                    fadeOut
                )

                duration = 550L

                // Smooth deceleration rather than AnticipateInterpolator.
                interpolator = android.view.animation.PathInterpolator(
                    0.2f,
                    0f,
                    0f,
                    1f
                )

                addListener(object : Animator.AnimatorListener {

                    override fun onAnimationStart(animation: Animator) = Unit

                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        splashScreenView.remove()
                    }

                    override fun onAnimationRepeat(animation: Animator) = Unit
                })

                start()
            }
        }

        enableEdgeToEdge()

        setContent {
            val snackBarHostState = remember { SnackbarHostState() }

            // ViewModels
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val notesViewModel: NotesViewModel = hiltViewModel()
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            val eventViewModel: EventViewModel = hiltViewModel()
            val habitViewModel: HabitViewModel = hiltViewModel()
            val todoViewModel: TodoViewModel = hiltViewModel()
            val journalViewModel: JournalViewModel = hiltViewModel()
            val backupViewModel: BackupViewModel = hiltViewModel()
            val labelViewModel: LabelViewModel = hiltViewModel()
            val progressBoardViewModel: ProgressBoardViewModel = hiltViewModel()
            val backupSettingsViewModel: BackupSettingsViewModel = hiltViewModel()

            // States
            val settings by settingsViewModel.state.collectAsState()
            val notesState by notesViewModel.state.collectAsStateWithLifecycle()
            val workspaceState by workspaceViewModel.state.collectAsStateWithLifecycle()
            val eventState by eventViewModel.state.collectAsStateWithLifecycle()
            val habitState by habitViewModel.state.collectAsStateWithLifecycle()
            val todoState by todoViewModel.state.collectAsStateWithLifecycle()
            val journalState by journalViewModel.state.collectAsStateWithLifecycle()
            val labelState by labelViewModel.state.collectAsStateWithLifecycle()
            val progressBoardState by progressBoardViewModel.state.collectAsStateWithLifecycle()
            val rootChangeState by settingsViewModel.rootChangeState.collectAsStateWithLifecycle()

            // Keep system splash on screen until settings are actually loaded
            LaunchedEffect(settings.isLoading) { keepSplashScreen.value = settings.isLoading }

            HandleSideEffects(
                snackBarHostState,
                workspaceViewModel.effect,
                settingsViewModel.effect
            )

            FluxTheme(settings, settingsViewModel::onEvent) {
                if (!settings.isLoading) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AppNavHost(
                            navController = rememberNavController(),
                            snackbarHostState = snackBarHostState,
                            viewModels = ViewModels(
                                notesViewModel,
                                eventViewModel,
                                todoViewModel,
                                habitViewModel,
                                workspaceViewModel,
                                journalViewModel,
                                settingsViewModel,
                                backupViewModel,
                                labelViewModel,
                                progressBoardViewModel,
                                backupSettingsViewModel
                            ),
                            states = States(
                                notesState,
                                eventState,
                                habitState,
                                todoState,
                                workspaceState,
                                journalState,
                                progressBoardState,
                                labelState,
                                settings,
                                rootChangeState
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HandleSideEffects(
    snackbarHostState: SnackbarHostState,
    vararg effectFlows: Flow<ScreenEffect>
) {
    LaunchedEffect(Other.SIDE_EFFECT_KEY) {
        effectFlows.forEach { effectFlow ->
            effectFlow
                .onEach { effect ->
                    if (effect is ScreenEffect.ShowSnackBarMessage) {
                        val result = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Short
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    }
                }
                .launchIn(this)
        }
    }
}