package `in`.caffeinelabs.archie.ui.state

import android.annotation.SuppressLint
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.archie.data.TabDatabase
import `in`.caffeinelabs.archie.util.UrlSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NavigationCommand {
    data class LoadUrl(val url: String) : NavigationCommand
    data object GoBack : NavigationCommand
    data object GoForward : NavigationCommand
    data object Reload : NavigationCommand
    data object StopLoading : NavigationCommand
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val tabDatabase = TabDatabase(application)

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    private val webViewPool = mutableMapOf<String, WebView>()
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val records = tabDatabase.getAllActiveTabs()
            if (records.isNotEmpty()) {
                val loadedTabs = records.map {
                    Tab(
                        id = it.id,
                        url = it.url,
                        title = it.title,
                        isPinned = it.isPinned
                    )
                }
                _state.update { current ->
                    current.copy(
                        tabs = loadedTabs,
                        activeTabId = loadedTabs.first().id
                    )
                }
            } else {
                val defaultTab = _state.value.tabs.first()
                tabDatabase.saveTab(
                    id = defaultTab.id,
                    url = defaultTab.url,
                    title = defaultTab.title,
                    isPinned = false
                )
            }
        }
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(tabId: String, context: Context): WebView {
        val existing = webViewPool[tabId]
        if (existing != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        val tab = _state.value.tabs.find { it.id == tabId }
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false

                if (tab?.isDesktopMode == true) {
                    userAgentString = DESKTOP_USER_AGENT
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    val url = uri.toString()
                    val scheme = uri.scheme?.lowercase() ?: return false

                    if (scheme == "http" || scheme == "https") {
                        val cleaned = UrlSanitizer.cleanUrl(url)
                        if (cleaned != url) {
                            view?.loadUrl(cleaned)
                            return true
                        }
                        return false
                    }

                    if (scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "javascript") {
                        return false
                    }

                    return try {
                        val parsedIntent = if (scheme == "intent") {
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        parsedIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                        parsedIntent.component = null
                        parsedIntent.selector = null
                        parsedIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(parsedIntent)
                        true
                    } catch (_: ActivityNotFoundException) {
                        true
                    } catch (_: SecurityException) {
                        true
                    } catch (_: Exception) {
                        true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStartedForTab(tabId, it, favicon) }
                    view?.let { updateHistoryForTab(tabId, it.canGoBack(), it.canGoForward()) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { onPageFinishedForTab(tabId, it) }
                    view?.let { updateHistoryForTab(tabId, it.canGoBack(), it.canGoForward()) }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    view?.let { updateHistoryForTab(tabId, it.canGoBack(), it.canGoForward()) }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgressChangedForTab(tabId, newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    onTitleChangedForTab(tabId, title ?: "")
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    onFaviconChangedForTab(tabId, icon)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view != null && callback != null) {
                        this@BrowserViewModel.onShowCustomView(view, callback)
                    }
                }

                override fun onHideCustomView() {
                    this@BrowserViewModel.onHideCustomView()
                }
            }

            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val delta = scrollY - oldScrollY
                if (delta > 20) {
                    setOmniboxVisible(false)
                } else if (delta < -20 || scrollY <= 16) {
                    setOmniboxVisible(true)
                }
            }
        }

        webViewPool[tabId] = webView
        val initialUrl = tab?.url ?: "https://en.wikipedia.org"
        if (initialUrl.isNotBlank()) {
            webView.loadUrl(initialUrl)
        }
        return webView
    }

    fun openNewTab(url: String = "", openSearchImmediately: Boolean = true, isPinned: Boolean = false) {
        val initialUrl = if (url.isNotBlank()) UrlSanitizer.cleanUrl(UrlSanitizer.resolveQuery(url)) else "https://en.wikipedia.org"
        val newTab = Tab(url = initialUrl, isPinned = isPinned)
        _state.update { current ->
            current.copy(
                tabs = current.tabs + newTab,
                activeTabId = newTab.id,
                isTabSwitcherOpen = false,
                isSearchOpen = openSearchImmediately,
                isMenuOpen = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            tabDatabase.saveTab(newTab.id, newTab.url, newTab.title, isPinned)
        }
    }

    fun selectTab(tabId: String) {
        _state.update {
            it.copy(
                activeTabId = tabId,
                isTabSwitcherOpen = false,
                isMenuOpen = false
            )
        }
        val tab = _state.value.tabs.find { it.id == tabId }
        if (tab != null) {
            viewModelScope.launch(Dispatchers.IO) {
                tabDatabase.updateTabMetadata(tabId, tab.url, tab.title)
            }
        }
    }

    fun togglePinTab(tabId: String) {
        val tab = _state.value.tabs.find { it.id == tabId } ?: return
        val newPinned = !tab.isPinned
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) it.copy(isPinned = newPinned) else it
            }
            current.copy(tabs = updatedTabs)
        }
        viewModelScope.launch(Dispatchers.IO) {
            tabDatabase.setPinned(tabId, newPinned)
        }
    }

    fun closeTab(tabId: String) {
        webViewPool.remove(tabId)?.apply {
            stopLoading()
            destroy()
        }

        viewModelScope.launch(Dispatchers.IO) {
            tabDatabase.deleteTab(tabId)
        }

        val remaining = _state.value.tabs.filter { it.id != tabId }
        if (remaining.isEmpty()) {
            val fresh = Tab()
            viewModelScope.launch(Dispatchers.IO) {
                tabDatabase.saveTab(fresh.id, fresh.url, fresh.title, false)
            }
            _state.update {
                it.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    isSearchOpen = true,
                    isTabSwitcherOpen = false
                )
            }
        } else {
            val newActiveId = if (_state.value.activeTabId == tabId) {
                val index = _state.value.tabs.indexOfFirst { it.id == tabId }
                val targetIndex = (index - 1).coerceAtLeast(0)
                remaining[targetIndex.coerceAtMost(remaining.lastIndex)].id
            } else {
                _state.value.activeTabId
            }
            _state.update {
                it.copy(
                    tabs = remaining,
                    activeTabId = newActiveId
                )
            }
        }
    }

    fun archiveAllTabs() {
        val pinnedTabs = _state.value.tabs.filter { it.isPinned }
        val nonPinnedIds = _state.value.tabs.filter { !it.isPinned }.map { it.id }

        nonPinnedIds.forEach { id ->
            webViewPool.remove(id)?.apply {
                stopLoading()
                destroy()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            tabDatabase.archiveNonPinnedTabs()
        }

        if (pinnedTabs.isNotEmpty()) {
            _state.update {
                it.copy(
                    tabs = pinnedTabs,
                    activeTabId = pinnedTabs.first().id,
                    isTabSwitcherOpen = false,
                    isMenuOpen = false
                )
            }
        } else {
            val freshTab = Tab()
            viewModelScope.launch(Dispatchers.IO) {
                tabDatabase.saveTab(freshTab.id, freshTab.url, freshTab.title, false)
            }
            _state.update {
                it.copy(
                    tabs = listOf(freshTab),
                    activeTabId = freshTab.id,
                    isTabSwitcherOpen = false,
                    isSearchOpen = true,
                    isMenuOpen = false
                )
            }
        }
    }

    fun setTabSwitcherOpen(isOpen: Boolean) {
        _state.update { it.copy(isTabSwitcherOpen = isOpen, isMenuOpen = false) }
    }

    fun setMenuOpen(isOpen: Boolean) {
        _state.update { it.copy(isMenuOpen = isOpen) }
    }

    fun setSearchOpen(isOpen: Boolean) {
        _state.update { it.copy(isSearchOpen = isOpen, isMenuOpen = false) }
    }

    fun setOmniboxVisible(visible: Boolean) {
        if (_state.value.isOmniboxVisible != visible) {
            _state.update { it.copy(isOmniboxVisible = visible) }
        }
    }

    fun setBrowseForMe(enabled: Boolean) {
        _state.update { it.copy(isBrowseForMe = enabled) }
    }

    fun loadUrl(input: String, browseForMe: Boolean = false) {
        val resolved = UrlSanitizer.resolveQuery(input, browseForMe = browseForMe)
        val cleaned = UrlSanitizer.cleanUrl(resolved)
        val activeId = _state.value.activeTabId

        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == activeId) it.copy(url = cleaned) else it
            }
            current.copy(tabs = updatedTabs, isSearchOpen = false, isMenuOpen = false, isBrowseForMe = browseForMe)
        }

        webViewPool[activeId]?.loadUrl(cleaned) ?: viewModelScope.launch {
            _commands.emit(NavigationCommand.LoadUrl(cleaned))
        }
    }

    fun goBack() {
        val activeId = _state.value.activeTabId
        val webView = webViewPool[activeId]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            viewModelScope.launch {
                _commands.emit(NavigationCommand.GoBack)
            }
        }
    }

    fun goForward() {
        val activeId = _state.value.activeTabId
        val webView = webViewPool[activeId]
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
        } else {
            viewModelScope.launch {
                _commands.emit(NavigationCommand.GoForward)
            }
        }
    }

    fun reload() {
        val activeId = _state.value.activeTabId
        webViewPool[activeId]?.reload() ?: viewModelScope.launch {
            _commands.emit(NavigationCommand.Reload)
        }
        _state.update { it.copy(isMenuOpen = false) }
    }

    fun stopLoading() {
        val activeId = _state.value.activeTabId
        webViewPool[activeId]?.stopLoading() ?: viewModelScope.launch {
            _commands.emit(NavigationCommand.StopLoading)
        }
    }

    fun toggleDesktopMode() {
        val activeId = _state.value.activeTabId
        val currentTab = _state.value.activeTab
        val newDesktopState = !currentTab.isDesktopMode

        _state.update { current ->
            val updated = current.tabs.map {
                if (it.id == activeId) it.copy(isDesktopMode = newDesktopState) else it
            }
            current.copy(tabs = updated, isMenuOpen = false)
        }

        webViewPool[activeId]?.apply {
            settings.userAgentString = if (newDesktopState) DESKTOP_USER_AGENT else null
            reload()
        }
    }

    private fun onPageStartedForTab(tabId: String, url: String, favicon: Bitmap?) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) {
                    it.copy(
                        url = cleaned,
                        favicon = favicon ?: it.favicon,
                        isLoading = true,
                        loadingProgress = 10
                    )
                } else it
            }
            current.copy(tabs = updatedTabs, isOmniboxVisible = true)
        }
    }

    private fun onPageFinishedForTab(tabId: String, url: String) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) {
                    it.copy(
                        url = cleaned,
                        isLoading = false,
                        loadingProgress = 100
                    )
                } else it
            }
            current.copy(tabs = updatedTabs)
        }
        val tab = _state.value.tabs.find { it.id == tabId }
        if (tab != null) {
            viewModelScope.launch(Dispatchers.IO) {
                tabDatabase.updateTabMetadata(tabId, cleaned, tab.title)
            }
        }
    }

    private fun onProgressChangedForTab(tabId: String, progress: Int) {
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) {
                    it.copy(
                        loadingProgress = progress,
                        isLoading = progress in 1..99
                    )
                } else it
            }
            current.copy(tabs = updatedTabs)
        }
    }

    private fun onTitleChangedForTab(tabId: String, title: String) {
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) it.copy(title = title) else it
            }
            current.copy(tabs = updatedTabs)
        }
        val tab = _state.value.tabs.find { it.id == tabId }
        if (tab != null) {
            viewModelScope.launch(Dispatchers.IO) {
                tabDatabase.updateTabMetadata(tabId, tab.url, title)
            }
        }
    }

    private fun onFaviconChangedForTab(tabId: String, favicon: Bitmap?) {
        if (favicon == null) return
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) it.copy(favicon = favicon) else it
            }
            current.copy(tabs = updatedTabs)
        }
    }

    private fun updateHistoryForTab(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _state.update { current ->
            val updatedTabs = current.tabs.map {
                if (it.id == tabId) {
                    it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                } else it
            }
            current.copy(tabs = updatedTabs)
        }
    }

    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = callback
        _state.update { it.copy(customFullscreenView = view) }
    }

    fun onHideCustomView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        _state.update { it.copy(customFullscreenView = null) }
    }

    fun closeFullscreen() {
        onHideCustomView()
    }

    fun onPauseAll() {
        webViewPool.values.forEach {
            it.onPause()
            it.pauseTimers()
        }
    }

    fun onResumeAll() {
        webViewPool.values.forEach {
            it.onResume()
            it.resumeTimers()
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewPool.values.forEach {
            it.stopLoading()
            it.destroy()
        }
        webViewPool.clear()
    }
}
