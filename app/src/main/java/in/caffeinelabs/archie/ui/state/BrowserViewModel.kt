package `in`.caffeinelabs.archie.ui.state

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.archie.util.UrlSanitizer
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

class BrowserViewModel : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    fun loadUrl(input: String) {
        val resolved = UrlSanitizer.resolveQuery(input)
        val cleaned = UrlSanitizer.cleanUrl(resolved)
        _state.update { it.copy(currentUrl = cleaned, isSearchOpen = false) }
        viewModelScope.launch {
            _commands.emit(NavigationCommand.LoadUrl(cleaned))
        }
    }

    fun goBack() {
        viewModelScope.launch {
            _commands.emit(NavigationCommand.GoBack)
        }
    }

    fun goForward() {
        viewModelScope.launch {
            _commands.emit(NavigationCommand.GoForward)
        }
    }

    fun reload() {
        viewModelScope.launch {
            _commands.emit(NavigationCommand.Reload)
        }
    }

    fun stopLoading() {
        viewModelScope.launch {
            _commands.emit(NavigationCommand.StopLoading)
        }
    }

    fun setSearchOpen(isOpen: Boolean) {
        _state.update { it.copy(isSearchOpen = isOpen) }
    }

    fun setOmniboxVisible(visible: Boolean) {
        if (_state.value.isOmniboxVisible != visible) {
            _state.update { it.copy(isOmniboxVisible = visible) }
        }
    }

    fun onPageStarted(url: String) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _state.update {
            it.copy(
                currentUrl = cleaned,
                isLoading = true,
                loadingProgress = 10,
                isOmniboxVisible = true
            )
        }
    }

    fun onPageFinished(url: String) {
        val cleaned = UrlSanitizer.cleanUrl(url)
        _state.update {
            it.copy(
                currentUrl = cleaned,
                isLoading = false,
                loadingProgress = 100
            )
        }
    }

    fun onProgressChanged(progress: Int) {
        _state.update {
            it.copy(
                loadingProgress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onFaviconChanged(favicon: Bitmap?) {
        _state.update { it.copy(favicon = favicon) }
    }

    fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _state.update {
            it.copy(
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
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
}
