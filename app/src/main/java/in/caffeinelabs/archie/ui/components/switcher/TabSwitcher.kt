package `in`.caffeinelabs.archie.ui.components.switcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieDestructive
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary

@Composable
fun TabSwitcher(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isTabSwitcherOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val listState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

        LaunchedEffect(state.isTabSwitcherOpen) {
            if (state.isTabSwitcherOpen) {
                val activeIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }
                if (activeIndex >= 0) {
                    listState.scrollToItem(activeIndex)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ArchieDarkBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tabs (${state.tabs.size})",
                    color = ArchieTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (state.tabs.any { !it.isPinned }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ArchieDarkPill)
                            .border(1.dp, ArchieBorder, RoundedCornerShape(18.dp))
                            .clickable { onAction(BrowserAction.ArchiveNonPinnedTabs) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = "Archive All",
                                tint = ArchieDestructive,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Archive All",
                                color = ArchieDestructive,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            PinnedTabRow(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val cardWidth = maxWidth * 0.76f
                val horizontalContentPadding = (maxWidth - cardWidth) / 2

                LazyRow(
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(horizontal = horizontalContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.tabs, key = { _, tab -> tab.id }) { _, tab ->
                        val isActive = tab.id == state.activeTabId
                        TabCard(
                            tab = tab,
                            isActive = isActive,
                            width = cardWidth,
                            onSelect = { onAction(BrowserAction.SelectTab(tab.id)) },
                            onTogglePin = { onAction(BrowserAction.TogglePinTab(tab.id)) },
                            onClose = { onAction(BrowserAction.CloseTab(tab.id)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.size(44.dp))

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(ArchieDarkPill)
                        .border(1.dp, ArchieBorder, RoundedCornerShape(22.dp))
                        .clickable { onAction(BrowserAction.OpenNewTab(openSearchImmediately = true)) }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "New Tab",
                            tint = ArchieTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New Tab",
                            color = ArchieTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { onAction(BrowserAction.SetTabSwitcherOpen(false)) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ArchieDarkPill)
                        .border(1.dp, ArchieBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Tabs",
                        tint = ArchieTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
