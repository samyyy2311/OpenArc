package `in`.caffeinelabs.archie

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView

class ArchieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        WebView.setWebContentsDebuggingEnabled(isDebuggable)
    }
}
