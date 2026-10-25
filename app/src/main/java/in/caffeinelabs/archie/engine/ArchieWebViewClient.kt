package `in`.caffeinelabs.archie.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
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
    fun onPageError(errorCode: Int, description: String, failingUrl: String)
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

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        if (request?.isForMainFrame == true) {
            val failingUrl = request.url.toString()
            val description = error?.description?.toString() ?: "Unable to connect"
            val errorCode = error?.errorCode ?: -1
            listener.onPageError(errorCode, description, failingUrl)

            val errorHtml = generateErrorHtml(failingUrl, description)
            view?.loadDataWithBaseURL(failingUrl, errorHtml, "text/html", "UTF-8", null)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        url?.let { listener.onPageStarted(it, favicon) }
        view?.let { listener.onHistoryChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        url?.let { listener.onPageFinished(it) }
        view?.let {
            listener.onHistoryChanged(it.canGoBack(), it.canGoForward())
            it.evaluateJavascript(
                """
                (function() {
                    try {
                        if (document.body) {
                            var current = parseInt(window.getComputedStyle(document.body).paddingBottom || '0', 10);
                            if (current < 80) {
                                document.body.style.paddingBottom = (current + 80) + 'px';
                            }
                        }
                    } catch (_) {}
                })();
                """.trimIndent(),
                null
            )
        }
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        view?.let { listener.onHistoryChanged(it.canGoBack(), it.canGoForward()) }
    }

    private fun generateErrorHtml(url: String, description: String): String {
        val domain = UrlSanitizer.extractDisplayDomain(url).ifBlank { "Page" }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body {
                        background-color: #131418;
                        color: #F0F1F5;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        margin: 0;
                        padding: 32px 24px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 80vh;
                        box-sizing: border-box;
                        text-align: center;
                    }
                    .icon {
                        width: 48px;
                        height: 48px;
                        margin-bottom: 20px;
                        stroke: #8E909A;
                    }
                    h1 {
                        font-size: 20px;
                        font-weight: 600;
                        margin: 0 0 10px 0;
                    }
                    p {
                        color: #8E909A;
                        font-size: 14px;
                        line-height: 1.5;
                        margin: 0 0 28px 0;
                        max-width: 320px;
                    }
                    .btn {
                        background-color: #007AFF;
                        color: #FFFFFF;
                        border: none;
                        border-radius: 20px;
                        padding: 12px 28px;
                        font-size: 14px;
                        font-weight: 600;
                        cursor: pointer;
                        text-decoration: none;
                    }
                </style>
            </head>
            <body>
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <h1>Unable to load $domain</h1>
                <p>$description. Check your internet connection and try again.</p>
                <button class="btn" onclick="location.reload()">Try Again</button>
            </body>
            </html>
        """.trimIndent()
    }
}
