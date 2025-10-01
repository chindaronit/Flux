package com.flux.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flux.ui.state.EventState
import com.flux.ui.state.HabitState
import com.flux.ui.state.JournalState
import com.flux.ui.state.NotesState
import com.flux.ui.state.Settings
import com.flux.ui.state.States
import com.flux.ui.state.TodoState
import com.flux.ui.state.WorkspaceState
import com.flux.ui.viewModel.BackupViewModel
import com.flux.ui.viewModel.EventViewModel
import com.flux.ui.viewModel.HabitViewModel
import com.flux.ui.viewModel.JournalViewModel
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import com.flux.ui.viewModel.TodoViewModel
import com.flux.ui.viewModel.ViewModels
import com.flux.ui.viewModel.WorkspaceViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState,
    settingsViewModel: SettingsViewModel,
    notesViewModel: NotesViewModel,
    workspaceViewModel: WorkspaceViewModel,
    eventViewModel: EventViewModel,
    habitViewModel: HabitViewModel,
    todoViewModel: TodoViewModel,
    journalViewModel: JournalViewModel,
    backupViewModel: BackupViewModel,
    settings: Settings,
    notesState: NotesState,
    workspaceState: WorkspaceState,
    eventState: EventState,
    habitState: HabitState,
    todoState: TodoState,
    journalState: JournalState
) {
    NavHost(navController, startDestination = NavRoutes.AuthScreen.route) {
        NotesScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{notesId}")) {
                arguments.add(navArgument("notesId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val notesId = entry.arguments?.getString("notesId") ?: ""
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""

                screen(
                    navController,
                    notesId,
                    workspaceId,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    )
                )
            }
        }

        AuthScreen.forEach { (route, screen) ->
            animatedComposable(route) {
                screen(
                    navController,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    )
                )
            }
        }

        JournalScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{journalId}")) {
                arguments.add(navArgument("journalId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val journalId = entry.arguments?.getString("journalId") ?: ""
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""

                screen(
                    navController,
                    journalId,
                    workspaceId,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    )
                )
            }
        }

        TodoScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{listId}")) {
                arguments.add(navArgument("listId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val listId = entry.arguments?.getString("listId") ?: ""
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""

                screen(
                    navController,
                    listId,
                    workspaceId,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    )
                )
            }
        }

        HabitScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{habitId}")) {
                arguments.add(navArgument("habitId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val habitId = entry.arguments?.getString("habitId") ?: ""
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""

                screen(
                    navController,
                    habitId,
                    workspaceId,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    )
                )
            }
        }

        SettingsScreens.forEach { (route, screen) ->
            if (route == NavRoutes.Settings.route) {
                slideInComposable(route) {
                    screen(
                        navController,
                        snackbarHostState,
                        States(
                            notesState,
                            eventState,
                            habitState,
                            todoState,
                            workspaceState,
                            journalState,
                            settings
                        ),
                        ViewModels(
                            notesViewModel,
                            eventViewModel,
                            todoViewModel,
                            habitViewModel,
                            workspaceViewModel,
                            journalViewModel,
                            settingsViewModel,
                            backupViewModel
                        )
                    )
                }
            } else {
                animatedComposable(route) {
                    screen(
                        navController,
                        snackbarHostState,
                        States(
                            notesState,
                            eventState,
                            habitState,
                            todoState,
                            workspaceState,
                            journalState,
                            settings
                        ),
                        ViewModels(
                            notesViewModel,
                            eventViewModel,
                            todoViewModel,
                            habitViewModel,
                            workspaceViewModel,
                            journalViewModel,
                            settingsViewModel,
                            backupViewModel
                        )
                    )
                }
            }
        }

        EventScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{eventId}")) {
                arguments.add(navArgument("eventId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            if (route.contains("{instanceDate}")) {
                arguments.add(navArgument("instanceDate") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""
                val eventId = entry.arguments?.getString("eventId") ?: ""
                val instanceDate = entry.arguments?.getLong("instanceDate") ?: 0L

                screen(navController, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel, backupViewModel), eventId, workspaceId, instanceDate)
            }
        }

        LabelScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""

                screen(
                    navController,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    ),
                    workspaceId
                )
            }
        }

        WorkspaceScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = false
                })
            }

            animatedComposable(route, arguments) { entry ->
                val id = entry.arguments?.getString("workspaceId") ?: ""
                screen(
                    navController,
                    snackbarHostState,
                    States(
                        notesState,
                        eventState,
                        habitState,
                        todoState,
                        workspaceState,
                        journalState,
                        settings
                    ),
                    ViewModels(
                        notesViewModel,
                        eventViewModel,
                        todoViewModel,
                        habitViewModel,
                        workspaceViewModel,
                        journalViewModel,
                        settingsViewModel,
                        backupViewModel
                    ),
                    id
                )
            }
        }
    }
}

