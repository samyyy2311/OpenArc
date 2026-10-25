package `in`.caffeinelabs.archie.ui.viewmodel

import android.app.Application
import `in`.caffeinelabs.archie.data.model.HistoryEntity
import `in`.caffeinelabs.archie.data.model.TabEntity
import `in`.caffeinelabs.archie.data.repository.SettingsRepository
import `in`.caffeinelabs.archie.data.repository.TabRepository
import `in`.caffeinelabs.archie.ui.state.BrowserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTabRepository : TabRepository {
    val tabs = mutableMapOf<String, TabEntity>()
    val history = mutableListOf<HistoryEntity>()

    override suspend fun getActiveTabs(): List<TabEntity> = tabs.values.filter { !it.isArchived }

    override suspend fun saveTab(tab: TabEntity) {
        tabs[tab.id] = tab
    }

    override suspend fun updateMetadata(id: String, url: String, title: String) {
        val existing = tabs[id] ?: return
        tabs[id] = existing.copy(url = url, title = title)
    }

    override suspend fun setPinned(id: String, isPinned: Boolean) {
        val existing = tabs[id] ?: return
        tabs[id] = existing.copy(isPinned = isPinned)
    }

    override suspend fun updatePreferences(
        id: String,
        isShieldEnabled: Boolean,
        isDesktopMode: Boolean,
        isForceDark: Boolean
    ) {
        val existing = tabs[id] ?: return
        tabs[id] = existing.copy(
            isShieldEnabled = isShieldEnabled,
            isDesktopMode = isDesktopMode,
            isForceDark = isForceDark
        )
    }

    override suspend fun deleteTab(id: String) {
        tabs.remove(id)
    }

    override suspend fun archiveNonPinnedTabs() {
        tabs.values.forEach { tab ->
            if (!tab.isPinned) {
                tabs[tab.id] = tab.copy(isArchived = true)
            }
        }
    }

    override suspend fun recordHistoryVisit(url: String, title: String) {
        history.add(HistoryEntity(url, title))
    }

    override suspend fun searchHistory(query: String): List<HistoryEntity> {
        return history.filter { it.url.contains(query) || it.title.contains(query) }
    }

    override suspend fun getRecentHistory(limit: Int): List<HistoryEntity> {
        return history.sortedByDescending { it.lastVisitedTimestamp }.take(limit)
    }

    override suspend fun deleteHistoryEntry(url: String) {
        history.removeAll { it.url == url }
    }

    override suspend fun clearAllHistory() {
        history.clear()
    }
}

