package com.flux.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.flux.data.model.EventModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.NotesModel
import com.flux.data.model.TodoModel
import com.flux.ui.screens.auth.AuthScreen
import com.flux.ui.screens.events.EventDetails
import com.flux.ui.screens.events.NewEvent
import com.flux.ui.screens.habits.HabitDetails
import com.flux.ui.screens.habits.NewHabit
import com.flux.ui.screens.journal.EditJournal
import com.flux.ui.screens.notes.EditLabels
import com.flux.ui.screens.notes.NoteDetails
import com.flux.ui.screens.settings.About
import com.flux.ui.screens.settings.Backup
import com.flux.ui.screens.settings.Contact
import com.flux.ui.screens.settings.Customize
import com.flux.ui.screens.settings.Languages
import com.flux.ui.screens.settings.Privacy
import com.flux.ui.screens.settings.Settings
import com.flux.ui.screens.todo.TodoDetail
import com.flux.ui.screens.workspaces.WorkSpaces
import com.flux.ui.screens.workspaces.WorkspaceDetails
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

sealed class NavRoutes(val route: String) {
    data object AuthScreen : NavRoutes("biometric") // auth screen
    data object Workspace : NavRoutes("workspace") // workspaces
    data object WorkspaceHome : NavRoutes("workspace/details")
    data object EditLabels : NavRoutes("workspace/labels/edit") //Labels
    data object NoteDetails : NavRoutes("workspace/note/details") // Notes
    data object HabitDetails : NavRoutes("workspace/habit/details") // Habit detail
    data object NewHabit : NavRoutes("workspace/habit/new") // new habit
    data object EventDetails : NavRoutes("workspace/event/details") //  event detail
    data object TodoDetail : NavRoutes("workspace/todo/details") // TodoList
    data object EditJournal : NavRoutes("workspace/journal/edit") // Journal
    data object NewEvent : NavRoutes("workspace/event/edit") // new event

    // Settings
    data object Settings : NavRoutes("settings")
    data object Privacy : NavRoutes("settings/privacy")
    data object Customize : NavRoutes("settings/customize")
    data object Languages : NavRoutes("settings/language")
    data object About : NavRoutes("settings/about")
    data object Contact : NavRoutes("settings/contact")
    data object Backup : NavRoutes("setting/backup")

    fun withArgs(vararg args: Any): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/${arg}")
            }
        }
    }
}

val AuthScreen =
    mapOf<String, @Composable (navController: NavController, states: States) -> Unit>(
        NavRoutes.AuthScreen.route to { navController, states ->
            AuthScreen(navController, states.settings.data.isBiometricEnabled)
        }
    )

val NotesScreens =
    mapOf<String, @Composable (navController: NavController, notesId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.NoteDetails.route + "/{workspaceId}" + "/{notesId}" to { navController, notesId, workspaceId, states, viewModel ->
            NoteDetails(
                navController,
                workspaceId,
                states.notesState.allNotes.find { it.notesId == notesId }
                    ?: NotesModel(workspaceId = workspaceId),
                states.notesState.allLabels.filter { it.workspaceId == workspaceId },
                viewModel.notesViewModel::onEvent
            )
        }
    )

val HabitScreens =
    mapOf<String, @Composable (navController: NavController, habitId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.HabitDetails.route + "/{workspaceId}" + "/{habitId}" to { navController, habitId, workspaceId, states, viewModel ->
            HabitDetails(
                navController,
                states.settings.data.cornerRadius,
                workspaceId,
                states.habitState.allHabits.first { it.id == habitId },
                states.habitState.allInstances.filter { it.habitId == habitId },
                viewModel.habitViewModel::onEvent
            )
        },
        NavRoutes.NewHabit.route + "/{workspaceId}" + "/{habitId}" to { navController, habitId, workspaceId, states, viewModel ->
            NewHabit(
                navController,
                states.habitState.allHabits.find { it.id == habitId } ?: HabitModel(workspaceId=workspaceId),
                states.settings,
                viewModel.habitViewModel::onEvent
            )
        }
    )

val TodoScreens =
    mapOf<String, @Composable (navController: NavController, listId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.TodoDetail.route + "/{workspaceId}" + "/{listId}" to { navController, listId, workspaceId, states, viewModel ->
            TodoDetail(
                navController,
                states.todoState.allLists.find { it.id == listId }
                    ?: TodoModel(workspaceId = workspaceId),
                workspaceId,
                viewModel.todoViewModel::onEvent
            )
        }
    )

