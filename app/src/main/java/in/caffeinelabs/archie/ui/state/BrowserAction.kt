package `in`.caffeinelabs.archie.ui.state

sealed interface BrowserAction {
    data class LoadUrl(val input: String, val browseForMe: Boolean = false) : BrowserAction
    data class OpenNewTab(
        val initialUrl: String = "",
        val openSearchImmediately: Boolean = true,
        val isPinned: Boolean = false
    ) : BrowserAction
    data class SelectTab(val tabId: String) : BrowserAction
    data class CloseTab(val tabId: String) : BrowserAction
    data object ArchiveNonPinnedTabs : BrowserAction
    data class TogglePinTab(val tabId: String) : BrowserAction
    data class ToggleShield(val tabId: String) : BrowserAction
    data class ToggleDesktopMode(val tabId: String) : BrowserAction
    data class ToggleForceDark(val tabId: String) : BrowserAction
    data class SetSearchOpen(val isOpen: Boolean) : BrowserAction
    data class SetTabSwitcherOpen(val isOpen: Boolean) : BrowserAction
    data class SetMenuOpen(val isOpen: Boolean) : BrowserAction
    data class SetSiteSettingsOpen(val isOpen: Boolean) : BrowserAction
    data class SearchHistory(val query: String) : BrowserAction
    data object GoBack : BrowserAction
    data object GoForward : BrowserAction
    data object Reload : BrowserAction
    data object StopLoading : BrowserAction
}
