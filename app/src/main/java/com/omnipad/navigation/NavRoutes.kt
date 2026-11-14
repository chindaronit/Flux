package com.omnipad.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.eventColorForOccurrence
import com.omnipad.data.model.HabitModel
import com.omnipad.data.model.JournalModel
import com.omnipad.data.model.NotesModel
import com.omnipad.data.model.ProgressBoardModel
import com.omnipad.data.model.TodoModel
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.ui.screens.auth.AuthScreen
import com.omnipad.ui.screens.events.NewEvent
import com.omnipad.ui.screens.habits.HabitDetails
import com.omnipad.ui.screens.habits.NewHabit
import com.omnipad.ui.screens.journal.EditJournal
import com.omnipad.ui.screens.labels.EditLabels
import com.omnipad.ui.screens.notes.NoteDetails
import com.omnipad.ui.screens.progressBoard.NewProgressItem
import com.omnipad.ui.screens.search.SearchScreen
import com.omnipad.ui.screens.settings.About
import com.omnipad.ui.screens.settings.Changelog
import com.omnipad.ui.screens.settings.Contact
import com.omnipad.ui.screens.settings.Customize
import com.omnipad.ui.screens.settings.Data
import com.omnipad.ui.screens.settings.DefaultWorkspace
import com.omnipad.ui.screens.settings.Editor
import com.omnipad.ui.screens.settings.EventSettings
import com.omnipad.ui.screens.settings.Languages
import com.omnipad.ui.screens.settings.Mode
import com.omnipad.ui.screens.settings.NotesPreviewSetting
import com.omnipad.ui.screens.settings.Privacy
import com.omnipad.ui.screens.settings.Settings
import com.omnipad.ui.screens.settings.StorageSelectionScreen
import com.omnipad.ui.screens.settings.Themes
import com.omnipad.ui.screens.todo.NewTodoList
import com.omnipad.ui.screens.todo.TodoDetail
import com.omnipad.ui.screens.workspaces.NewWorkspaceScreen
import com.omnipad.ui.screens.workspaces.WorkspaceDetails
import com.omnipad.ui.screens.workspaces.WorkspaceHomeScreen
import com.omnipad.ui.state.States
import com.omnipad.ui.viewModel.ViewModels
import java.time.Instant
import java.time.ZoneId

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
    data object TodoDetail : NavRoutes("workspace/todo/details") // TodoList
    data object NewTodoList : NavRoutes("workspace/todo/newTodo") // TodoList
    data object EditJournal : NavRoutes("workspace/journal/edit") // Journal
    data object NewEvent : NavRoutes("workspace/event/edit") // new event
    data object NewEventOccurrence : NavRoutes("workspace/event/editOccurrence")
    data object Analytics : NavRoutes("Analytics")
    data object Search : NavRoutes("workspace/search")
    data object NewProgressItem : NavRoutes("workspace/progress/new") // new habit

    // Settings
    data object Settings : NavRoutes("settings")
    data object Privacy : NavRoutes("settings/privacy")
    data object Customize : NavRoutes("settings/customize")
    data object EventSettings : NavRoutes("settings/customize/events")
    data object Theme : NavRoutes("settings/customize/theme")
    data object Languages : NavRoutes("settings/language")
    data object About : NavRoutes("settings/about")
    data object Changelog : NavRoutes("settings/about/changelog")
    data object Contact : NavRoutes("settings/contact")
    data object Backup : NavRoutes("setting/backup")
    data object Editor : NavRoutes("setting/editor")
    data object Mode : NavRoutes("setting/mode")
    data object DefaultWorkspace : NavRoutes("setting/defaultWorkspace")
    data object NotesPreview : NavRoutes("setting/editor/notesPreview")

    fun withArgs(vararg args: Any): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/${arg}")
            }
        }
    }

    companion object {
        fun eventEditor(event: EventModel, occurrenceDate: Long): String =
            if (event.recurrence == com.omnipad.data.model.RecurrenceRule.Once) {
                NewEvent.withArgs(event.workspaceId, event.id, event.startDateTime)
            } else {
                NewEventOccurrence.withArgs(event.workspaceId, event.id, occurrenceDate)
        }
    }
}

val AuthScreen =
    mapOf<String, @Composable (navController: NavController, states: States) -> Unit>(
        NavRoutes.AuthScreen.route to { navController, states ->
            AuthScreen(navController, states.settings.data, states.settings.data.isBiometricEnabled)
        }
    )

val StorageSelectionScreen = mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels) -> Unit>(
    NavRoutes.StorageSelection.route to { navController, states, viewModels ->
        StorageSelectionScreen(navController, states.settings.data, viewModels.settingsViewModel, states.settings.data.storageRootUri!=null)
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
                states.settings.data.fontNumber,
                states.settings.data.isDarkMode,
                states.settings.data.isLintValid,
                states.settings.data.isLineNumbersVisible,
                states.settings.data.startWithReadView,
                states.notesState.allNotes.find { it.notesId == notesId } ?: NotesModel(workspaceId = workspaceId, notesId = notesId),
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
                states.habitState.allHabits.find { it.id == habitId } ?: HabitModel(workspaceId=workspaceId, id = habitId),
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
                states.todoState.allLists.find { it.id == listId } ?: TodoModel(workspaceId = workspaceId, id = listId),
                workspaceId,
                viewModel.todoViewModel::onEvent
            )
        }
    )

