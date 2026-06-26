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
import com.flux.data.model.WorkspaceModel
import com.flux.ui.screens.auth.AuthScreen
import com.flux.ui.screens.events.EventDetails
import com.flux.ui.screens.events.NewEvent
import com.flux.ui.screens.habits.HabitDetails
import com.flux.ui.screens.habits.NewHabit
import com.flux.ui.screens.journal.EditJournal
import com.flux.ui.screens.labels.EditLabels
import com.flux.ui.screens.notes.NoteDetails
import com.flux.ui.screens.search.SearchScreen
import com.flux.ui.screens.settings.About
import com.flux.ui.screens.settings.Contact
import com.flux.ui.screens.settings.Customize
import com.flux.ui.screens.settings.Data
import com.flux.ui.screens.settings.Editor
import com.flux.ui.screens.settings.Languages
import com.flux.ui.screens.settings.Mode
import com.flux.ui.screens.settings.NotesPreviewSetting
import com.flux.ui.screens.settings.Privacy
import com.flux.ui.screens.settings.Settings
import com.flux.ui.screens.settings.StorageSelectionScreen
import com.flux.ui.screens.settings.Themes
import com.flux.ui.screens.todo.NewTodoList
import com.flux.ui.screens.todo.TodoDetail
import com.flux.ui.screens.workspaces.NewWorkspaceScreen
import com.flux.ui.screens.workspaces.WorkspaceDetails
import com.flux.ui.screens.workspaces.WorkspaceHomeScreen
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

sealed class NavRoutes(val route: String) {
    data object AuthScreen : NavRoutes("biometric") // auth screen
    data object StorageSelection : NavRoutes("storageSelection") // Storage Selection
    data object Workspace : NavRoutes("workspace") // workspaces
    data object NewWorkspace: NavRoutes("workspace/edit") // edit workspace
    data object WorkspaceHome : NavRoutes("workspace/details")
    data object EditLabels : NavRoutes("workspace/labels/edit") //Labels
    data object NoteDetails : NavRoutes("workspace/note/details") // Notes
    data object HabitDetails : NavRoutes("workspace/habit/details") // Habit detail
    data object NewHabit : NavRoutes("workspace/habit/new") // new habit
    data object EventDetails : NavRoutes("workspace/event/details") //  event detail
    data object TodoDetail : NavRoutes("workspace/todo/details") // TodoList
    data object NewTodoList : NavRoutes("workspace/todo/newTodo") // TodoList
    data object EditJournal : NavRoutes("workspace/journal/edit") // Journal
    data object NewEvent : NavRoutes("workspace/event/edit") // new event
    data object Analytics : NavRoutes("Analytics")
    data object Search : NavRoutes("workspace/search")

    // Settings
    data object Settings : NavRoutes("settings")
    data object Privacy : NavRoutes("settings/privacy")
    data object Customize : NavRoutes("settings/customize")
    data object Theme : NavRoutes("settings/customize/theme")
    data object Languages : NavRoutes("settings/language")
    data object About : NavRoutes("settings/about")
    data object Contact : NavRoutes("settings/contact")
    data object Backup : NavRoutes("setting/backup")
    data object Editor : NavRoutes("setting/editor")
    data object Mode : NavRoutes("setting/mode")
    data object NotesPreview : NavRoutes("setting/editor/notesPreview")

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

val StorageSelectionScreen = mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels) -> Unit>(
    NavRoutes.StorageSelection.route to { navController, states, viewModels ->
        StorageSelectionScreen(navController, viewModels.settingsViewModel, states.settings.data.storageRootUri!=null)
    }
)

val SearchScreens = mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels) -> Unit>(
    NavRoutes.Search.route to { navController, states, viewModels ->
        SearchScreen(navController, states, viewModels)
    }
)

val NotesScreens =
    mapOf<String, @Composable (navController: NavController, notesId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.NoteDetails.route + "/{workspaceId}" + "/{notesId}" to { navController, notesId, workspaceId, states, viewModel ->
            NoteDetails(
                navController,
                states.workspaceState.allWorkspaces,
                states.notesState.outline,
                states.notesState.textState,
                workspaceId,
                states.settings.data.isDarkMode,
                states.settings.data.isLintValid,
                states.settings.data.isLineNumbersVisible,
                states.settings.data.startWithReadView,
                states.notesState.allNotes.find { it.notesId == notesId } ?: NotesModel(workspaceId = workspaceId),
                states.settings.data.storageRootUri,
                states.labelState.allLabels.filter { it.workspaceId==workspaceId },
                viewModel.settingsViewModel,
                viewModel.notesViewModel,
                viewModel.notesViewModel::onEvent,
                viewModel.journalViewModel::onEvent,
                viewModel.todoViewModel::onEvent,
                viewModel.workspaceViewModel::onEvent
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
                states.workspaceState.allWorkspaces,
                states.habitState.allInstances.filter { it.habitId == habitId },
                viewModel.habitViewModel::onEvent,
                viewModel.workspaceViewModel::onEvent
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
                states.settings.data.cornerRadius,
                states.todoState.allLists.first { it.id==listId },
                states.workspaceState.allWorkspaces,
                states.todoState.allInstances.filter { it.workspaceId==workspaceId && it.todoId==listId },
                workspaceId,
                viewModel.todoViewModel::onEvent,
                viewModel.notesViewModel::onEvent,
                viewModel.journalViewModel::onEvent,
                viewModel.workspaceViewModel::onEvent
            )
        },
        NavRoutes.NewTodoList.route + "/{workspaceId}" + "/{listId}" to { navController, listId, workspaceId, states, viewModel ->
            NewTodoList(
                navController,
                states.settings.data.is24HourFormat,
                states.todoState.allLists.find { it.id == listId } ?: TodoModel(workspaceId = workspaceId),
                workspaceId,
                viewModel.todoViewModel::onEvent
            )
        }
    )

