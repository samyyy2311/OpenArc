package `in`.caffeinelabs.archie.ui.components.omnibox

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkSurface
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary

@Composable
fun OmniboxMenu(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab = state.activeTab

    DropdownMenu(
        expanded = state.isMenuOpen,
        onDismissRequest = { onAction(BrowserAction.SetMenuOpen(false)) },
        modifier = modifier
            .background(ArchieDarkSurface)
            .border(1.dp, ArchieBorder, RoundedCornerShape(14.dp))
    ) {
        DropdownMenuItem(
            text = { Text("New Tab", color = ArchieTextPrimary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = ArchieTextMuted)
            },
            onClick = {
                onAction(BrowserAction.OpenNewTab(openSearchImmediately = true))
            }
        )

        DropdownMenuItem(
            text = { Text("Reload", color = ArchieTextPrimary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = ArchieTextMuted)
            },
            onClick = {
                onAction(BrowserAction.SetMenuOpen(false))
                onAction(BrowserAction.Reload)
            }
        )

        DropdownMenuItem(
            text = { Text("Share Link", color = ArchieTextPrimary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = ArchieTextMuted)
            },
            onClick = {
                onAction(BrowserAction.SetMenuOpen(false))
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
                onAction(BrowserAction.SetMenuOpen(false))
                onAction(BrowserAction.TogglePinTab(activeTab.id))
            }
        )

        HorizontalDivider(color = ArchieBorder)

        DropdownMenuItem(
            text = { Text("Site Settings", color = ArchieTextPrimary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = ArchieTextMuted)
            },
            onClick = {
                onAction(BrowserAction.SetSiteSettingsOpen(true))
            }
        )

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
            onClick = {
                onAction(BrowserAction.ToggleDesktopMode(activeTab.id))
            }
        )
    }
}