val ProgressBoardScreens =
    mapOf<String, @Composable (navController: NavController, itemId: String, workspaceId: String, states: States, viewModels: ViewModels) -> Unit>(
        NavRoutes.NewProgressItem.route + "/{workspaceId}" + "/{itemId}" to { navController, itemId, workspaceId, states, viewModel ->
            NewProgressItem(
                navController,
                states.progressBoardState.allItems.find { it.itemId == itemId } ?: ProgressBoardModel(workspaceId = workspaceId, itemId = itemId),
                viewModel.progressBoardViewModel::onEvent
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
                states.journalState.data.find { it.journalId == journalId } ?: JournalModel(journalId = journalId, workspaceId = workspaceId, dateTime = journalDateTime),
                states.journalState.outline,
                states.journalState.textState,
                states.settings.data.fontNumber,
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
        NavRoutes.Settings.route to { navController, _, states, _ ->
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
        NavRoutes.EventSettings.route to { navController, _, states, viewModels ->
            EventSettings(navController, states.settings, viewModels.settingsViewModel::onEvent)
        },
        NavRoutes.Contact.route to { navController, _, states, _ ->
            Contact(navController, states.settings.data.cornerRadius)
        },
        NavRoutes.Backup.route to { navController, snackbarHostState, states, viewModels ->
            Data(navController, states.settings.data.cornerRadius, states.settings, states.rootChangeState, snackbarHostState, viewModels.backupViewModel, viewModels.backupSettingsViewModel, viewModels.settingsViewModel::onEvent)
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
        },
        NavRoutes.Changelog.route to { navController, _, _, _ ->
            Changelog(navController)
        },
        NavRoutes.DefaultWorkspace.route to { navController, _, states, viewModels ->
            DefaultWorkspace(navController, states.settings, states.workspaceState.allWorkspaces, viewModels.settingsViewModel::onEvent)
        }
    )

val EventScreens =
    mapOf<String, @Composable (navController: NavController, states: States, viewModels: ViewModels, eventId: String, workspaceId: String, instanceDate: Long, eventDate: Long) -> Unit>(
        NavRoutes.NewEvent.route + "/{workspaceId}" + "/{eventId}" + "/{eventDate}"  to { navController, states, viewModels, eventId, workspaceId, _, eventDate ->
            if (states.eventState.isAllEventsLoading) {
                Loader()
            } else {
                val storedEvent = states.eventState.allEvent.find { it.id == eventId }
                val isNewEvent = rememberSaveable(eventId) { storedEvent == null }
                val event = storedEvent
                    ?: EventModel(workspaceId = workspaceId, startDateTime = eventDate, id = eventId)
                val instanceDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                val hasOccurrenceColorOverride = event.recurrence == RecurrenceRule.Once &&
                    states.eventState.eventOccurrenceColors.any {
                        it.eventId == event.id && it.occurrenceDate == instanceDate
                    }
                NewEvent(
                    navController,
                    event,
                    states.settings,
                    viewModels.eventViewModel::onEvent,
                    isNewEvent = isNewEvent,
                    workspaces = states.workspaceState.allWorkspaces,
                    allEventInstances = states.eventState.allEventInstances,
                    eventOccurrenceColorArgb = if (event.recurrence == RecurrenceRule.Once) {
                        eventColorForOccurrence(event, instanceDate, states.eventState.eventOccurrenceColors)
                    } else event.colorArgb,
                    hasOccurrenceColorOverride = hasOccurrenceColorOverride,
                    onWorkspaceEvents = viewModels.workspaceViewModel::onEvent,
                    settingsViewModel = viewModels.settingsViewModel
                )
            }
        },
        NavRoutes.NewEventOccurrence.route + "/{workspaceId}" + "/{eventId}" + "/{occurrenceDate}" to { navController, states, viewModels, eventId, workspaceId, occurrenceDate, _ ->
            if (states.eventState.isAllEventsLoading) {
                Loader()
            } else {
                val event = states.eventState.allEvent.find { it.id == eventId }
                    ?: EventModel(workspaceId = workspaceId, id = eventId)
                NewEvent(
                    navController,
                    event,
                    states.settings,
                    viewModels.eventViewModel::onEvent,
                    occurrenceDate,
                    isNewEvent = false,
                    workspaces = states.workspaceState.allWorkspaces,
                    allEventInstances = states.eventState.allEventInstances,
                    eventOccurrenceColorArgb = eventColorForOccurrence(
                        event, occurrenceDate, states.eventState.eventOccurrenceColors
                    ),
                    hasOccurrenceColorOverride = false,
                    onWorkspaceEvents = viewModels.workspaceViewModel::onEvent,
                    settingsViewModel = viewModels.settingsViewModel
                )
            }
        }
    )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val WorkspaceScreens =
    mapOf<String, @Composable (navController: NavController, snackbarHostState: SnackbarHostState, states: States, viewModels: ViewModels, workspaceId: String, spaceId: Int) -> Unit>(
        NavRoutes.Workspace.route to { navController, snackbarHostState, states, viewModels, _, _ ->
            WorkspaceHomeScreen(snackbarHostState, navController, states, viewModels)
        },

        NavRoutes.NewWorkspace.route + "/{workspaceId}" to { navController, _, states, viewModels, workspaceId, _ ->
            NewWorkspaceScreen(
                navController,
                states.workspaceState.allWorkspaces.find { it.workspaceId == workspaceId } ?: WorkspaceModel(),
                viewModels,
                viewModels.workspaceViewModel::onEvent
            )
        },

        NavRoutes.WorkspaceHome.route + "/{workspaceId}?spaceId={spaceId}" to { navController, _, states, viewModels, workspaceId, spaceId ->
            WorkspaceDetails(
                navController,
                states,
                states.workspaceState.allWorkspaces.find { it.workspaceId == workspaceId } ?: WorkspaceModel(workspaceId = workspaceId),
                viewModels,
                spaceId
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
