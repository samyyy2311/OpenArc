package `in`.caffeinelabs.archie.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.state.NavigationCommand
import `in`.caffeinelabs.archie.util.UrlSanitizer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val webView = remember {
        WebView(context).apply {
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
                    url?.let {
                        viewModel.onPageStarted(it)
                        viewModel.onFaviconChanged(favicon)
                    }
                    view?.let {
                        viewModel.onHistoryChanged(it.canGoBack(), it.canGoForward())
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { viewModel.onPageFinished(it) }
                    view?.let {
                        viewModel.onHistoryChanged(it.canGoBack(), it.canGoForward())
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    view?.let {
                        viewModel.onHistoryChanged(it.canGoBack(), it.canGoForward())
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    viewModel.onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    viewModel.onTitleChanged(title ?: "")
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    viewModel.onFaviconChanged(icon)
                }

                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                    if (view != null && callback != null) {
                        viewModel.onShowCustomView(view, callback)
                    }
                }

                override fun onHideCustomView() {
                    viewModel.onHideCustomView()
                }
            }

            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val delta = scrollY - oldScrollY
                if (delta > 20) {
                    viewModel.setOmniboxVisible(false)
                } else if (delta < -20 || scrollY <= 16) {
                    viewModel.setOmniboxVisible(true)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.onPause()
                    webView.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView.onResume()
                    webView.resumeTimers()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (webView.url == null) {
            val initial = viewModel.state.value.currentUrl
            if (initial.isNotBlank()) {
                webView.loadUrl(initial)
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.stopLoading()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            when (command) {
                is NavigationCommand.LoadUrl -> webView.loadUrl(command.url)
                is NavigationCommand.GoBack -> if (webView.canGoBack()) webView.goBack()
                is NavigationCommand.GoForward -> if (webView.canGoForward()) webView.goForward()
                is NavigationCommand.Reload -> webView.reload()
                is NavigationCommand.StopLoading -> webView.stopLoading()
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
