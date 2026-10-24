package `in`.caffeinelabs.archie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary

@Composable
fun SearchModal(
    initialQuery: String,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialQuery,
                selection = TextRange(0, initialQuery.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val clipboardContent = remember { clipboardManager.getText()?.text?.trim() }

    fun closeSearch() {
        keyboardController?.hide()
        viewModel.setSearchOpen(false)
    }

    fun submitQuery(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isNotBlank()) {
            keyboardController?.hide()
            viewModel.loadUrl(trimmed)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArchieDarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                closeSearch()
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { closeSearch() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close search",
                        tint = ArchieTextMuted
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!clipboardContent.isNullOrBlank() && clipboardContent != textFieldValue.text) {
                    AssistChip(
                        onClick = {
                            textFieldValue = TextFieldValue(
                                text = clipboardContent,
                                selection = TextRange(clipboardContent.length)
                            )
                        },
                        label = {
                            Text(
                                text = "Paste: $clipboardContent",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = ArchieTextPrimary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = null,
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ArchieDarkPill,
                            labelColor = ArchieTextPrimary
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = ArchieBorder,
                            borderWidth = 1.dp,
                            enabled = true
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                val pillShape = RoundedCornerShape(26.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(pillShape)
                        .background(ArchieDarkPill)
                        .border(1.dp, ArchieBorder, pillShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = ArchieTextMuted,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = ArchieTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(ArchieBlue),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { submitQuery(textFieldValue.text) }
                        ),
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Search or enter URL",
                                    color = ArchieTextMuted,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = { textFieldValue = TextFieldValue("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear text",
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ArchieBlue)
                                .clickable { submitQuery(textFieldValue.text) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Go",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
