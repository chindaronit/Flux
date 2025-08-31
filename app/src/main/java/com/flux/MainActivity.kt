package com.flux

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flux.navigation.AppNavHost
import com.flux.navigation.Loader
import com.flux.other.Notifications
import com.flux.ui.effects.ScreenEffect
import com.flux.ui.theme.FluxTheme
import com.flux.ui.viewModel.BackupViewModel
import com.flux.ui.viewModel.EventViewModel
import com.flux.ui.viewModel.HabitViewModel
import com.flux.ui.viewModel.JournalViewModel
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import com.flux.ui.viewModel.TodoViewModel
import com.flux.ui.viewModel.WorkspaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var keepSplashScreen = mutableStateOf(true)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable predictive back gesture to prevent conflicts with file pickers
        onBackPressedDispatcher.addCallback(this) {
            // Handle back press normally
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        Notifications.setupNotifications(this)
        // Splash screen condition
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen.value }

//        startAttentionManager(applicationContext)

//        if (!checkUsageStatsPermission()) {
//            requestUsageStatsPermission();
//        } else {
//            val service = Executors.newSingleThreadScheduledExecutor()
//            val handler = Handler(Looper.getMainLooper())
//            service.scheduleWithFixedDelay({
//                handler.run {
//                    var foregroundAppPackageName: String? = null
//                    val usageStatsManager =
//                        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
//
//                    val endTime = System.currentTimeMillis()
//                    val startTime = endTime - 1000 * 60 * 10 // 10 mins
//
//                    val usageEvents =
//                        usageStatsManager.queryEvents(
//                            startTime,
//                            endTime
//                        )
//                    val usageEvent = UsageEvents.Event()
//                    while (usageEvents.hasNextEvent()) {
//                        usageEvents.getNextEvent(usageEvent)
//                        if (usageEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
//                            try {
//                                val appInfo =
//                                    packageManager.getApplicationInfo(usageEvent.packageName, 0)
//                                foregroundAppPackageName =
//                                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) usageEvent.packageName else null
//                            } catch (e: PackageManager.NameNotFoundException) {
//                                // unreachable?
//                                Log.e(
//                                    "FOREGROUND_APP",
//                                    "Package not found: ${usageEvent.packageName}",
//                                    e
//                                )
//                            }
//                        }
//                    }
//                    Log.e(
//                        "FOREGROUND_APP",
//                        "Currently open non-system app: $foregroundAppPackageName"
//                    )
//                }
//            }, 0, 15, TimeUnit.SECONDS);
//        }

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

            // States
            val settings by settingsViewModel.state.collectAsState()
            val notesState by notesViewModel.state.collectAsStateWithLifecycle()
            val workspaceState by workspaceViewModel.state.collectAsStateWithLifecycle()
            val eventState by eventViewModel.state.collectAsStateWithLifecycle()
            val habitState by habitViewModel.state.collectAsStateWithLifecycle()
            val todoState by todoViewModel.state.collectAsStateWithLifecycle()
            val journalState by journalViewModel.state.collectAsStateWithLifecycle()

            // Stop splash screen when settings are loaded
            LaunchedEffect(settings.isLoading) { keepSplashScreen.value = settings.isLoading }

            // Snackbar effect
            LaunchedEffect(Unit) {
                workspaceViewModel.effect.collect { effect ->
                    if (effect is ScreenEffect.ShowSnackBarMessage) {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        snackBarHostState.showSnackbar(
                            message = effect.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }

            if (!settings.isLoading) {
                FluxTheme(settings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AppNavHost(
                            snackbarHostState = snackBarHostState,
                            settingsViewModel = settingsViewModel,
                            notesViewModel = notesViewModel,
                            workspaceViewModel = workspaceViewModel,
                            eventViewModel = eventViewModel,
                            habitViewModel = habitViewModel,
                            todoViewModel = todoViewModel,
                            journalViewModel = journalViewModel,
                            backupViewModel = backupViewModel,
                            settings = settings,
                            notesState = notesState,
                            workspaceState = workspaceState,
                            eventState = eventState,
                            habitState = habitState,
                            todoState = todoState,
                            journalState = journalState
                        )
                    }
                }
            } else {
                Loader()
            }
        }
    }

    private fun requestUsageStatsPermission(): Unit {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = "package:${packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            applicationContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "Unable to open usage stats permission settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getNonSystemAppsList(): Map<String, String> {
        val appInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appInfoMap = HashMap<String, String>()
        for (appInfo in appInfos) {
            if (appInfo.flags != ApplicationInfo.FLAG_SYSTEM) {
                appInfoMap[appInfo.packageName] =
                    packageManager.getApplicationLabel(appInfo).toString()
            }
        }
        return appInfoMap
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            Process.myUid(), packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }
}