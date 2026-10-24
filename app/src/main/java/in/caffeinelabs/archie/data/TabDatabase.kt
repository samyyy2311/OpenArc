package `in`.caffeinelabs.archie.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TabRecord(
    val id: String,
    val url: String,
    val title: String,
    val lastSelectedTimestamp: Long,
    val isPinned: Boolean,
    val isArchived: Boolean
)

class TabDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "archie_tabs.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TABS = "tabs"
        private const val COL_ID = "id"
        private const val COL_URL = "url"
        private const val COL_TITLE = "title"
        private const val COL_LAST_SELECTED = "lastSelectedTimestamp"
        private const val COL_IS_PINNED = "isPinned"
        private const val COL_IS_ARCHIVED = "isArchived"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TABS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_URL TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_LAST_SELECTED INTEGER NOT NULL,
                $COL_IS_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_IS_ARCHIVED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TABS")
        onCreate(db)
    }

    fun getAllActiveTabs(): List<TabRecord> {
        val records = mutableListOf<TabRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TABS,
            null,
            "$COL_IS_ARCHIVED = 0",
            null,
            null,
            null,
            "$COL_IS_PINNED DESC, $COL_LAST_SELECTED DESC"
        )

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_ID)
            val urlIdx = c.getColumnIndexOrThrow(COL_URL)
            val titleIdx = c.getColumnIndexOrThrow(COL_TITLE)
            val timeIdx = c.getColumnIndexOrThrow(COL_LAST_SELECTED)
            val pinIdx = c.getColumnIndexOrThrow(COL_IS_PINNED)
            val archIdx = c.getColumnIndexOrThrow(COL_IS_ARCHIVED)

            while (c.moveToNext()) {
                records.add(
                    TabRecord(
                        id = c.getString(idIdx),
                        url = c.getString(urlIdx),
                        title = c.getString(titleIdx),
                        lastSelectedTimestamp = c.getLong(timeIdx),
                        isPinned = c.getInt(pinIdx) == 1,
                        isArchived = c.getInt(archIdx) == 1
                    )
                )
            }
        }
        return records
    }

    fun saveTab(
        id: String,
        url: String,
        title: String,
        isPinned: Boolean = false,
        lastSelectedTimestamp: Long = System.currentTimeMillis()
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, id)
            put(COL_URL, url)
            put(COL_TITLE, title)
            put(COL_LAST_SELECTED, lastSelectedTimestamp)
            put(COL_IS_PINNED, if (isPinned) 1 else 0)
            put(COL_IS_ARCHIVED, 0)
        }
        db.insertWithOnConflict(TABLE_TABS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTabMetadata(id: String, url: String, title: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_URL, url)
            put(COL_TITLE, title)
            put(COL_LAST_SELECTED, System.currentTimeMillis())
        }
        db.update(TABLE_TABS, values, "$COL_ID = ?", arrayOf(id))
    }

    fun setPinned(id: String, isPinned: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_PINNED, if (isPinned) 1 else 0)
        }
        db.update(TABLE_TABS, values, "$COL_ID = ?", arrayOf(id))
    }

    fun deleteTab(id: String) {
        val db = writableDatabase
        db.delete(TABLE_TABS, "$COL_ID = ?", arrayOf(id))
    }

    fun archiveNonPinnedTabs() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_ARCHIVED, 1)
        }
        db.update(TABLE_TABS, values, "$COL_IS_PINNED = 0", null)
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_TABS, null, null)
    }
}
