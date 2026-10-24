package `in`.caffeinelabs.archie.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserState
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkSurface
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer

@Composable
fun BottomOmnibox(
    state: BrowserState,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab = state.activeTab

    AnimatedVisibility(
        visible = state.isOmniboxVisible && !state.isTabSwitcherOpen,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier
    ) {
        val barShape = RoundedCornerShape(28.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                DropdownMenu(
                    expanded = state.isMenuOpen,
                    onDismissRequest = { viewModel.setMenuOpen(false) },
                    modifier = Modifier
                        .background(ArchieDarkSurface)
                        .border(1.dp, ArchieBorder, RoundedCornerShape(14.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("New Tab", color = ArchieTextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = ArchieTextMuted)
                        },
                        onClick = { viewModel.openNewTab(openSearchImmediately = true) }
                    )

                    DropdownMenuItem(
                        text = { Text("Reload", color = ArchieTextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = ArchieTextMuted)
                        },
                        onClick = { viewModel.reload() }
                    )

                    DropdownMenuItem(
                        text = { Text("Share Link", color = ArchieTextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = ArchieTextMuted)
                        },
                        onClick = {
                            viewModel.setMenuOpen(false)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, activeTab.url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Link"))
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (activeTab.isPinned) "Unpin Tab" else "Pin Tab",
                                color = if (activeTab.isPinned) ArchieBlue else ArchieTextPrimary,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (activeTab.isPinned) ArchieBlue else ArchieTextMuted
                            )
                        },
                        onClick = {
                            viewModel.setMenuOpen(false)
                            viewModel.togglePinTab(activeTab.id)
                        }
                    )

                    HorizontalDivider(color = ArchieBorder)

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (activeTab.isDesktopMode) "Request Mobile Site" else "Request Desktop Site",
                                color = if (activeTab.isDesktopMode) ArchieBlue else ArchieTextPrimary,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.DesktopWindows,
                                contentDescription = null,
                                tint = if (activeTab.isDesktopMode) ArchieBlue else ArchieTextMuted
                            )
                        },
                        onClick = { viewModel.toggleDesktopMode() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 12.dp, shape = barShape, spotColor = Color.Black)
                    .clip(barShape)
                    .background(ArchieDarkPill)
                    .border(width = 1.dp, color = ArchieBorder, shape = barShape)
            ) {
                if (activeTab.isLoading) {
                    val progressFloat = (activeTab.loadingProgress.coerceIn(0, 100)) / 100f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.TopCenter),
                        color = ArchieBlue,
                        trackColor = Color.Transparent
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabStackIcon(
                        tabCount = state.tabs.size,
                        onClick = { viewModel.setTabSwitcherOpen(true) }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val pillShape = RoundedCornerShape(22.dp)
                    val displayDomain = UrlSanitizer.extractDisplayDomain(activeTab.url)
                    val isHttps = activeTab.url.startsWith("https://", ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(pillShape)
                            .background(ArchieDarkBg)
                            .border(1.dp, ArchieBorder, pillShape)
                            .clickable { viewModel.setSearchOpen(true) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = if (isHttps) "Secure connection" else "Insecure connection",
                            tint = if (isHttps) ArchieTextMuted else Color(0xFFFF9500),
                            modifier = Modifier.size(13.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (displayDomain.isNotBlank()) displayDomain else "Search or enter URL",
                            color = if (displayDomain.isNotBlank()) ArchieTextPrimary else ArchieTextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.goBack() },
                        enabled = activeTab.canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = if (activeTab.canGoBack) ArchieTextPrimary else ArchieTextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setMenuOpen(!state.isMenuOpen) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Menu",
                            tint = ArchieTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
