package `in`.caffeinelabs.archie.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
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
import `in`.caffeinelabs.archie.ui.state.BrowserState
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.util.UrlSanitizer

@Composable
fun BottomOmnibox(
    state: BrowserState,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isOmniboxVisible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(26.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(elevation = 12.dp, shape = shape, spotColor = Color.Black)
                .clip(shape)
                .background(ArchieDarkPill)
                .border(width = 1.dp, color = ArchieBorder, shape = shape)
                .clickable { viewModel.setSearchOpen(true) }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHttps = state.currentUrl.startsWith("https://", ignoreCase = true)
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = if (isHttps) "Secure connection" else "Connection info",
                        tint = if (isHttps) ArchieTextMuted else Color(0xFFFF9500),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val displayDomain = UrlSanitizer.extractDisplayDomain(state.currentUrl)
                    val label = if (displayDomain.isNotBlank()) displayDomain else "Search or type URL"

                    Text(
                        text = label,
                        color = if (displayDomain.isNotBlank()) ArchieTextPrimary else ArchieTextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (state.isLoading) {
                        IconButton(
                            onClick = { viewModel.stopLoading() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Stop loading",
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Reload page",
                                tint = ArchieTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    val progressFloat = (state.loadingProgress.coerceIn(0, 100)) / 100f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = ArchieBlue,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}
