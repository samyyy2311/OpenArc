package `in`.caffeinelabs.archie.data.repository

import android.content.Context
import `in`.caffeinelabs.archie.data.local.TabDatabaseHelper
import `in`.caffeinelabs.archie.data.model.HistoryEntity
import `in`.caffeinelabs.archie.data.model.TabEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TabRepository {
    suspend fun getActiveTabs(): List<TabEntity>
    suspend fun saveTab(tab: TabEntity)
    suspend fun updateMetadata(id: String, url: String, title: String)
    suspend fun setPinned(id: String, isPinned: Boolean)
    suspend fun updatePreferences(id: String, isShieldEnabled: Boolean, isDesktopMode: Boolean, isForceDark: Boolean)
    suspend fun deleteTab(id: String)
    suspend fun archiveNonPinnedTabs()
    suspend fun recordHistoryVisit(url: String, title: String)
    suspend fun searchHistory(query: String): List<HistoryEntity>
}

class DefaultTabRepository(context: Context) : TabRepository {

    private val dbHelper = TabDatabaseHelper(context.applicationContext)

    override suspend fun getActiveTabs(): List<TabEntity> = withContext(Dispatchers.IO) {
        dbHelper.getAllActiveTabs()
    }

    override suspend fun saveTab(tab: TabEntity) = withContext(Dispatchers.IO) {
        dbHelper.upsertTab(tab)
    }

    override suspend fun updateMetadata(id: String, url: String, title: String) = withContext(Dispatchers.IO) {
        dbHelper.updateTabMetadata(id, url, title)
    }

    override suspend fun setPinned(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dbHelper.setTabPinned(id, isPinned)
    }

    override suspend fun updatePreferences(
        id: String,
        isShieldEnabled: Boolean,
        isDesktopMode: Boolean,
        isForceDark: Boolean
    ) = withContext(Dispatchers.IO) {
        dbHelper.updateTabPreferences(id, isShieldEnabled, isDesktopMode, isForceDark)
    }

    override suspend fun deleteTab(id: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteTab(id)
    }

    override suspend fun archiveNonPinnedTabs() = withContext(Dispatchers.IO) {
        dbHelper.archiveNonPinnedTabs()
    }

    override suspend fun recordHistoryVisit(url: String, title: String) = withContext(Dispatchers.IO) {
        dbHelper.recordHistoryVisit(url, title)
    }

    override suspend fun searchHistory(query: String): List<HistoryEntity> = withContext(Dispatchers.IO) {
        dbHelper.searchHistory(query)
    }
}
