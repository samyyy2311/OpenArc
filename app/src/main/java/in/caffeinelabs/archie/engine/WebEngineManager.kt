package `in`.caffeinelabs.archie.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import `in`.caffeinelabs.archie.domain.shield.ShieldEngine

interface TabEventListener {
    val navigationListener: WebViewNavigationListener
    val chromeListener: WebChromeEventListener
    fun onScrollDelta(deltaY: Int)
}

class WebEngineManager(
    private val context: Context,
    private val shieldEngine: ShieldEngine = ShieldEngine(),
    val snapshotManager: SnapshotManager = SnapshotManager(context),
    private val downloadHandler: DownloadHandler = DownloadHandler(context)
) {
    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private val webViewPool = mutableMapOf<String, WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        tabId: String,
        initialUrl: String,
        isDesktopMode: Boolean,
        isForceDark: Boolean,
        isShieldEnabledProvider: () -> Boolean,
        eventListener: TabEventListener
    ): WebView {
        val existing = webViewPool[tabId]
        if (existing != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

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

                if (isDesktopMode) {
                    userAgentString = DESKTOP_USER_AGENT
                }

                if (isForceDark && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                downloadHandler.handleDownload(url, userAgent, contentDisposition, mimetype, contentLength)
            }

            webViewClient = ArchieWebViewClient(
                context = context,
                shieldEngine = shieldEngine,
                isShieldEnabledProvider = isShieldEnabledProvider,
                listener = eventListener.navigationListener
            )

            webChromeClient = ArchieWebChromeClient(
                listener = eventListener.chromeListener
            )

            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                eventListener.onScrollDelta(scrollY - oldScrollY)
            }
        }

        webViewPool[tabId] = webView
        if (initialUrl.isNotBlank()) {
            webView.loadUrl(initialUrl)
        }
        return webView
    }

    fun loadUrl(tabId: String, url: String) {
        webViewPool[tabId]?.loadUrl(url)
    }

    fun goBack(tabId: String): Boolean {
        val webView = webViewPool[tabId]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    fun goForward(tabId: String): Boolean {
        val webView = webViewPool[tabId]
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
            return true
        }
        return false
    }

    fun reload(tabId: String) {
        webViewPool[tabId]?.reload()
    }

    fun stopLoading(tabId: String) {
        webViewPool[tabId]?.stopLoading()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val webView = webViewPool[tabId] ?: return
        webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
        webView.reload()
    }

    fun setForceDark(tabId: String, enabled: Boolean) {
        val webView = webViewPool[tabId] ?: return
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (enabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    fun captureSnapshot(tabId: String): Bitmap? {
        val webView = webViewPool[tabId] ?: return snapshotManager.getSnapshot(tabId)
        if (webView.width <= 0 || webView.height <= 0) {
            return snapshotManager.getSnapshot(tabId)
        }

        return try {
            val scale = 0.5f
            val targetWidth = (webView.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (webView.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            webView.draw(canvas)
            snapshotManager.saveSnapshot(tabId, bitmap)
            bitmap
        } catch (_: Exception) {
            snapshotManager.getSnapshot(tabId)
        }
    }

    fun getCachedSnapshot(tabId: String): Bitmap? {
        return snapshotManager.getSnapshot(tabId)
    }

    fun findAll(tabId: String, query: String, onFindResult: (activeMatchOrdinal: Int, numberOfMatches: Int) -> Unit) {
        val webView = webViewPool[tabId] ?: return
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            onFindResult(activeMatchOrdinal, numberOfMatches)
        }
        webView.findAllAsync(query)
    }

    fun findNext(tabId: String, forward: Boolean) {
        webViewPool[tabId]?.findNext(forward)
    }

    fun clearMatches(tabId: String) {
        webViewPool[tabId]?.clearMatches()
    }

    fun destroyTab(tabId: String) {
        snapshotManager.removeSnapshot(tabId)
        webViewPool.remove(tabId)?.apply {
            stopLoading()
            destroy()
        }
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

    fun onTrimMemory(level: Int) {
        snapshotManager.trimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            webViewPool.values.forEach {
                it.freeMemory()
                it.clearCache(false)
            }
        }
    }

    fun destroyAll() {
        snapshotManager.clearAll()
        webViewPool.values.forEach {
            it.stopLoading()
            it.destroy()
        }
        webViewPool.clear()
    }
}
