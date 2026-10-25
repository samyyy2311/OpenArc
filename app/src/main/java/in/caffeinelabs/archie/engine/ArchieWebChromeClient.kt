package `in`.caffeinelabs.archie.engine

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView

interface WebChromeEventListener {
    fun onProgressChanged(progress: Int)
    fun onTitleReceived(title: String)
    fun onFaviconReceived(icon: Bitmap)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
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
}
