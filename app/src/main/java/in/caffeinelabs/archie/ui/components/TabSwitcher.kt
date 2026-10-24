package `in`.caffeinelabs.archie.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserState
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.state.Tab
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkSurface
import `in`.caffeinelabs.archie.ui.theme.ArchieDestructive
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TabSwitcher(
    state: BrowserState,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(state.activeTabId) {
        val activeIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }
        if (activeIndex >= 0) {
            listState.scrollToItem(activeIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArchieDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tabs",
                    color = ArchieTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArchieDarkPill)
                        .border(1.dp, ArchieBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.tabs.size}",
                        color = ArchieBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (state.tabs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.archiveAllTabs() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ArchieDestructive)
                    ) {
                        Text(
                            text = "Archive All",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

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
                            onSelect = { viewModel.selectTab(tab.id) },
                            onClose = { viewModel.closeTab(tab.id) }
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
                        .clickable { viewModel.openNewTab(openSearchImmediately = true) }
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
                    onClick = { viewModel.setTabSwitcherOpen(false) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ArchieDarkPill)
                        .border(1.dp, ArchieBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Library",
                        tint = ArchieTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: Tab,
    isActive: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    var isDismissed by remember { androidx.compose.runtime.mutableStateOf(false) }

    val displayDomain = UrlSanitizer.extractDisplayDomain(tab.url)
    val cardCornerRadius = 22.dp
    val shape = RoundedCornerShape(cardCornerRadius)

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight(0.82f)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .alpha(1f - (kotlin.math.abs(offsetY.value) / 800f).coerceIn(0f, 1f))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta < 0 || offsetY.value < 0) {
                        coroutineScope.launch {
                            offsetY.snapTo(offsetY.value + delta)
                        }
                    }
                },
                onDragStopped = { velocity ->
                    if (offsetY.value < -250f || velocity < -1000f) {
                        coroutineScope.launch {
                            offsetY.animateTo(-1200f)
                            onClose()
                        }
                    } else {
                        coroutineScope.launch {
                            offsetY.animateTo(0f, spring())
                        }
                    }
                }
            )
            .shadow(
                elevation = if (isActive) 18.dp else 8.dp,
                shape = shape,
                spotColor = Color.Black
            )
            .clip(shape)
            .background(ArchieDarkSurface)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) ArchieBlue else ArchieBorder,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArchieDarkPill)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.favicon != null) {
                    Image(
                        bitmap = tab.favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = ArchieTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (tab.title.isNotBlank()) tab.title else displayDomain.ifBlank { "New Tab" },
                    color = ArchieTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close tab",
                        tint = ArchieTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayDomain.ifBlank { "Archie" },
                        color = ArchieTextPrimary.copy(alpha = 0.85f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tab.url,
                        color = ArchieTextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
