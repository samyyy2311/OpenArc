package `in`.caffeinelabs.archie.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import `in`.caffeinelabs.archie.data.model.HistoryEntity
import `in`.caffeinelabs.archie.data.model.TabEntity

class TabDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "archie_browser.db"
        const val DATABASE_VERSION = 3

        const val TABLE_TABS = "tabs"
        const val COL_TAB_ID = "id"
        const val COL_TAB_URL = "url"
        const val COL_TAB_TITLE = "title"
        const val COL_TAB_LAST_SELECTED = "lastSelectedTimestamp"
        const val COL_TAB_IS_PINNED = "isPinned"
        const val COL_TAB_IS_ARCHIVED = "isArchived"
        const val COL_TAB_IS_SHIELD_ENABLED = "isShieldEnabled"
        const val COL_TAB_IS_DESKTOP_MODE = "isDesktopMode"
        const val COL_TAB_IS_FORCE_DARK = "isForceDark"

        const val TABLE_HISTORY = "history"
        const val COL_HIST_URL = "url"
        const val COL_HIST_TITLE = "title"
        const val COL_HIST_VISIT_COUNT = "visitCount"
        const val COL_HIST_LAST_VISITED = "lastVisitedTimestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TABS (
                $COL_TAB_ID TEXT PRIMARY KEY,
                $COL_TAB_URL TEXT NOT NULL,
                $COL_TAB_TITLE TEXT NOT NULL,
                $COL_TAB_LAST_SELECTED INTEGER NOT NULL,
                $COL_TAB_IS_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_IS_ARCHIVED INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_IS_SHIELD_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COL_TAB_IS_DESKTOP_MODE INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_IS_FORCE_DARK INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_HIST_URL TEXT PRIMARY KEY,
                $COL_HIST_TITLE TEXT NOT NULL,
                $COL_HIST_VISIT_COUNT INTEGER NOT NULL DEFAULT 1,
                $COL_HIST_LAST_VISITED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        createIndices(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_TABS ADD COLUMN $COL_TAB_IS_SHIELD_ENABLED INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE $TABLE_TABS ADD COLUMN $COL_TAB_IS_DESKTOP_MODE INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_TABS ADD COLUMN $COL_TAB_IS_FORCE_DARK INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                    $COL_HIST_URL TEXT PRIMARY KEY,
                    $COL_HIST_TITLE TEXT NOT NULL,
                    $COL_HIST_VISIT_COUNT INTEGER NOT NULL DEFAULT 1,
                    $COL_HIST_LAST_VISITED INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            createIndices(db)
        }
    }

    private fun createIndices(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabs_lookup ON $TABLE_TABS ($COL_TAB_IS_ARCHIVED, $COL_TAB_IS_PINNED, $COL_TAB_LAST_SELECTED)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_lookup ON $TABLE_HISTORY ($COL_HIST_LAST_VISITED DESC)")
    }

    fun getAllActiveTabs(): List<TabEntity> {
        val list = mutableListOf<TabEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TABS,
            null,
            "$COL_TAB_IS_ARCHIVED = 0",
            null,
            null,
            null,
            "$COL_TAB_IS_PINNED DESC, $COL_TAB_LAST_SELECTED DESC"
        )

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_TAB_ID)
            val urlIdx = c.getColumnIndexOrThrow(COL_TAB_URL)
            val titleIdx = c.getColumnIndexOrThrow(COL_TAB_TITLE)
            val timeIdx = c.getColumnIndexOrThrow(COL_TAB_LAST_SELECTED)
            val pinIdx = c.getColumnIndexOrThrow(COL_TAB_IS_PINNED)
            val archIdx = c.getColumnIndexOrThrow(COL_TAB_IS_ARCHIVED)
            val shieldIdx = c.getColumnIndex(COL_TAB_IS_SHIELD_ENABLED)
            val deskIdx = c.getColumnIndex(COL_TAB_IS_DESKTOP_MODE)
            val darkIdx = c.getColumnIndex(COL_TAB_IS_FORCE_DARK)

            while (c.moveToNext()) {
                list.add(
                    TabEntity(
                        id = c.getString(idIdx),
                        url = c.getString(urlIdx),
                        title = c.getString(titleIdx),
                        lastSelectedTimestamp = c.getLong(timeIdx),
                        isPinned = c.getInt(pinIdx) == 1,
                        isArchived = c.getInt(archIdx) == 1,
                        isShieldEnabled = if (shieldIdx != -1) c.getInt(shieldIdx) == 1 else true,
                        isDesktopMode = if (deskIdx != -1) c.getInt(deskIdx) == 1 else false,
                        isForceDark = if (darkIdx != -1) c.getInt(darkIdx) == 1 else false
                    )
                )
            }
        }
        return list
    }

    fun upsertTab(tab: TabEntity) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_TAB_ID, tab.id)
                put(COL_TAB_URL, tab.url)
                put(COL_TAB_TITLE, tab.title)
                put(COL_TAB_LAST_SELECTED, tab.lastSelectedTimestamp)
                put(COL_TAB_IS_PINNED, if (tab.isPinned) 1 else 0)
                put(COL_TAB_IS_ARCHIVED, if (tab.isArchived) 1 else 0)
                put(COL_TAB_IS_SHIELD_ENABLED, if (tab.isShieldEnabled) 1 else 0)
                put(COL_TAB_IS_DESKTOP_MODE, if (tab.isDesktopMode) 1 else 0)
                put(COL_TAB_IS_FORCE_DARK, if (tab.isForceDark) 1 else 0)
            }
            db.insertWithOnConflict(TABLE_TABS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTabMetadata(id: String, url: String, title: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_TAB_URL, url)
                put(COL_TAB_TITLE, title)
                put(COL_TAB_LAST_SELECTED, System.currentTimeMillis())
            }
            db.update(TABLE_TABS, values, "$COL_TAB_ID = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setTabPinned(id: String, isPinned: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_TAB_IS_PINNED, if (isPinned) 1 else 0)
            }
            db.update(TABLE_TABS, values, "$COL_TAB_ID = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTabPreferences(id: String, isShieldEnabled: Boolean, isDesktopMode: Boolean, isForceDark: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_TAB_IS_SHIELD_ENABLED, if (isShieldEnabled) 1 else 0)
                put(COL_TAB_IS_DESKTOP_MODE, if (isDesktopMode) 1 else 0)
                put(COL_TAB_IS_FORCE_DARK, if (isForceDark) 1 else 0)
            }
            db.update(TABLE_TABS, values, "$COL_TAB_ID = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTab(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_TABS, "$COL_TAB_ID = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun archiveNonPinnedTabs() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_TAB_IS_ARCHIVED, 1)
            }
            db.update(TABLE_TABS, values, "$COL_TAB_IS_PINNED = 0", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun recordHistoryVisit(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """
                INSERT INTO $TABLE_HISTORY ($COL_HIST_URL, $COL_HIST_TITLE, $COL_HIST_VISIT_COUNT, $COL_HIST_LAST_VISITED)
                VALUES (?, ?, 1, ?)
                ON CONFLICT($COL_HIST_URL) DO UPDATE SET
                    $COL_HIST_TITLE = excluded.$COL_HIST_TITLE,
                    $COL_HIST_VISIT_COUNT = $COL_HIST_VISIT_COUNT + 1,
                    $COL_HIST_LAST_VISITED = excluded.$COL_HIST_LAST_VISITED
                """.trimIndent(),
                arrayOf(url, title, System.currentTimeMillis())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun searchHistory(query: String, limit: Int = 20): List<HistoryEntity> {
        val list = mutableListOf<HistoryEntity>()
        val db = readableDatabase
        val sanitizedQuery = "%${query.trim()}%"
        val cursor = db.query(
            TABLE_HISTORY,
            null,
            "$COL_HIST_URL LIKE ? OR $COL_HIST_TITLE LIKE ?",
            arrayOf(sanitizedQuery, sanitizedQuery),
            null,
            null,
            "$COL_HIST_LAST_VISITED DESC",
            limit.toString()
        )

        cursor.use { c ->
            val urlIdx = c.getColumnIndexOrThrow(COL_HIST_URL)
            val titleIdx = c.getColumnIndexOrThrow(COL_HIST_TITLE)
            val countIdx = c.getColumnIndexOrThrow(COL_HIST_VISIT_COUNT)
            val timeIdx = c.getColumnIndexOrThrow(COL_HIST_LAST_VISITED)

            while (c.moveToNext()) {
                list.add(
                    HistoryEntity(
                        url = c.getString(urlIdx),
                        title = c.getString(titleIdx),
                        visitCount = c.getInt(countIdx),
                        lastVisitedTimestamp = c.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    fun getRecentHistory(limit: Int = 100): List<HistoryEntity> {
        val list = mutableListOf<HistoryEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COL_HIST_LAST_VISITED DESC",
            limit.toString()
        )

        cursor.use { c ->
            val urlIdx = c.getColumnIndexOrThrow(COL_HIST_URL)
            val titleIdx = c.getColumnIndexOrThrow(COL_HIST_TITLE)
            val countIdx = c.getColumnIndexOrThrow(COL_HIST_VISIT_COUNT)
            val timeIdx = c.getColumnIndexOrThrow(COL_HIST_LAST_VISITED)

            while (c.moveToNext()) {
                list.add(
                    HistoryEntity(
                        url = c.getString(urlIdx),
                        title = c.getString(titleIdx),
                        visitCount = c.getInt(countIdx),
                        lastVisitedTimestamp = c.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteHistoryEntry(url: String) {
        val db = writableDatabase
        db.delete(TABLE_HISTORY, "$COL_HIST_URL = ?", arrayOf(url))
    }

    fun clearAllHistory() {
        val db = writableDatabase
        db.delete(TABLE_HISTORY, null, null)
    }
}
