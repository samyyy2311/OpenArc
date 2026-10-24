package `in`.caffeinelabs.archie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import `in`.caffeinelabs.archie.ui.components.BottomOmnibox
import `in`.caffeinelabs.archie.ui.components.BrowserWebView
import `in`.caffeinelabs.archie.ui.components.SearchModal
import `in`.caffeinelabs.archie.ui.components.TabSwitcher
import `in`.caffeinelabs.archie.ui.state.BrowserViewModel
import `in`.caffeinelabs.archie.ui.theme.ArchieTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent?.dataString?.let { incomingUrl ->
            if (incomingUrl.isNotBlank()) {
                viewModel.openNewTab(incomingUrl, openSearchImmediately = false)
            }
        }

        setContent {
            ArchieTheme {
                val state by viewModel.state.collectAsState()
                val insetsController = remember(this) {
                    WindowInsetsControllerCompat(window, window.decorView)
                }

                LaunchedEffect(state.customFullscreenView != null) {
                    if (state.customFullscreenView != null) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                BackHandler(
                    enabled = state.customFullscreenView != null ||
                            state.isSearchOpen ||
                            state.isTabSwitcherOpen ||
                            state.isMenuOpen ||
                            state.activeTab.canGoBack
                ) {
                    when {
                        state.customFullscreenView != null -> viewModel.closeFullscreen()
                        state.isSearchOpen -> viewModel.setSearchOpen(false)
                        state.isTabSwitcherOpen -> viewModel.setTabSwitcherOpen(false)
                        state.isMenuOpen -> viewModel.setMenuOpen(false)
                        state.activeTab.canGoBack -> viewModel.goBack()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BrowserWebView(
                            viewModel = viewModel,
                            activeTabId = state.activeTabId,
                            modifier = Modifier.fillMaxSize()
                        )

                        BottomOmnibox(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )

                        AnimatedVisibility(
                            visible = state.isTabSwitcherOpen,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
                        ) {
                            TabSwitcher(
                                state = state,
                                viewModel = viewModel
                            )
                        }

                        AnimatedVisibility(
                            visible = state.isSearchOpen,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            SearchModal(
                                initialQuery = state.activeTab.url,
                                viewModel = viewModel
                            )
                        }

                        val customView = state.customFullscreenView
                        if (customView != null) {
                            AndroidView(
                                factory = { customView },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { incomingUrl ->
            if (incomingUrl.isNotBlank()) {
                viewModel.openNewTab(incomingUrl, openSearchImmediately = false)
            }
        }
    }
}
