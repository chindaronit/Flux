package com.flux.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flux.ui.state.HabitState
import com.flux.ui.state.NotesState
import com.flux.ui.state.Settings
import com.flux.ui.state.States
import com.flux.ui.state.EventState
import com.flux.ui.state.JournalState
import com.flux.ui.state.TodoState
import com.flux.ui.state.WorkspaceState
import com.flux.ui.viewModel.HabitViewModel
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import com.flux.ui.viewModel.EventViewModel
import com.flux.ui.viewModel.JournalViewModel
import com.flux.ui.viewModel.TodoViewModel
import com.flux.ui.viewModel.ViewModels
import com.flux.ui.viewModel.WorkspaceViewModel

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
                    type = NavType.LongType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val notesId = entry.arguments?.getLong("notesId") ?: 0L
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L

                screen(navController, notesId, workspaceId, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel))
            }
        }

        AuthScreen.forEach { (route, screen) ->
            animatedComposable(route){
                screen(navController, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings))
            }
        }

        JournalScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            if (route.contains("{journalId}")) {
                arguments.add(navArgument("journalId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val journalId = entry.arguments?.getLong("journalId") ?: 0L
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L

                screen(navController, journalId, workspaceId, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel))
            }
        }

        TodoScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{listId}")) {
                arguments.add(navArgument("listId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: 0L
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L

                screen(navController, listId, workspaceId, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel))
            }
        }

        HabitScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{habitId}")) {
                arguments.add(navArgument("habitId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            animatedComposable(route, arguments) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: 0L
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L

                screen(navController, habitId, workspaceId, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel))
            }
        }

        SettingsScreens.forEach { (route, screen)->
            if (route == NavRoutes.Settings.route){
                slideInComposable(route) { screen(navController, snackbarHostState, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel)) }
            }
            else{
                animatedComposable(route) { screen(navController, snackbarHostState, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel)) }
            }
        }

        EventScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{eventId}")) {
                arguments.add(navArgument("eventId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) {entry->
                val eventId = entry.arguments?.getLong("eventId") ?: 0L
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L
                screen(navController, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel), eventId, workspaceId)
            }
        }

        LabelScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry->
                val workspaceId = entry.arguments?.getLong("workspaceId") ?: 0L

                screen(navController, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel), workspaceId)
            }
        }

        WorkspaceScreens.forEach { (route, screen) ->
            val arguments = mutableListOf<NamedNavArgument>()

            if (route.contains("{workspaceId}")) {
                arguments.add(navArgument("workspaceId") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            animatedComposable(route, arguments) { entry ->
                val id = entry.arguments?.getLong("workspaceId") ?: 0L
                screen(navController, snackbarHostState, States(notesState, eventState, habitState, todoState, workspaceState, journalState, settings), ViewModels(notesViewModel, eventViewModel, todoViewModel, habitViewModel, workspaceViewModel, journalViewModel, settingsViewModel), id)
            }
        }
    }
}

