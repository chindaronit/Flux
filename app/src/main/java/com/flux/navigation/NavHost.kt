package com.flux.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.flux.ui.common.BottomBar
import com.flux.ui.common.WorkspaceDropdownTopBar
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

/**
 * Global scaffold wrapper that provides workspace dropdown top bar
 * and bottom bar on every main screen.
 *
 * @param showTopBar When true, shows the WorkspaceDropdownTopBar.
 *                   Set to false for screens that have their own top bar (e.g., NotesScreen).
 */
@Composable
fun WorkspaceScaffold(
    states: States,
    viewModels: ViewModels,
    navController: NavHostController,
    workspaceId: String = "",
    showTopBar: Boolean = true,
    showBottomBar: Boolean = false,
    showBackButton: Boolean = false,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val allWorkspaces = states.workspaceState.allWorkspaces
    val currentWorkspace = allWorkspaces.find { it.workspaceId == workspaceId }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showTopBar) {
            WorkspaceDropdownTopBar(
                currentWorkspace = currentWorkspace,
                allWorkspaces = allWorkspaces,
                onWorkspaceSelected = { ws ->
                    if (ws.workspaceId != workspaceId) {
                        navController.navigate(NavRoutes.WorkspaceHome.withArgs(ws.workspaceId)) {
                            popUpTo(NavRoutes.WorkspaceHome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNewWorkspace = {
                    navController.navigate(NavRoutes.NewWorkspace.withArgs(""))
                },
                showBackButton = showBackButton,
                onBackPressed = { navController.popBackStack() },
                actions = actions
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            content()
        }

        if (showBottomBar) {
            BottomBar(
                modifier = Modifier.padding(bottom = 16.dp),
                navController = navController,
                currentWorkspaceId = workspaceId
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavHost(navController: NavHostController, snackbarHostState: SnackbarHostState, viewModels: ViewModels, states: States) {
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

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        notesId,
                        workspaceId,
                        states,
                        viewModels
                    )
                }
            }
        }

        AuthScreen.forEach { (route, screen) ->
            animatedComposable(route) {
                screen(
                    navController,
                    states
                )
            }
        }

        StorageSelectionScreen.forEach { (route, screen) ->
            animatedComposable(route) {
                screen(
                    navController,
                    states,
                    viewModels
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

            if (route.contains("{journalDateTime}")) {
                arguments.add(navArgument("journalDateTime") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val journalId = entry.arguments?.getString("journalId") ?: ""
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""
                val journalDateTime = entry.arguments?.getLong("journalDateTime") ?: 0L

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        journalId,
                        journalDateTime,
                        workspaceId,
                        states,
                        viewModels
                    )
                }
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

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        listId,
                        workspaceId,
                        states,
                        viewModels
                    )
                }
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

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        habitId,
                        workspaceId,
                        states,
                        viewModels
                    )
                }
            }
        }

        SettingsScreens.forEach { (route, screen) ->
            animatedComposable(route) {
                val wsId = states.workspaceState.allWorkspaces.firstOrNull()?.workspaceId ?: ""
                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = wsId,
                    showTopBar = true,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        snackbarHostState,
                        states,
                        viewModels
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

            if (route.contains("{eventDate}")) {
                arguments.add(navArgument("eventDate") {
                    type = NavType.LongType
                    nullable = false
                })
            }

            bottomSlideComposable(route, arguments) { entry ->
                val workspaceId = entry.arguments?.getString("workspaceId") ?: ""
                val eventId = entry.arguments?.getString("eventId") ?: ""
                val instanceDate = entry.arguments?.getLong("instanceDate") ?: 0L
                val eventDate = entry.arguments?.getLong("eventDate") ?: 0L

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(navController,
                        states,
                        viewModels,
                        eventId,
                        workspaceId,
                        instanceDate,
                        eventDate)
                }
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

                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = workspaceId,
                    showTopBar = false,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        states,
                        viewModels,
                        workspaceId
                    )
                }
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

                val isListRoute = route == NavRoutes.Workspace.route
                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = id,
                    showTopBar = isListRoute,  // only show top bar on selection grid
                    showBackButton = !isListRoute,
                    showBottomBar = true
                ) {
                    screen(
                        navController,
                        snackbarHostState,
                        states,
                        viewModels,
                        id
                    )
                }
            }
        }

        SearchScreens.forEach { (route, screen) ->
            animatedComposable(route) {
                val wsId = states.workspaceState.allWorkspaces.firstOrNull()?.workspaceId ?: ""
                WorkspaceScaffold(
                    states = states,
                    viewModels = viewModels,
                    navController = navController,
                    workspaceId = wsId,
                    showTopBar = true,
                    showBackButton = true,
                    showBottomBar = false
                ) {
                    screen(
                        navController,
                        states,
                        viewModels
                    )
                }
            }
        }
    }
}
