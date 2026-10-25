package `in`.caffeinelabs.archie.ui.components.find

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary

@Composable
fun FindInPageBar(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isFindInPageOpen,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier
    ) {
        val barShape = RoundedCornerShape(28.dp)
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(state.isFindInPageOpen) {
            if (state.isFindInPageOpen) {
                focusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 12.dp, shape = barShape, spotColor = Color.Black)
                    .clip(barShape)
                    .background(ArchieDarkPill)
                    .border(width = 1.dp, color = ArchieBorder, shape = barShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = ArchieTextMuted,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (state.findQuery.isEmpty()) {
                        Text(
                            text = "Find in page…",
                            color = ArchieTextMuted,
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = state.findQuery,
                        onValueChange = { onAction(BrowserAction.SetFindQuery(it)) },
                        textStyle = TextStyle(
                            color = ArchieTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(ArchieBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onAction(BrowserAction.FindNext(true)) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (state.findQuery.isNotBlank()) {
                    val matchText = if (state.findTotalMatches > 0) {
                        "${state.findCurrentMatch + 1} of ${state.findTotalMatches}"
                    } else {
                        "0 of 0"
                    }
                    Text(
                        text = matchText,
                        color = ArchieTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onAction(BrowserAction.FindNext(forward = false)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Previous match",
                            tint = if (state.findTotalMatches > 0) ArchieTextPrimary else ArchieTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onAction(BrowserAction.FindNext(forward = true)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Next match",
                            tint = if (state.findTotalMatches > 0) ArchieTextPrimary else ArchieTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onAction(BrowserAction.SetFindInPageOpen(false)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close search",
                        tint = ArchieTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