class FakeSettingsRepository : SettingsRepository {
    override var isGlobalShieldEnabled: Boolean = true
    override var isForceDarkEnabled: Boolean = false
    override var defaultSearchEngine: String = "Google"
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeTabRepository()
    private val fakeSettings = FakeSettingsRepository()
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo.tabs["tab-1"] = TabEntity(
            id = "tab-1",
            url = "https://en.wikipedia.org",
            title = "Wikipedia",
            isPinned = false
        )
        fakeRepo.tabs["tab-2"] = TabEntity(
            id = "tab-2",
            url = "https://github.com",
            title = "GitHub",
            isPinned = true
        )
        viewModel = BrowserViewModel(Application(), fakeRepo, fakeSettings, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoreTabs_loadsPersistedTabs() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.tabs.size)
        assertTrue(state.tabs.any { it.id == "tab-2" && it.isPinned })
    }

    @Test
    fun onAction_openNewTab_addsTabAndPersists() = runTest {
        advanceUntilIdle()
        viewModel.onAction(BrowserAction.OpenNewTab("https://news.ycombinator.com", openSearchImmediately = false))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.tabs.size)
        assertEquals("https://news.ycombinator.com", state.activeTab.url)
        assertTrue(fakeRepo.tabs.containsKey(state.activeTabId))
    }

    @Test
    fun onAction_closeTab_removesTabAndSelectsAdjacent() = runTest {
        advanceUntilIdle()
        viewModel.onAction(BrowserAction.CloseTab("tab-1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tabs.size)
        assertEquals("tab-2", state.activeTabId)
        assertFalse(fakeRepo.tabs.containsKey("tab-1"))
    }

    @Test
    fun onAction_togglePinTab_updatesStateAndRepository() = runTest {
        advanceUntilIdle()
        viewModel.onAction(BrowserAction.TogglePinTab("tab-1"))
        advanceUntilIdle()

        val tab = viewModel.uiState.value.tabs.find { it.id == "tab-1" }
        assertTrue(tab?.isPinned == true)
        assertTrue(fakeRepo.tabs["tab-1"]?.isPinned == true)
    }

    @Test
    fun onAction_archiveNonPinnedTabs_preservesPinnedTabsOnly() = runTest {
        advanceUntilIdle()
        viewModel.onAction(BrowserAction.ArchiveNonPinnedTabs)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tabs.size)
        assertEquals("tab-2", state.tabs.first().id)
        assertTrue(state.tabs.first().isPinned)
    }

    @Test
    fun onAction_toggleShield_flipsShieldStatus() = runTest {
        advanceUntilIdle()
        val initial = viewModel.uiState.value.activeTab.isShieldEnabled
        viewModel.onAction(BrowserAction.ToggleShield(viewModel.uiState.value.activeTabId))
        advanceUntilIdle()

        assertEquals(!initial, viewModel.uiState.value.activeTab.isShieldEnabled)
    }

    @Test
    fun onAction_setFindInPageOpen_updatesState() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFindInPageOpen)

        viewModel.onAction(BrowserAction.SetFindInPageOpen(true))
        assertTrue(viewModel.uiState.value.isFindInPageOpen)

        viewModel.onAction(BrowserAction.SetFindInPageOpen(false))
        assertFalse(viewModel.uiState.value.isFindInPageOpen)
    }

    @Test
    fun onAction_setFindQuery_and_onEngineFindResult_updatesMatchCount() = runTest {
        advanceUntilIdle()
        viewModel.onAction(BrowserAction.SetFindInPageOpen(true))
        viewModel.onAction(BrowserAction.SetFindQuery("kotlin"))
        assertEquals("kotlin", viewModel.uiState.value.findQuery)

        viewModel.onEngineFindResult(viewModel.uiState.value.activeTabId, activeMatchOrdinal = 2, numberOfMatches = 5)
        assertEquals(2, viewModel.uiState.value.findCurrentMatch)
        assertEquals(5, viewModel.uiState.value.findTotalMatches)
    }

    @Test
    fun onAction_setHistoryOpen_loadsHistoryAndClearsData() = runTest {
        advanceUntilIdle()
        fakeRepo.recordHistoryVisit("https://kotlinlang.org", "Kotlin")
        fakeRepo.recordHistoryVisit("https://android.com", "Android")

        viewModel.onAction(BrowserAction.SetHistoryOpen(true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isHistoryOpen)
        assertEquals(2, viewModel.uiState.value.historyItems.size)

        viewModel.onAction(BrowserAction.DeleteHistoryItem("https://kotlinlang.org"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.historyItems.size)
        assertEquals("https://android.com", viewModel.uiState.value.historyItems.first().url)

        viewModel.onAction(BrowserAction.ClearBrowsingData(clearHistory = true, clearCache = true, clearCookies = false))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.historyItems.isEmpty())
        assertTrue(fakeRepo.history.isEmpty())
    }

    @Test
    fun onAction_setSearchEngine_updatesSettingsAndState() = runTest {
        advanceUntilIdle()
        assertEquals("Google", viewModel.uiState.value.selectedSearchEngine)

        viewModel.onAction(BrowserAction.SetSearchEngine("DuckDuckGo"))
        assertEquals("DuckDuckGo", viewModel.uiState.value.selectedSearchEngine)
        assertEquals("DuckDuckGo", fakeSettings.defaultSearchEngine)
    }

    @Test
    fun onAction_setGlobalShield_updatesSettingsAndState() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGlobalShieldEnabled)

        viewModel.onAction(BrowserAction.SetGlobalShield(false))
        assertFalse(viewModel.uiState.value.isGlobalShieldEnabled)
        assertFalse(fakeSettings.isGlobalShieldEnabled)
    }
}