val JournalScreens =
    mapOf<String, @Composable (navController: NavController, journalId: String, journalDateTime: Long, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.EditJournal.route + "/{workspaceId}" + "/{journalId}" + "/{journalDateTime}" to { navController, journalId, journalDateTime, workspaceId, states, viewModel ->
            EditJournal(
                navController,
                states.workspaceState.allWorkspaces,
                workspaceId,
                states.journalState.data.find { it.journalId == journalId } ?: JournalModel(workspaceId = workspaceId, dateTime = journalDateTime),
                states.journalState.outline,
                states.journalState.textState,
                states.settings.data.isDarkMode,
                states.settings.data.isLintValid,
                states.settings.data.isLineNumbersVisible,
                states.settings.data.startWithReadView,
                states.settings.data.storageRootUri,
                states.labelState.allLabels.filter { it.workspaceId==workspaceId },
                viewModel.journalViewModel,
                viewModel.settingsViewModel,
                viewModel.journalViewModel::onEvent,
                viewModel.notesViewModel::onEvent,
                viewModel.todoViewModel::onEvent,
                viewModel.workspaceViewModel::onEvent
            )
        }
    )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val SettingsScreens =
    mapOf<String, @Composable (navController: NavController, snackbarHostState: SnackbarHostState, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.Settings.route to { navController, _, states, viewModels ->
            Settings(
                navController = navController,
                settings = states.settings,
                onSettingsEvent = viewModels.settingsViewModel::onEvent,
                workspaces = states.workspaceState.allWorkspaces
            )
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
        NavRoutes.Backup.route to { navController, snackbarHostState, states, viewModels ->
            Data(navController, states.settings.data.cornerRadius, states.settings, snackbarHostState, viewModels.backupViewModel, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.Editor.route to { navController, _, states, viewModels ->
            Editor(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.Theme.route to { navController, _, states, viewModels ->
            Themes(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.Mode.route to { navController, _, states, viewModels ->
            Mode(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.NotesPreview.route to { navController, _, states, viewModels ->
            NotesPreviewSetting(navController, states.settings, viewModels.settingsViewModel::onEvent)
        }
    )

val EventScreens =
    mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels, eventId: String, workspaceId: String, instanceDate: Long, eventDate: Long) -> Unit>(
        NavRoutes.EventDetails.route + "/{workspaceId}" + "/{eventId}" + "/{instanceDate}" to { navController, states, viewModels, eventId, workspaceId, instanceDate, _ ->
            EventDetails(
                navController,
                states.workspaceState.allWorkspaces,
                workspaceId,
                states.eventState.allEvent.find { it.id == eventId } ?: EventModel(workspaceId = workspaceId),
                states.eventState.allEventInstances.find { it.eventId == eventId && it.instanceDate == instanceDate } == null,
                instanceDate,
                states.settings,
                viewModels.eventViewModel::onEvent,
                viewModels.workspaceViewModel::onEvent
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
            WorkspaceHomeScreen(
                snackbarHostState,
                navController,
                states,
                viewModels
            )
        },

        NavRoutes.NewWorkspace.route + "/{workspaceId}" to { navController, _, states, viewModels, workspaceId ->
            NewWorkspaceScreen (
                navController,
                states.workspaceState.allWorkspaces.find { it.workspaceId==workspaceId }?: WorkspaceModel(),
                viewModels.workspaceViewModel::onEvent
            )
        },

        NavRoutes.WorkspaceHome.route + "/{workspaceId}" to { navController, _, states, viewModels, workspaceId ->
            WorkspaceDetails(
                navController,
                states,
                states.workspaceState.allWorkspaces.find { it.workspaceId==workspaceId }?: WorkspaceModel(workspaceId=workspaceId),
                viewModels
            )
        }
    )

val LabelScreens =
    mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels, workspaceId: String) -> Unit>(
        NavRoutes.EditLabels.route + "/{workspaceId}" to { navController, states, viewModels, workspaceId ->
            EditLabels(
                navController,
                states.labelState.isLoading,
                workspaceId,
                states.labelState.allLabels.filter { it.workspaceId==workspaceId },
                viewModels.labelViewModel::onEvent
            )
        }
    )
