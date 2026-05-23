package com.flux.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.other.icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsTopBar(
    isPinned: Boolean,
    isSearching: Boolean,
    isReadView: Boolean,
    onBackPressed: () -> Unit,
    onOutlineClicked: () -> Unit,
    onReadClick: () -> Unit,
    onEditClick: ()->Unit,
    onDelete: () -> Unit,
    onAddLabel: () -> Unit,
    onSearchClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onAboutClicked: () -> Unit,
    onShareNote: () -> Unit,
    onSaveNote: () -> Unit,
    onPrintNote: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Box(
                    modifier = Modifier
                        .background(
                            if (!isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                6.dp
                            ),
                            RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp))
                        .clickable { onEditClick() }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint= if(!isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(1.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                6.dp
                            ),
                            RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp))
                        .clickable { onReadClick() }
                        .padding(8.dp)
                ) { Icon(Icons.Default.RemoveRedEye, null, tint=if(isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) } },
        actions = {
            if(!isReadView){ IconButton({onSearchClick()}) { Icon(if(isSearching) Icons.Default.SearchOff else Icons.Default.Search, null) } }
            IconButton({onOutlineClicked()}) { Icon(Icons.Default.Summarize, null) }
            DropdownMenuWithDetails(isPinned, onTogglePinned, onAddLabel, onAboutClicked, onShareNote, onSaveNote, onPrintNote, onDelete) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailsTopBar(
    isSearching: Boolean,
    isReadView: Boolean,
    onBackPressed: () -> Unit,
    onOutlineClicked: () -> Unit,
    onReadClick: () -> Unit,
    onEditClick: ()->Unit,
    onDelete: () -> Unit,
    onAddLabel: () -> Unit,
    onSearchClick: () -> Unit,
    onAboutClicked: () -> Unit,
    onShareNote: () -> Unit,
    onSaveNote: () -> Unit,
    onPrintNote: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Box(
                    modifier = Modifier
                        .background(
                            if (!isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                6.dp
                            ),
                            RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp))
                        .clickable { onEditClick() }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint= if(!isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(1.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isReadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                6.dp
                            ),
                            RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp)
                        )
                        .clip(RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp))
                        .clickable { onReadClick() }
                        .padding(8.dp)
                ) { Icon(Icons.Default.RemoveRedEye, null, tint=if(isReadView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) } },
        actions = {
            if(!isReadView){ IconButton({onSearchClick()}) { Icon(if(isSearching) Icons.Default.SearchOff else Icons.Default.Search, null) } }
            IconButton({onOutlineClicked()}) { Icon(Icons.Default.Summarize, null) }
            JournalDropdownMenu(onAddLabel, onAboutClicked, onShareNote, onSaveNote, onPrintNote, onDelete)
        }
    )
}


