package `in`.caffeinelabs.archie.ui.components.browser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.caffeinelabs.archie.engine.TabEventListener
import `in`.caffeinelabs.archie.engine.WebEngineManager
import `in`.caffeinelabs.archie.ui.state.UiTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserWebView(
    activeTab: UiTab,
    engineManager: WebEngineManager,
    eventListener: TabEventListener,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> engineManager.onPauseAll()
                Lifecycle.Event.ON_RESUME -> engineManager.onResumeAll()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PullToRefreshBox(
        isRefreshing = activeTab.isLoading,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        key(activeTab.id) {
            AndroidView(
                factory = {
                    engineManager.getOrCreateWebView(
                        tabId = activeTab.id,
                        initialUrl = activeTab.url,
                        isDesktopMode = activeTab.isDesktopMode,
                        isForceDark = activeTab.isForceDark,
                        isShieldEnabledProvider = { activeTab.isShieldEnabled },
                        eventListener = eventListener
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
