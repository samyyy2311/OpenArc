package `in`.caffeinelabs.archie

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import `in`.caffeinelabs.archie.domain.shield.BlockedCategory
import `in`.caffeinelabs.archie.engine.TabEventListener
import `in`.caffeinelabs.archie.engine.WebChromeEventListener
import `in`.caffeinelabs.archie.engine.WebEngineManager
import `in`.caffeinelabs.archie.engine.WebPermissionPrompt
import `in`.caffeinelabs.archie.engine.WebViewNavigationListener
import `in`.caffeinelabs.archie.ui.components.browser.BrowserWebView
import `in`.caffeinelabs.archie.ui.components.find.FindInPageBar
import `in`.caffeinelabs.archie.ui.components.history.HistorySheet
import `in`.caffeinelabs.archie.ui.components.omnibox.BottomOmnibox
import `in`.caffeinelabs.archie.ui.components.search.SearchModal
import `in`.caffeinelabs.archie.ui.components.settings.SiteSettingsSheet
import `in`.caffeinelabs.archie.ui.components.switcher.TabSwitcher
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkSurface
import `in`.caffeinelabs.archie.ui.theme.ArchieTextMuted
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary
import `in`.caffeinelabs.archie.ui.theme.ArchieTheme
import `in`.caffeinelabs.archie.ui.viewmodel.BrowserViewModel
import `in`.caffeinelabs.archie.ui.viewmodel.NavigationEffect
import `in`.caffeinelabs.archie.util.UrlSanitizer

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

    private var pendingPermissionPrompt by mutableStateOf<WebPermissionPrompt?>(null)
    private var pendingAndroidPermissionCallback: ((Boolean) -> Unit)? = null
    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        pendingAndroidPermissionCallback?.invoke(allGranted)
        pendingAndroidPermissionCallback = null
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
                            is NavigationEffect.ClearBrowsingData -> {
                                webEngineManager.clearBrowsingData(effect.clearCache, effect.clearCookies)
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
                            pendingPermissionPrompt != null ||
                            state.isFindInPageOpen ||
                            state.isHistoryOpen ||
                            state.isSiteSettingsOpen ||
                            state.isSearchOpen ||
                            state.isTabSwitcherOpen ||
                            state.isMenuOpen ||
                            state.activeTab.canGoBack
                ) {
                    when {
                        customFullscreenView != null -> closeFullscreen()
                        pendingPermissionPrompt != null -> {
                            when (val prompt = pendingPermissionPrompt) {
                                is WebPermissionPrompt.Media -> prompt.request.deny()
                                is WebPermissionPrompt.Geolocation -> prompt.callback.invoke(prompt.origin, false, false)
                                null -> {}
                            }
                            pendingPermissionPrompt = null
                        }
                        state.isFindInPageOpen -> viewModel.onAction(BrowserAction.SetFindInPageOpen(false))
                        state.isHistoryOpen -> viewModel.onAction(BrowserAction.SetHistoryOpen(false))
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

                                    override fun onPermissionRequest(request: PermissionRequest) {
                                        val resources = request.resources
                                        val labels = mutableListOf<String>()
                                        val androidPerms = mutableListOf<String>()

                                        if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                            labels.add("Camera")
                                            androidPerms.add(android.Manifest.permission.CAMERA)
                                        }
                                        if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                            labels.add("Microphone")
                                            androidPerms.add(android.Manifest.permission.RECORD_AUDIO)
                                        }

                                        if (labels.isEmpty()) {
                                            request.deny()
                                            return
                                        }

                                        pendingPermissionPrompt = WebPermissionPrompt.Media(
                                            origin = request.origin.toString(),
                                            request = request,
                                            humanReadableLabels = labels,
                                            requiredAndroidPermissions = androidPerms
                                        )
                                    }

                                    override fun onGeolocationPermissionsShowPrompt(
                                        origin: String,
                                        callback: GeolocationPermissions.Callback
                                    ) {
                                        pendingPermissionPrompt = WebPermissionPrompt.Geolocation(
                                            origin = origin,
                                            callback = callback
                                        )
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
                            onRefresh = { viewModel.onAction(BrowserAction.Reload) },
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

                        HistorySheet(
                            state = state,
                            onAction = viewModel::onAction
                        )

                        pendingPermissionPrompt?.let { prompt ->
                            val displayDomain = UrlSanitizer.extractDisplayDomain(prompt.origin).ifBlank { prompt.origin }
                            val message = when (prompt) {
                                is WebPermissionPrompt.Media -> "$displayDomain wants to access your ${prompt.humanReadableLabels.joinToString(" and ")}."
                                is WebPermissionPrompt.Geolocation -> "$displayDomain wants to access your location."
                            }

                            AlertDialog(
                                onDismissRequest = {
                                    when (prompt) {
                                        is WebPermissionPrompt.Media -> prompt.request.deny()
                                        is WebPermissionPrompt.Geolocation -> prompt.callback.invoke(prompt.origin, false, false)
                                    }
                                    pendingPermissionPrompt = null
                                },
                                title = {
                                    Text(
                                        text = "Site Permission",
                                        color = ArchieTextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = message,
                                        color = ArchieTextPrimary,
                                        fontSize = 14.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            when (prompt) {
                                                is WebPermissionPrompt.Media -> {
                                                    val missing = prompt.requiredAndroidPermissions.filter {
                                                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                                                    }
                                                    if (missing.isNotEmpty()) {
                                                        pendingAndroidPermissionCallback = { granted ->
                                                            if (granted) {
                                                                prompt.request.grant(prompt.request.resources)
                                                            } else {
                                                                prompt.request.deny()
                                                            }
                                                        }
                                                        runtimePermissionLauncher.launch(missing.toTypedArray())
                                                    } else {
                                                        prompt.request.grant(prompt.request.resources)
                                                    }
                                                }
                                                is WebPermissionPrompt.Geolocation -> {
                                                    val fineGranted = ContextCompat.checkSelfPermission(
                                                        this@MainActivity,
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    val coarseGranted = ContextCompat.checkSelfPermission(
                                                        this@MainActivity,
                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                    if (!fineGranted && !coarseGranted) {
                                                        pendingAndroidPermissionCallback = { granted ->
                                                            prompt.callback.invoke(prompt.origin, granted, false)
                                                        }
                                                        runtimePermissionLauncher.launch(
                                                            arrayOf(
                                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                            )
                                                        )
                                                    } else {
                                                        prompt.callback.invoke(prompt.origin, true, false)
                                                    }
                                                }
                                            }
                                            pendingPermissionPrompt = null
                                        }
                                    ) {
                                        Text("Allow", color = ArchieBlue, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            when (prompt) {
                                                is WebPermissionPrompt.Media -> prompt.request.deny()
                                                is WebPermissionPrompt.Geolocation -> prompt.callback.invoke(prompt.origin, false, false)
                                            }
                                            pendingPermissionPrompt = null
                                        }
                                    ) {
                                        Text("Block", color = ArchieTextMuted)
                                    }
                                },
                                containerColor = ArchieDarkSurface,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }

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
        when (val prompt = pendingPermissionPrompt) {
            is WebPermissionPrompt.Media -> prompt.request.deny()
            is WebPermissionPrompt.Geolocation -> prompt.callback.invoke(prompt.origin, false, false)
            null -> {}
        }
        pendingPermissionPrompt = null
        webEngineManager.destroyAll()
    }
}
