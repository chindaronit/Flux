package com.flux.ui.screens.journal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.flux.R
import com.flux.data.model.JournalModel
import com.flux.ui.components.DatePickerModal
import com.flux.ui.components.RichTextStyleRow
import com.flux.ui.events.JournalEvents
import com.flux.ui.screens.events.toFormattedDate
import com.flux.ui.screens.workspaces.copyToInternalStorage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJournal(
    navController: NavController,
    journal: JournalModel,
    onJournalEvents: (JournalEvents) -> Unit
) {
    val isToday =
        LocalDate.now() == Instant.ofEpochMilli(journal.dateTime).atZone(ZoneId.systemDefault())
            .toLocalDate()
    val context = LocalContext.current
    val richTextState = rememberRichTextState()
    val interactionSource = remember { MutableInteractionSource() }
    val pickedImages = remember { mutableStateListOf<String>().apply { addAll(journal.images) } }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateTime by remember { mutableLongStateOf(journal.dateTime) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                pickedImages.add(
                    copyToInternalStorage(
                        context,
                        uri
                    ).toString()
                )
            }
        }
    )

    if (showDatePicker) {
        DatePickerModal(onDateSelected = { newDateMillis ->
            if (newDateMillis != null) {
                selectedDateTime = newDateMillis
            }
        }, onDismiss = { showDatePicker = false })
    }

    LaunchedEffect(journal.journalId) { richTextState.setHtml(journal.text) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                title = { Text(selectedDateTime.toFormattedDate()) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton({
                        onJournalEvents(
                            JournalEvents.UpsertEntry(
                                journal.copy(
                                    text = richTextState.toHtml(),
                                    images = pickedImages.toList(),
                                    dateTime = if (isToday) System.currentTimeMillis() else journal.dateTime
                                )
                            )
                        )
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Check, null)
                    }
                    IconButton({
                        navController.popBackStack()
                        onJournalEvents(JournalEvents.DeleteEntry(journal))
                    }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Carousel(pickedImages) { pickedImages.remove(it) }

            RichTextEditor(
                state = richTextState,
                interactionSource = interactionSource,
                placeholder = { Text(stringResource(R.string.Write_here)) },
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 12.dp),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    placeholderColor = MaterialTheme.colorScheme.primary,
                )
            )

            Row {
                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = richTextState,
                    isAddImage = true
                ) {
                    imagePickerLauncher.launch("image/*")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Carousel(items: List<String>, onItemRemoved: (String) -> Unit) {
    if (items.isNotEmpty()) {
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { items.count() },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 9.dp, bottom = 8.dp),
            preferredItemWidth = 160.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val item = items[i]
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
            ) {
                AsyncImage(
                    model = item,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = { onItemRemoved(item) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete image")
                }
            }
        }
    }
}