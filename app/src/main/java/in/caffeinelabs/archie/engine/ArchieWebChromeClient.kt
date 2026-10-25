package `in`.caffeinelabs.archie.engine

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

interface WebChromeEventListener {
    fun onProgressChanged(progress: Int)
    fun onTitleReceived(title: String)
    fun onFaviconReceived(icon: Bitmap)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean
    fun onPermissionRequest(request: PermissionRequest)
    fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)
}

class ArchieWebChromeClient(
    private val listener: WebChromeEventListener
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        listener.onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        listener.onTitleReceived(title ?: "")
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        if (icon != null) {
            listener.onFaviconReceived(icon)
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            listener.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        listener.onHideCustomView()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return listener.onShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request != null) {
            listener.onPermissionRequest(request)
        } else {
            super.onPermissionRequest(null)
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin != null && callback != null) {
            listener.onGeolocationPermissionsShowPrompt(origin, callback)
        } else {
            super.onGeolocationPermissionsShowPrompt(origin, callback)
        }
    }
}