@Composable
fun SelectedToolBarRow(
    showDeleteOption: Boolean = true,
    selectionCount: Int,
    isAllSelected: Boolean,
    isAllPinned: Boolean,
    onClear: () -> Unit,
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit,
    onToggleSelection: () -> Unit,
){
    val shape = if (!showDeleteOption) RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp) else RectangleShape
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClear){
                Icon(
                    Icons.Filled.Clear,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(selectionCount.toString())
        }

        Row(Modifier.padding(end = 8.dp)) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp)
                    )
                    .clip(RoundedCornerShape(bottomStart = 32.dp, topStart = 32.dp))
                    .clickable { onTogglePin() }
                    .padding(8.dp)
            ) {
                Icon(
                    if(isAllPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(1.dp))
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        shape
                    )
                    .clip(shape)
                    .clickable { onToggleSelection() }
                    .padding(8.dp)
            ) {
                Icon(
                    if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (showDeleteOption){
                Spacer(Modifier.width(1.dp))
                Box(Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                        RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp)
                    )
                    .clip(RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp))
                    .clickable { onDelete() }
                    .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactCard(icon: ImageVector, title: String, onClick: () -> Unit){
    Card(
        modifier = Modifier.clip(RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    icon,
                    null,
                    Modifier.size(18.dp)
                )
            }

            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .widthIn(max = 150.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    description: String,
    cover: String,
    icon: Int,
    isLocked: Boolean = false,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onEditWorkspace: () -> Unit,
    onBackPressed: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit
) {
    val hasCover = cover.isNotBlank()
    val hasDescription = description.isNotBlank()
    val density = LocalDensity.current

    val statusBarHeight  = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val coverHeightDp    = 180.dp
    val toolbarContentDp = 48.dp
    val toolbarHeight    = toolbarContentDp + statusBarHeight

    // Derive title section height purely from typography — no measurement callback needed.
    // Layout: vertical padding (6dp top + 6dp bottom) + icon/title row + optional (2dp spacer + description row)
    val titleStyle        = MaterialTheme.typography.titleLarge
    val bodyStyle         = MaterialTheme.typography.bodyMedium
    val titleLineHeightDp = with(density) { titleStyle.lineHeight.toDp() }
    val bodyLineHeightDp  = with(density) { bodyStyle.lineHeight.toDp() }
    val titleRowDp        = maxOf(24.dp, titleLineHeightDp) // Icon default size is 24.dp
    val titleSectionHeightDp = 12.dp + titleRowDp + if (hasDescription) 2.dp + bodyLineHeightDp else 0.dp

    val expandedHeightDp = if (hasCover)
        coverHeightDp + titleSectionHeightDp + statusBarHeight - 32.dp
    else
        toolbarHeight + titleSectionHeightDp + 24.dp

    val expandedPx  = with(density) { expandedHeightDp.toPx() }
    val collapsedPx = with(density) { toolbarHeight.toPx() }

    SideEffect {
        val limit = collapsedPx - expandedPx
        if (scrollBehavior.state.heightOffsetLimit != limit) {
            scrollBehavior.state.heightOffsetLimit = limit
        }
    }

    val fraction            = scrollBehavior.state.collapsedFraction
    val currentHeight       = lerp(expandedHeightDp, toolbarHeight, fraction)
    val coverAlpha          = (1f - fraction * 1.5f).coerceIn(0f, 1f)
    val expandedTitleAlpha  = (1f - fraction * 2f).coerceIn(0f, 1f)
    val collapsedTitleAlpha = ((fraction - 0.5f) * 2f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(currentHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // ── Cover + title/description stacked flush ───────────────────
        Column(Modifier.fillMaxWidth()) {
            if (hasCover) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lerp(coverHeightDp, 0.dp, fraction))
                        .alpha(coverAlpha)
                )
            } else {
                Spacer(Modifier.height(toolbarHeight))
            }

            // Title + description — always visible, fades on scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(expandedTitleAlpha)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icons[icon], null)
                    Text(
                        text = title,
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hasDescription) {
                    Text(
                        text = description,
                        style = bodyStyle,
                        fontWeight = FontWeight.ExtraLight,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Toolbar — overlaid, always on top ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(toolbarHeight)
                .padding(top = statusBarHeight, start = 8.dp, end = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .alpha(collapsedTitleAlpha)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasDescription) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            WorkspaceMore(
                isCoverAdded = hasCover,
                isLocked = isLocked,
                onDelete = onDeleteWorkspace,
                onEditDetails = onEditWorkspace,
                onRemoveCover = onRemoveCover,
                onAddCover = onAddCover,
                onToggleLock = onToggleLock
            )
        }
    }
}

sealed class Destination(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : Destination(title = "Home", route = NavRoutes.Workspace.route, selectedIcon = Icons.Filled.Home, unselectedIcon = Icons.Outlined.Home)
    data object Search : Destination(title = "Search", route = NavRoutes.Search.route, selectedIcon = Icons.Filled.Search, unselectedIcon = Icons.Outlined.Search)
    data object Settings : Destination(title = "Settings", route = NavRoutes.Settings.route, selectedIcon = Icons.Filled.Settings, unselectedIcon = Icons.Outlined.Settings)
}

@Composable
fun BottomBar(modifier: Modifier, navController: NavController) {
    val screens = listOf(Destination.Home, Destination.Search, Destination.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen->
                BottomBarCard(
                    screen = screen,
                    currentDestination = currentDestination,
                    navController = navController
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            {navController.navigate(NavRoutes.NewWorkspace.withArgs(""))},
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Icon(Icons.Default.Add, null) }
    }
}

@Composable
fun BottomBarCard(
    screen: Destination,
    currentDestination: NavDestination?,
    navController: NavController
){
    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

    val containerColor = if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val contentColor = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        onClick = { if (currentDestination?.route != screen.route) { navController.navigate(screen.route) } },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation= CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(if(selected) screen.selectedIcon else screen.unselectedIcon, null)
            if(selected) Text(screen.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
fun SpaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.Search_Here)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({}){
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    innerTextField()
                }
            )

            IconButton( {
                onCloseClicked()
                onQueryChange("")
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
