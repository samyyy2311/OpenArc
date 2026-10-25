package `in`.caffeinelabs.archie.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast

class DownloadHandler(private val context: Context) {

    fun handleDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        try {
            val uri = Uri.parse(url)
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val cookie = CookieManager.getInstance().getCookie(url)

            val request = DownloadManager.Request(uri).apply {
                setTitle(filename)
                setDescription("Downloading $filename")
                setMimeType(mimetype)
                if (!cookie.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookie)
                }
                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager != null) {
                downloadManager.enqueue(request)
                Toast.makeText(context, "Downloading $filename", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
