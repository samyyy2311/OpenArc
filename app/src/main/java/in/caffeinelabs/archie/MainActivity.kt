package `in`.caffeinelabs.archie

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import `in`.caffeinelabs.archie.domain.shield.BlockedCategory
import `in`.caffeinelabs.archie.engine.TabEventListener
import `in`.caffeinelabs.archie.engine.WebChromeEventListener
import `in`.caffeinelabs.archie.engine.WebEngineManager
import `in`.caffeinelabs.archie.engine.WebViewNavigationListener
import `in`.caffeinelabs.archie.ui.components.browser.BrowserWebView
import `in`.caffeinelabs.archie.ui.components.find.FindInPageBar
import `in`.caffeinelabs.archie.ui.components.omnibox.BottomOmnibox
import `in`.caffeinelabs.archie.ui.components.search.SearchModal
import `in`.caffeinelabs.archie.ui.components.settings.SiteSettingsSheet
import `in`.caffeinelabs.archie.ui.components.switcher.TabSwitcher
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.theme.ArchieTheme
import `in`.caffeinelabs.archie.ui.viewmodel.BrowserViewModel
import `in`.caffeinelabs.archie.ui.viewmodel.NavigationEffect

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var webEngineManager: WebEngineManager

    private var customFullscreenView by mutableStateOf<View?>(null)
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        webEngineManager = WebEngineManager(this)

        intent?.dataString?.let { incomingUrl ->
            if (incomingUrl.isNotBlank()) {
                viewModel.onAction(BrowserAction.OpenNewTab(incomingUrl, openSearchImmediately = false))
            }
        }

        setContent {
            ArchieTheme {
                val state by viewModel.uiState.collectAsState()
                val insetsController = remember(this) {
                    WindowInsetsControllerCompat(window, window.decorView)
                }

                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is NavigationEffect.LoadUrl -> webEngineManager.loadUrl(effect.tabId, effect.url)
                            is NavigationEffect.GoBack -> webEngineManager.goBack(effect.tabId)
                            is NavigationEffect.GoForward -> webEngineManager.goForward(effect.tabId)
                            is NavigationEffect.Reload -> webEngineManager.reload(effect.tabId)
                            is NavigationEffect.StopLoading -> webEngineManager.stopLoading(effect.tabId)
                            is NavigationEffect.ApplyDesktopMode -> webEngineManager.setDesktopMode(effect.tabId, effect.enabled)
                            is NavigationEffect.ApplyForceDark -> webEngineManager.setForceDark(effect.tabId, effect.enabled)
                            is NavigationEffect.DestroyTab -> webEngineManager.destroyTab(effect.tabId)
                            is NavigationEffect.CaptureSnapshot -> {
                                val snapshot = webEngineManager.captureSnapshot(effect.tabId)
                                if (snapshot != null) {
                                    viewModel.onEngineSnapshotCaptured(effect.tabId, snapshot)
                                }
                            }
                            is NavigationEffect.FindAll -> {
                                webEngineManager.findAll(effect.tabId, effect.query) { activeMatch, totalMatches ->
                                    viewModel.onEngineFindResult(effect.tabId, activeMatch, totalMatches)
                                }
                            }
                            is NavigationEffect.FindNext -> {
                                webEngineManager.findNext(effect.tabId, effect.forward)
                            }
                            is NavigationEffect.ClearFindMatches -> {
                                webEngineManager.clearMatches(effect.tabId)
                            }
                        }
                    }
                }

                LaunchedEffect(customFullscreenView != null) {
                    if (customFullscreenView != null) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                BackHandler(
                    enabled = customFullscreenView != null ||
                            state.isFindInPageOpen ||
                            state.isSiteSettingsOpen ||
                            state.isSearchOpen ||
                            state.isTabSwitcherOpen ||
                            state.isMenuOpen ||
                            state.activeTab.canGoBack
                ) {
                    when {
                        customFullscreenView != null -> closeFullscreen()
                        state.isFindInPageOpen -> viewModel.onAction(BrowserAction.SetFindInPageOpen(false))
                        state.isSiteSettingsOpen -> viewModel.onAction(BrowserAction.SetSiteSettingsOpen(false))
                        state.isSearchOpen -> viewModel.onAction(BrowserAction.SetSearchOpen(false))
                        state.isTabSwitcherOpen -> viewModel.onAction(BrowserAction.SetTabSwitcherOpen(false))
                        state.isMenuOpen -> viewModel.onAction(BrowserAction.SetMenuOpen(false))
                        state.activeTab.canGoBack -> viewModel.onAction(BrowserAction.GoBack)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val activeTab = state.activeTab

                        val eventListener = remember(activeTab.id) {
                            object : TabEventListener {
                                override val navigationListener = object : WebViewNavigationListener {
                                    override fun onPageStarted(url: String, favicon: Bitmap?) {
                                        viewModel.onEnginePageStarted(activeTab.id, url, favicon)
                                    }

                                    override fun onPageFinished(url: String) {
                                        viewModel.onEnginePageFinished(activeTab.id, url)
                                    }

                                    override fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) {
                                        viewModel.onEngineHistoryChanged(activeTab.id, canGoBack, canGoForward)
                                    }

                                    override fun onTrackerBlocked(url: String, category: BlockedCategory) {
                                        viewModel.onEngineTrackerBlocked(activeTab.id, url, category)
                                    }

                                    override fun onPageError(errorCode: Int, description: String, failingUrl: String) {}
                                }

                                override val chromeListener = object : WebChromeEventListener {
                                    override fun onProgressChanged(progress: Int) {
                                        viewModel.onEngineProgress(activeTab.id, progress)
                                    }

                                    override fun onTitleReceived(title: String) {
                                        viewModel.onEngineTitle(activeTab.id, title)
                                    }

                                    override fun onFaviconReceived(icon: Bitmap) {
                                        viewModel.onEngineFavicon(activeTab.id, icon)
                                    }

                                    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                                        showCustomView(view, callback)
                                    }

                                    override fun onHideCustomView() {
                                        hideCustomView()
                                    }

                                    override fun onShowFileChooser(
                                        filePathCallback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: WebChromeClient.FileChooserParams?
                                    ): Boolean {
                                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                                        this@MainActivity.filePathCallback = filePathCallback
                                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                        }
                                        return try {
                                            fileChooserLauncher.launch(intent)
                                            true
                                        } catch (_: Exception) {
                                            this@MainActivity.filePathCallback?.onReceiveValue(null)
                                            this@MainActivity.filePathCallback = null
                                            false
                                        }
                                    }
                                }

                                override fun onScrollDelta(deltaY: Int) {
                                    viewModel.onOmniboxScrollDelta(deltaY)
                                }
                            }
                        }

                        BrowserWebView(
                            activeTab = activeTab,
                            engineManager = webEngineManager,
                            eventListener = eventListener,
                            modifier = Modifier.fillMaxSize()
                        )

                        BottomOmnibox(
                            state = state,
                            onAction = viewModel::onAction,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )

                        FindInPageBar(
                            state = state,
                            onAction = viewModel::onAction,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )

                        TabSwitcher(
                            state = state,
                            onAction = viewModel::onAction
                        )

                        AnimatedVisibility(
                            visible = state.isSearchOpen,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            SearchModal(
                                state = state,
                                onAction = viewModel::onAction
                            )
                        }

                        SiteSettingsSheet(
                            state = state,
                            onAction = viewModel::onAction
                        )

                        customFullscreenView?.let { fullView ->
                            AndroidView(
                                factory = { fullView },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = callback
        customFullscreenView = view
    }

    private fun hideCustomView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        customFullscreenView = null
    }

    private fun closeFullscreen() {
        hideCustomView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { incomingUrl ->
            if (incomingUrl.isNotBlank()) {
                viewModel.onAction(BrowserAction.OpenNewTab(incomingUrl, openSearchImmediately = false))
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::webEngineManager.isInitialized) {
            webEngineManager.onTrimMemory(level)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webEngineManager.destroyAll()
    }
}
