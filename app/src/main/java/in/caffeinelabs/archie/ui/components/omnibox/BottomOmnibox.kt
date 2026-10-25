package `in`.caffeinelabs.archie.ui.components.omnibox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.components.TabStackIcon
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkBg
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer

@Composable
fun BottomOmnibox(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = state.activeTab

    AnimatedVisibility(
        visible = state.isOmniboxVisible && !state.isTabSwitcherOpen && !state.isFindInPageOpen,
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
                OmniboxMenu(state = state, onAction = onAction)
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
                        onClick = { onAction(BrowserAction.SetTabSwitcherOpen(true)) }
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
                            .clickable { onAction(BrowserAction.SetSearchOpen(true)) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAction(BrowserAction.SetSiteSettingsOpen(true)) }
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = if (isHttps) "Secure connection" else "Insecure connection",
                                tint = if (isHttps) ArchieTextMuted else Color(0xFFFF9500),
                                modifier = Modifier.size(13.dp)
                            )
                        }

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

                        if (activeTab.isShieldEnabled && activeTab.blockedCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ArchieBlue.copy(alpha = 0.18f))
                                    .clickable { onAction(BrowserAction.SetSiteSettingsOpen(true)) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = "Shield",
                                        tint = ArchieBlue,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${activeTab.blockedCount}",
                                        color = ArchieBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { onAction(BrowserAction.GoBack) },
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
                        onClick = { onAction(BrowserAction.SetMenuOpen(!state.isMenuOpen)) },
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
