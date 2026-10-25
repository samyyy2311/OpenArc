package `in`.caffeinelabs.archie.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import `in`.caffeinelabs.archie.ui.theme.ArchieTeal
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer

@Composable
fun SiteSettingsSheet(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = state.activeTab
    val displayDomain = UrlSanitizer.extractDisplayDomain(activeTab.url).ifBlank { "Current Site" }
    val isHttps = activeTab.url.startsWith("https://", ignoreCase = true)

    AnimatedVisibility(
        visible = state.isSiteSettingsOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAction(BrowserAction.SetSiteSettingsOpen(false)) }
                )
        ) {
            AnimatedVisibility(
                visible = state.isSiteSettingsOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(ArchieDarkSurface)
                        .border(1.dp, ArchieBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ArchieDarkPill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHttps) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = if (isHttps) ArchieTeal else ArchieTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = displayDomain,
                                    color = ArchieTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isHttps) "Secure connection (HTTPS)" else "Insecure connection (HTTP)",
                                    color = if (isHttps) ArchieTeal else ArchieTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { onAction(BrowserAction.SetSiteSettingsOpen(false)) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ArchieDarkPill)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = ArchieTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = ArchieBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingToggleRow(
                        icon = Icons.Outlined.Shield,
                        iconTint = if (activeTab.isShieldEnabled) ArchieBlue else ArchieTextMuted,
                        title = "Ad & Tracker Shield",
                        subtitle = if (activeTab.isShieldEnabled) {
                            if (activeTab.blockedCount > 0) "Blocked ${activeTab.blockedCount} ads & trackers" else "Protection active"
                        } else {
                            "Protection disabled for this site"
                        },
                        checked = activeTab.isShieldEnabled,
                        onCheckedChange = { onAction(BrowserAction.ToggleShield(activeTab.id)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        icon = Icons.Outlined.DesktopWindows,
                        iconTint = if (activeTab.isDesktopMode) ArchieBlue else ArchieTextMuted,
                        title = "Desktop Site",
                        subtitle = "Request desktop version of pages",
                        checked = activeTab.isDesktopMode,
                        onCheckedChange = { onAction(BrowserAction.ToggleDesktopMode(activeTab.id)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        icon = Icons.Outlined.DarkMode,
                        iconTint = if (activeTab.isForceDark) ArchieBlue else ArchieTextMuted,
                        title = "Force Dark Theme",
                        subtitle = "Invert web content for low-light reading",
                        checked = activeTab.isForceDark,
                        onCheckedChange = { onAction(BrowserAction.ToggleForceDark(activeTab.id)) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArchieDarkPill)
            .border(1.dp, ArchieBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = ArchieTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = ArchieTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ArchieBlue,
                uncheckedThumbColor = ArchieTextMuted,
                uncheckedTrackColor = ArchieDarkBg,
                uncheckedBorderColor = ArchieBorder
            )
        )
    }
}
