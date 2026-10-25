package `in`.caffeinelabs.archie.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.archie.data.model.TabEntity
import `in`.caffeinelabs.archie.data.repository.DefaultTabRepository
import `in`.caffeinelabs.archie.data.repository.TabRepository
import `in`.caffeinelabs.archie.domain.shield.BlockedCategory
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.state.BrowserUiState
import `in`.caffeinelabs.archie.ui.state.UiTab
import `in`.caffeinelabs.archie.util.UrlSanitizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NavigationEffect {
    data class LoadUrl(val tabId: String, val url: String) : NavigationEffect
    data class GoBack(val tabId: String) : NavigationEffect
    data class GoForward(val tabId: String) : NavigationEffect
    data class Reload(val tabId: String) : NavigationEffect
    data class StopLoading(val tabId: String) : NavigationEffect
    data class ApplyDesktopMode(val tabId: String, val enabled: Boolean) : NavigationEffect
    data class ApplyForceDark(val tabId: String, val enabled: Boolean) : NavigationEffect
    data class DestroyTab(val tabId: String) : NavigationEffect
    data class CaptureSnapshot(val tabId: String) : NavigationEffect
}

class BrowserViewModel(
    application: Application,
    private val tabRepository: TabRepository = DefaultTabRepository(application),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<NavigationEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<NavigationEffect> = _effects.asSharedFlow()

    init {
        restoreTabs()
    }

    private fun restoreTabs() {
        viewModelScope.launch(ioDispatcher) {
            val persistedTabs = tabRepository.getActiveTabs()
            if (persistedTabs.isNotEmpty()) {
                val mappedTabs = persistedTabs.map { entity ->
                    UiTab(
                        id = entity.id,
                        url = entity.url,
                        title = entity.title,
                        isPinned = entity.isPinned,
                        isShieldEnabled = entity.isShieldEnabled,
                        isDesktopMode = entity.isDesktopMode,
                        isForceDark = entity.isForceDark
                    )
                }
                _uiState.update { current ->
                    current.copy(
                        tabs = mappedTabs,
                        activeTabId = mappedTabs.first().id
                    )
                }
            } else {
                val initialTab = _uiState.value.tabs.first()
                tabRepository.saveTab(
                    TabEntity(
                        id = initialTab.id,
                        url = initialTab.url,
                        title = initialTab.title,
                        isPinned = false
                    )
                )
            }
        }
    }

    fun onAction(action: BrowserAction) {
        when (action) {
            is BrowserAction.LoadUrl -> handleLoadUrl(action.input, action.browseForMe)
            is BrowserAction.OpenNewTab -> handleOpenNewTab(action.initialUrl, action.openSearchImmediately, action.isPinned)
            is BrowserAction.SelectTab -> handleSelectTab(action.tabId)
            is BrowserAction.CloseTab -> handleCloseTab(action.tabId)
            is BrowserAction.ArchiveNonPinnedTabs -> handleArchiveNonPinnedTabs()
            is BrowserAction.TogglePinTab -> handleTogglePinTab(action.tabId)
            is BrowserAction.ToggleShield -> handleToggleShield(action.tabId)
            is BrowserAction.ToggleDesktopMode -> handleToggleDesktopMode(action.tabId)
            is BrowserAction.ToggleForceDark -> handleToggleForceDark(action.tabId)
            is BrowserAction.SetSearchOpen -> _uiState.update { it.copy(isSearchOpen = action.isOpen, isMenuOpen = false) }
            is BrowserAction.SetTabSwitcherOpen -> handleSetTabSwitcherOpen(action.isOpen)
            is BrowserAction.SetMenuOpen -> _uiState.update { it.copy(isMenuOpen = action.isOpen) }
            is BrowserAction.SetSiteSettingsOpen -> _uiState.update { it.copy(isSiteSettingsOpen = action.isOpen, isMenuOpen = false) }
            is BrowserAction.SearchHistory -> handleSearchHistory(action.query)
            is BrowserAction.GoBack -> _effects.tryEmit(NavigationEffect.GoBack(_uiState.value.activeTabId))
            is BrowserAction.GoForward -> _effects.tryEmit(NavigationEffect.GoForward(_uiState.value.activeTabId))
            is BrowserAction.Reload -> _effects.tryEmit(NavigationEffect.Reload(_uiState.value.activeTabId))
            is BrowserAction.StopLoading -> _effects.tryEmit(NavigationEffect.StopLoading(_uiState.value.activeTabId))
        }
    }

    private fun handleLoadUrl(input: String, browseForMe: Boolean) {
        val resolved = UrlSanitizer.resolveQuery(input, browseForMe = browseForMe)
        val cleaned = UrlSanitizer.cleanUrl(resolved)
        val activeId = _uiState.value.activeTabId

        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == activeId) tab.copy(url = cleaned) else tab
            }
            current.copy(
                tabs = updatedTabs,
                isSearchOpen = false,
                isMenuOpen = false,
                isBrowseForMe = browseForMe
            )
        }
        _effects.tryEmit(NavigationEffect.LoadUrl(activeId, cleaned))
    }

    private fun handleOpenNewTab(initialUrl: String, openSearchImmediately: Boolean, isPinned: Boolean) {
        val url = if (initialUrl.isNotBlank()) {
            UrlSanitizer.cleanUrl(UrlSanitizer.resolveQuery(initialUrl))
        } else {
            "https://en.wikipedia.org"
        }
        val newTab = UiTab(url = url, isPinned = isPinned)

        _uiState.update { current ->
            current.copy(
                tabs = current.tabs + newTab,
                activeTabId = newTab.id,
                isTabSwitcherOpen = false,
                isSearchOpen = openSearchImmediately,
                isMenuOpen = false
            )
        }

        viewModelScope.launch(ioDispatcher) {
            tabRepository.saveTab(
                TabEntity(
                    id = newTab.id,
                    url = newTab.url,
                    title = newTab.title,
                    isPinned = isPinned
                )
            )
        }
    }

    private fun handleSelectTab(tabId: String) {
        _uiState.update { current ->
            current.copy(
                activeTabId = tabId,
                isTabSwitcherOpen = false,
                isMenuOpen = false
            )
        }
    }

    private fun handleCloseTab(tabId: String) {
        _effects.tryEmit(NavigationEffect.DestroyTab(tabId))
        viewModelScope.launch(ioDispatcher) {
            tabRepository.deleteTab(tabId)
        }

        val remaining = _uiState.value.tabs.filter { it.id != tabId }
        if (remaining.isEmpty()) {
            val fresh = UiTab()
            viewModelScope.launch(ioDispatcher) {
                tabRepository.saveTab(
                    TabEntity(fresh.id, fresh.url, fresh.title, isPinned = false)
                )
            }
            _uiState.update { current ->
                current.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    isSearchOpen = true,
                    isTabSwitcherOpen = false
                )
            }
        } else {
            val newActiveId = if (_uiState.value.activeTabId == tabId) {
                val index = _uiState.value.tabs.indexOfFirst { it.id == tabId }
                val targetIndex = (index - 1).coerceAtLeast(0)
                remaining[targetIndex.coerceAtMost(remaining.lastIndex)].id
            } else {
                _uiState.value.activeTabId
            }
            _uiState.update { current ->
                current.copy(tabs = remaining, activeTabId = newActiveId)
            }
        }
    }

    private fun handleArchiveNonPinnedTabs() {
        val nonPinned = _uiState.value.tabs.filter { !it.isPinned }
        nonPinned.forEach {
            _effects.tryEmit(NavigationEffect.DestroyTab(it.id))
        }

        viewModelScope.launch(ioDispatcher) {
            tabRepository.archiveNonPinnedTabs()
        }

        val pinnedTabs = _uiState.value.tabs.filter { it.isPinned }
        if (pinnedTabs.isNotEmpty()) {
            _uiState.update { current ->
                current.copy(
                    tabs = pinnedTabs,
                    activeTabId = pinnedTabs.first().id,
                    isTabSwitcherOpen = false,
                    isMenuOpen = false
                )
            }
        } else {
            val freshTab = UiTab()
            viewModelScope.launch(ioDispatcher) {
                tabRepository.saveTab(
                    TabEntity(freshTab.id, freshTab.url, freshTab.title, isPinned = false)
                )
            }
            _uiState.update { current ->
                current.copy(
                    tabs = listOf(freshTab),
                    activeTabId = freshTab.id,
                    isTabSwitcherOpen = false,
                    isSearchOpen = true,
                    isMenuOpen = false
                )
            }
        }
    }

    private fun handleTogglePinTab(tabId: String) {
        var newPinState = false
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    newPinState = !tab.isPinned
                    tab.copy(isPinned = newPinState)
                } else tab
            }
            current.copy(tabs = updated)
        }
        viewModelScope.launch(ioDispatcher) {
            tabRepository.setPinned(tabId, newPinState)
        }
    }

    private fun handleToggleShield(tabId: String) {
        var newShieldState = true
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    newShieldState = !tab.isShieldEnabled
                    tab.copy(isShieldEnabled = newShieldState)
                } else tab
            }
            current.copy(tabs = updated)
        }
        persistTabPreferences(tabId)
        if (tabId == _uiState.value.activeTabId) {
            _effects.tryEmit(NavigationEffect.Reload(tabId))
        }
    }

    private fun handleToggleDesktopMode(tabId: String) {
        var newDesktopState = false
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    newDesktopState = !tab.isDesktopMode
                    tab.copy(isDesktopMode = newDesktopState)
                } else tab
            }
            current.copy(tabs = updated, isMenuOpen = false)
        }
        persistTabPreferences(tabId)
        _effects.tryEmit(NavigationEffect.ApplyDesktopMode(tabId, newDesktopState))
    }

    private fun handleToggleForceDark(tabId: String) {
        var newForceDark = false
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    newForceDark = !tab.isForceDark
                    tab.copy(isForceDark = newForceDark)
                } else tab
            }
            current.copy(tabs = updated)
        }
        persistTabPreferences(tabId)
        _effects.tryEmit(NavigationEffect.ApplyForceDark(tabId, newForceDark))
    }

    private fun persistTabPreferences(tabId: String) {
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        viewModelScope.launch(ioDispatcher) {
            tabRepository.updatePreferences(
                id = tab.id,
                isShieldEnabled = tab.isShieldEnabled,
                isDesktopMode = tab.isDesktopMode,
                isForceDark = tab.isForceDark
            )
        }
    }

    private fun handleSetTabSwitcherOpen(isOpen: Boolean) {
        if (isOpen) {
            // Request snapshot of active tab prior to opening switcher
            _effects.tryEmit(NavigationEffect.CaptureSnapshot(_uiState.value.activeTabId))
        }
        _uiState.update { it.copy(isTabSwitcherOpen = isOpen, isMenuOpen = false) }
    }

    private fun handleSearchHistory(query: String) {
        viewModelScope.launch(ioDispatcher) {
            val results = tabRepository.searchHistory(query)
            _uiState.update { it.copy(historySearchResults = results) }
        }
    }

    fun onOmniboxScrollDelta(deltaY: Int) {
        if (deltaY > 20) {
            if (_uiState.value.isOmniboxVisible) {
                _uiState.update { it.copy(isOmniboxVisible = false) }
            }
        } else if (deltaY < -20) {
            if (!_uiState.value.isOmniboxVisible) {
                _uiState.update { it.copy(isOmniboxVisible = true) }
            }
        }
    }

    // Engine Event Callbacks
    fun onEnginePageStarted(tabId: String, url: String, favicon: Bitmap?) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = cleaned,
                        favicon = favicon ?: tab.favicon,
                        isLoading = true,
                        loadingProgress = 10,
                        blockedCount = 0
                    )
                } else tab
            }
            current.copy(tabs = updatedTabs, isOmniboxVisible = true)
        }
    }

    fun onEnginePageFinished(tabId: String, url: String) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = cleaned,
                        isLoading = false,
                        loadingProgress = 100
                    )
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }

        val tab = _uiState.value.tabs.find { it.id == tabId }
        if (tab != null) {
            viewModelScope.launch(ioDispatcher) {
                tabRepository.updateMetadata(tabId, cleaned, tab.title)
                tabRepository.recordHistoryVisit(cleaned, tab.title)
            }
        }
    }

    fun onEngineHistoryChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun onEngineProgress(tabId: String, progress: Int) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        loadingProgress = progress,
                        isLoading = progress in 1..99
                    )
                } else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun onEngineTitle(tabId: String, title: String) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(title = title) else tab
            }
            current.copy(tabs = updatedTabs)
        }

        val tab = _uiState.value.tabs.find { it.id == tabId }
        if (tab != null) {
            viewModelScope.launch(ioDispatcher) {
                tabRepository.updateMetadata(tabId, tab.url, title)
                tabRepository.recordHistoryVisit(tab.url, title)
            }
        }
    }

    fun onEngineFavicon(tabId: String, icon: Bitmap) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(favicon = icon) else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun onEngineSnapshotCaptured(tabId: String, snapshot: Bitmap) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(snapshot = snapshot) else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun onEngineTrackerBlocked(tabId: String, url: String, category: BlockedCategory) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(blockedCount = tab.blockedCount + 1) else tab
            }
            current.copy(tabs = updatedTabs)
        }
    }
}
