package com.flux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AddChart
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.ReportGmailerrorred
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.other.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconButton(
    enabled: Boolean = true,
    imageVector: ImageVector? = null,
    painter: Int? = null,
    contentDescription: String?=null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        } else {
            Icon(
                painter = painterResource(id = painter!!),
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun MarkdownEditorRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onEdit: (String) -> Unit,
    onTableButtonClick: () -> Unit,
    onListButtonClick: () -> Unit,
    onTaskButtonClick: () -> Unit,
    onLinkButtonClick: () -> Unit,
    onImageButtonClick: () -> Unit,
    onAudioButtonClick: () -> Unit,
    onVideoButtonClick: () -> Unit
) {

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isAlertExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 2.dp)
            .navigationBarsPadding()
            .height(48.dp)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomIconButton(
            enabled = canUndo,
            painter = R.drawable.undo,
            contentDescription = "Undo"
        ) {
            onEdit(Constants.Editor.UNDO)
        }

        CustomIconButton(
            enabled = canRedo,
            painter = R.drawable.redo,
            contentDescription = "Redo"
        ) {
            onEdit(Constants.Editor.REDO)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.Title,
            contentDescription = "Heading Level"
        ) {
            isExpanded = !isExpanded
        }

        AnimatedVisibility(visible = isExpanded) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                CustomIconButton(
                    painter = R.drawable.format_h1,
                    contentDescription = "H1"
                ) {
                    onEdit(Constants.Editor.H1)
                }

                CustomIconButton(
                    painter = R.drawable.format_h2,
                    contentDescription = "H2"
                ) {
                    onEdit(Constants.Editor.H2)
                }

                CustomIconButton(
                    painter = R.drawable.format_h3,
                    contentDescription = "H3"
                ) {
                    onEdit(Constants.Editor.H3)
                }

                CustomIconButton(
                    painter = R.drawable.format_h4,
                    contentDescription = "H4"
                ) {
                    onEdit(Constants.Editor.H4)
                }

                CustomIconButton(
                    painter = R.drawable.format_h5,
                    contentDescription = "H5"
                ) {
                    onEdit(Constants.Editor.H5)
                }

                CustomIconButton(
                    painter = R.drawable.format_h6,
                    contentDescription = "H6"
                ) {
                    onEdit(Constants.Editor.H6)
                }
            }
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatBold,
            contentDescription = "Bold"
        ) {
            onEdit(Constants.Editor.BOLD)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatItalic,
            contentDescription = "Italic"
        ) {
            onEdit(Constants.Editor.ITALIC)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatUnderlined,
            contentDescription = "Underline"
        ) {
            onEdit(Constants.Editor.UNDERLINE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.StrikethroughS,
            contentDescription = "Strike Through"
        ) {
            onEdit(Constants.Editor.STRIKETHROUGH)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatPaint,
            contentDescription = "Mark"
        ) {
            onEdit(Constants.Editor.MARK)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.Code,
            contentDescription = "Code"
        ) {
            onEdit(Constants.Editor.INLINE_CODE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.DataArray,
            contentDescription = "Brackets"
        ) {
            onEdit(Constants.Editor.INLINE_BRACKETS)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.DataObject,
            contentDescription = "Braces"
        ) {
            onEdit(Constants.Editor.INLINE_BRACES)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
            contentDescription = "Tab"
        ) {
            onEdit(Constants.Editor.TAB)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.FormatIndentDecrease,
            contentDescription = "unTab"
        ) {
            onEdit(Constants.Editor.UN_TAB)
        }

        CustomIconButton(
            painter = R.drawable.function,
            contentDescription = "Math"
        ) {
            onEdit(Constants.Editor.INLINE_MATH)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = "Quote"
        ) {
            onEdit(Constants.Editor.QUOTE)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = "Alert",
        ) {
            isAlertExpanded = !isAlertExpanded
        }

        AnimatedVisibility(visible = isAlertExpanded) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                CustomIconButton(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Note Alert",
                ) {
                    onEdit(Constants.Editor.NOTE)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = "Tip Alert",
                ) {
                    onEdit(Constants.Editor.TIP)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.Feedback,
                    contentDescription = "Important Alert",
                ) {
                    onEdit(Constants.Editor.IMPORTANT)

                }
                CustomIconButton(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Warning Alert",
                ) {
                    onEdit(Constants.Editor.WARNING)
                }

                CustomIconButton(
                    imageVector = Icons.Outlined.ReportGmailerrorred,
                    contentDescription = "Caution Alert",
                ) {
                    onEdit(Constants.Editor.CAUTION)
                }
            }
        }

        CustomIconButton(
            imageVector = Icons.Outlined.HorizontalRule,
            contentDescription = "Horizontal Rule",
        ) {
            onEdit(Constants.Editor.RULE)
        }

        CustomIconButton(
            imageVector = Icons.Outlined.TableChart,
            contentDescription = "Table",
            onClick = onTableButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.AddChart,
            contentDescription = "Mermaid Diagram",
        ) {
            onEdit(Constants.Editor.DIAGRAM)
        }

        CustomIconButton(
            imageVector = Icons.AutoMirrored.Outlined.List,
            contentDescription = "List",
            onClick = onListButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.CheckBox,
            contentDescription = "Task List",
            onClick = onTaskButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Link,
            contentDescription = "Link",
            onClick = onLinkButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Mic,
            contentDescription = "Audio",
            onClick = onAudioButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.VideoFile,
            contentDescription = "Video",
            onClick = onVideoButtonClick
        )

        CustomIconButton(
            imageVector = Icons.Outlined.Image,
            contentDescription = "Image",
            onClick = onImageButtonClick
        )
    }
}