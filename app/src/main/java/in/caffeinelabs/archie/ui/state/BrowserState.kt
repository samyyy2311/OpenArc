package `in`.caffeinelabs.archie.ui.state

import android.graphics.Bitmap
import android.view.View

data class BrowserState(
    val currentUrl: String = "https://en.wikipedia.org",
    val title: String = "",
    val favicon: Bitmap? = null,
    val loadingProgress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSearchOpen: Boolean = false,
    val customFullscreenView: View? = null,
    val isOmniboxVisible: Boolean = true
)