val JournalScreens =
    mapOf<String, @Composable (navController: NavController, journalId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.EditJournal.route + "/{workspaceId}" + "/{journalId}" to { navController, journalId, workspaceId, states, viewModel ->
            EditJournal(
                navController,
                states.journalState.allEntries.find { it.journalId == journalId } ?:
                JournalModel(workspaceId = workspaceId),
                viewModel.journalViewModel::onEvent
            )
        }
    )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val SettingsScreens =
    mapOf<String, @Composable (navController: NavController, snackbarHostState: SnackbarHostState, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.Settings.route to { navController, _, states, viewModels ->
            Settings(navController, states.settings)
        },
        NavRoutes.Privacy.route to { navController, _, states, viewModels ->
            Privacy(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.About.route to { navController, _, states, _ ->
            About(navController, states.settings.data.cornerRadius)
        },
        NavRoutes.Languages.route to { navController, _, states, _ ->
            Languages(navController, states.settings)
        },
        NavRoutes.Customize.route to { navController, _, states, viewModels ->
            Customize(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.Contact.route to { navController, _, states, _ ->
            Contact(navController, states.settings.data.cornerRadius)
        },
        NavRoutes.Backup.route to { navController, _, states, viewModels ->
            Backup(navController, states.settings.data.cornerRadius, viewModels.backupViewModel)
        }
    )

val EventScreens =
    mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels, eventId: String, workspaceId: String, instanceDate: Long, eventDate: Long) -> Unit>(
        NavRoutes.EventDetails.route + "/{workspaceId}" + "/{eventId}" + "/{instanceDate}" to { navController, states, viewModels, eventId, workspaceId, instanceDate, _ ->
            EventDetails(
                navController,
                workspaceId,
                states.eventState.allEvent.find { it.id == eventId } ?: EventModel(workspaceId = workspaceId),
                states.eventState.allEventInstances.find { it.eventId == eventId && it.instanceDate == instanceDate } == null,
                instanceDate,
                states.settings,
                viewModels.eventViewModel::onEvent
            )
        },
        NavRoutes.NewEvent.route + "/{workspaceId}" + "/{eventId}" + "/{eventDate}"  to { navController, states, viewModels, eventId, workspaceId, _, eventDate ->
            NewEvent(
                navController,
                states.eventState.allEvent.find { it.id == eventId } ?: EventModel(workspaceId = workspaceId, startDateTime = eventDate),
                states.settings,
                viewModels.eventViewModel::onEvent
            )
        }
    )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val WorkspaceScreens =
    mapOf<String, @Composable (navController: NavController, snackbarHostState: SnackbarHostState, states: States, viewModels: ViewModels, workspaceId: String) -> Unit>(
        NavRoutes.Workspace.route to { navController, snackbarHostState, states, viewModels, _ ->
            WorkSpaces(
                snackbarHostState,
                navController,
                states.settings.data.workspaceGridColumns,
                states.settings.data.cornerRadius,
                states.workspaceState.allSpaces,
                viewModels.notesViewModel::onEvent,
                viewModels.eventViewModel::onEvent,
                viewModels.habitViewModel::onEvent,
                viewModels.todoViewModel::onEvent,
                viewModels.workspaceViewModel::onEvent,
                viewModels.journalViewModel::onEvent
            )
        },
        NavRoutes.WorkspaceHome.route + "/{workspaceId}" to { navController, snackbarHostState, states, viewModels, workspaceId ->
            WorkspaceDetails(
                navController,
                states.notesState.allLabels.filter { it.workspaceId == workspaceId },
                states.settings,
                states.notesState.isNotesLoading,
                states.eventState.isAllEventsLoading,
                states.eventState.isDatedEventLoading,
                states.todoState.isLoading,
                states.journalState.isLoading,
                states.habitState.isLoading,
                states.workspaceState.allSpaces.first { it.workspaceId == workspaceId },
                states.eventState.allEvent,
                states.notesState.allNotes.filter { it.workspaceId == workspaceId },
                states.notesState.selectedNotes,
                states.eventState.selectedYearMonth,
                states.eventState.selectedDate,
                states.eventState.datedEvents,
                states.habitState.allHabits,
                states.todoState.allLists,
                states.journalState.allEntries,
                states.habitState.allInstances,
                states.eventState.allEventInstances,
                viewModels.workspaceViewModel::onEvent,
                viewModels.notesViewModel::onEvent,
                viewModels.eventViewModel::onEvent,
                viewModels.habitViewModel::onEvent,
                viewModels.todoViewModel::onEvent,
                viewModels.journalViewModel::onEvent,
                viewModels.settingsViewModel::onEvent
            )
        }
    )

val LabelScreens =
    mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels, workspaceId: String) -> Unit>(
        NavRoutes.EditLabels.route + "/{workspaceId}" to { navController, states, viewModels, workspaceId ->
            EditLabels(
                navController,
                states.notesState.isLabelsLoading,
                workspaceId,
                states.notesState.allLabels,
                viewModels.notesViewModel::onEvent
            )
        }
    )
