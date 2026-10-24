package `in`.caffeinelabs.archie.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel

@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    activeTabId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onPauseAll()
                Lifecycle.Event.ON_RESUME -> viewModel.onResumeAll()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    key(activeTabId) {
        AndroidView(
            factory = {
                viewModel.getOrCreateWebView(activeTabId, context)
            },
            modifier = modifier
        )
    }
}
