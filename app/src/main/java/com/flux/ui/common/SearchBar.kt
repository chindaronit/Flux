package com.flux.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.flux.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSearchBar(
    textFieldState: TextFieldState,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onLeadingIconClicked: () -> Unit = {},
    onTrailingIconClicked: () -> Unit = {},
    onSearch: (String) -> Unit,
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
                GeneralSearchInputField(
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
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    onLeadingIconClicked = onLeadingIconClicked,
                    onTrailingIconClicked = onTrailingIconClicked
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
fun GeneralSearchInputField(
    query: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onLeadingIconClicked: () -> Unit = {},
    onTrailingIconClicked: () -> Unit = {}
) {
    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = false,
        onExpandedChange = { },
        placeholder = { Text(stringResource(R.string.Search_Here)) },
        leadingIcon = leadingIcon?.let {
            {
                IconButton(onClick = onLeadingIconClicked) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingIcon = {
            Row {
                if (query.isNotBlank()) CloseButton(onSearchClosed)
                trailingIcon?.let {
                    IconButton(onClick = onTrailingIconClicked) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp), focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    )
}