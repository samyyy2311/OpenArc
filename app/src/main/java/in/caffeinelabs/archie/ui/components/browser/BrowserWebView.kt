package `in`.caffeinelabs.archie.ui.components.browser

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

@Composable
fun BrowserWebView(
    activeTab: UiTab,
    engineManager: WebEngineManager,
    eventListener: TabEventListener,
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
            modifier = modifier
        )
    }
}
