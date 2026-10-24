package `in`.caffeinelabs.archie.ui.state

import android.graphics.Bitmap
import android.view.View
import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://en.wikipedia.org",
    val title: String = "",
    val favicon: Bitmap? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loadingProgress: Int = 0,
    val isLoading: Boolean = false,
    val isDesktopMode: Boolean = false,
    val themeColor: Long? = null
)

data class BrowserState(
    val tabs: List<Tab> = listOf(Tab()),
    val activeTabId: String = tabs.first().id,
    val isSearchOpen: Boolean = false,
    val isTabSwitcherOpen: Boolean = false,
    val isMenuOpen: Boolean = false,
    val isBrowseForMe: Boolean = false,
    val customFullscreenView: View? = null,
    val isOmniboxVisible: Boolean = true
) {
    val activeTab: Tab
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: Tab()
}
