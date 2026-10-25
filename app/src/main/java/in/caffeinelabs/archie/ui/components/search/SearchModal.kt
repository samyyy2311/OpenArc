package `in`.caffeinelabs.archie.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkSurface
import `in`.caffeinelabs.archie.ui.theme.ArchiePurple
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer

@Composable
fun SearchModal(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialQuery = state.activeTab.url
    var textFieldValue by remember(state.isSearchOpen) {
        val defaultText = if (initialQuery == "https://en.wikipedia.org" || initialQuery.isBlank()) "" else initialQuery
        mutableStateOf(
            TextFieldValue(
                text = defaultText,
                selection = TextRange(0, defaultText.length)
            )
        )
    }

    var isBrowseForMe by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val clipboardContent = remember { clipboardManager.getText()?.text?.trim() }

    LaunchedEffect(state.isSearchOpen) {
        if (state.isSearchOpen) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onAction(BrowserAction.SearchHistory(""))
        }
    }

    LaunchedEffect(textFieldValue.text) {
        onAction(BrowserAction.SearchHistory(textFieldValue.text))
    }

    fun closeSearch() {
        keyboardController?.hide()
        onAction(BrowserAction.SetSearchOpen(false))
    }

    fun submitQuery(raw: String, browseForMe: Boolean) {
        val trimmed = raw.trim()
        if (trimmed.isNotBlank()) {
            keyboardController?.hide()
            onAction(BrowserAction.LoadUrl(trimmed, browseForMe = browseForMe))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArchieDarkBg.copy(alpha = 0.98f))
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

            if (state.historySearchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.historySearchResults, key = { it.url }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { submitQuery(item.url, browseForMe = false) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.title.isNotBlank()) item.title else UrlSanitizer.extractDisplayDomain(item.url),
                                    color = ArchieTextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    color = ArchieTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = isBrowseForMe,
                            onClick = { isBrowseForMe = !isBrowseForMe },
                            label = {
                                Text(
                                    text = "Browse for me",
                                    color = if (isBrowseForMe) Color.White else ArchieTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isBrowseForMe) Color.White else ArchieBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ArchieDarkPill,
                                selectedContainerColor = ArchiePurple
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isBrowseForMe,
                                borderColor = ArchieBorder,
                                selectedBorderColor = ArchiePurple
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }

                    if (!clipboardContent.isNullOrBlank() && clipboardContent != textFieldValue.text) {
                        item {
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
                                        tint = ArchieBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = ArchieDarkPill),
                                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = ArchieBorder),
                                shape = RoundedCornerShape(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(ArchieDarkSurface)
                        .border(1.dp, ArchieBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 16.dp),
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
                        singleLine = true,
                        cursorBrush = SolidColor(ArchieBlue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { submitQuery(textFieldValue.text, isBrowseForMe) }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = "Search or enter URL",
                                        color = ArchieTextMuted,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = { textFieldValue = TextFieldValue("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { submitQuery(textFieldValue.text, isBrowseForMe) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (textFieldValue.text.isNotBlank()) ArchieBlue else ArchieDarkPill)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Go",
                            tint = if (textFieldValue.text.isNotBlank()) Color.White else ArchieTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
