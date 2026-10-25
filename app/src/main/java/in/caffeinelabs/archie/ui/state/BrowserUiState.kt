package `in`.caffeinelabs.archie.ui.state

import android.graphics.Bitmap
import `in`.caffeinelabs.archie.data.model.HistoryEntity
import java.util.UUID

data class UiTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://en.wikipedia.org",
    val title: String = "",
    val favicon: Bitmap? = null,
    val snapshot: Bitmap? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loadingProgress: Int = 0,
    val isLoading: Boolean = false,
    val isPinned: Boolean = false,
    val isShieldEnabled: Boolean = true,
    val blockedCount: Int = 0,
    val isDesktopMode: Boolean = false,
    val isForceDark: Boolean = false
)

data class BrowserUiState(
    val tabs: List<UiTab> = listOf(UiTab()),
    val activeTabId: String = tabs.first().id,
    val isSearchOpen: Boolean = false,
    val isTabSwitcherOpen: Boolean = false,
    val isMenuOpen: Boolean = false,
    val isSiteSettingsOpen: Boolean = false,
    val isFindInPageOpen: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val selectedSearchEngine: String = "Google",
    val isGlobalShieldEnabled: Boolean = true,
    val findQuery: String = "",
    val findCurrentMatch: Int = 0,
    val findTotalMatches: Int = 0,
    val isBrowseForMe: Boolean = false,
    val isOmniboxVisible: Boolean = true,
    val historySearchResults: List<HistoryEntity> = emptyList(),
    val historyItems: List<HistoryEntity> = emptyList()
) {
    val activeTab: UiTab
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: UiTab()
}
