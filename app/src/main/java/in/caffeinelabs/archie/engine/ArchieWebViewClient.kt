package `in`.caffeinelabs.archie.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import `in`.caffeinelabs.archie.domain.shield.BlockedCategory
import `in`.caffeinelabs.archie.domain.shield.ShieldEngine
import `in`.caffeinelabs.archie.util.UrlSanitizer
import java.io.ByteArrayInputStream

interface WebViewNavigationListener {
    fun onPageStarted(url: String, favicon: Bitmap?)
    fun onPageFinished(url: String)
    fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean)
    fun onTrackerBlocked(url: String, category: BlockedCategory)
}

class ArchieWebViewClient(
    private val context: Context,
    private val shieldEngine: ShieldEngine,
    private val isShieldEnabledProvider: () -> Boolean,
    private val listener: WebViewNavigationListener
) : WebViewClient() {

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

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (!isShieldEnabledProvider()) {
            return super.shouldInterceptRequest(view, request)
        }

        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
        val evaluation = shieldEngine.evaluate(reqUrl)

        if (evaluation.isBlocked) {
            evaluation.category?.let { listener.onTrackerBlocked(reqUrl, it) }
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        url?.let { listener.onPageStarted(it, favicon) }
        view?.let { listener.onHistoryChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        url?.let { listener.onPageFinished(it) }
        view?.let { listener.onHistoryChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        view?.let { listener.onHistoryChanged(it.canGoBack(), it.canGoForward()) }
    }
}
