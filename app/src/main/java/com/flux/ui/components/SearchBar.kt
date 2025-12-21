package com.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.flux.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    onSettingsClicked: () -> Unit,
    onCloseClicked: () -> Unit,
) {
    val query = textFieldState.text.toString()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .wrapContentHeight()
                .semantics { traversalIndex = 0f },
            inputField = {
                WorkspaceSearchInputField(
                    query,
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        onSearch(it)
                        if (it.isBlank()) {
                            expanded = false
                        }
                    },
                    onSearch = {
                        keyboardController?.hide()
                        onSearch(query)
                    },
                    onSearchClosed = {
                        textFieldState.edit { replace(0, length, "") }
                        onCloseClicked()
                        onSearch("")
                        expanded = false
                    },
                    onSettingsClicked = onSettingsClicked
                )
            },
            colors = SearchBarDefaults.colors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
            expanded = expanded,
            onExpandedChange = { },
        ) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = false,
        onExpandedChange = { },
        placeholder = { Text(stringResource(R.string.Search_Workspaces)) },
        leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            Row {
                if (query.isNotBlank()) CloseButton(onSearchClosed)
                SettingsButton(onSettingsClicked)
            }
        },
        colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp), focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    )
}

@Composable
fun NotesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(6.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp), // keeps text centered vertically
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = {
                    onCloseClicked()
                    onQueryChange("")
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}