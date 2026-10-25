package `in`.caffeinelabs.archie.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SnapshotManager(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher)
    private val cacheDir = File(context.cacheDir, "tab_snapshots").apply { mkdirs() }

    private val maxMemoryBytes = (Runtime.getRuntime().maxMemory() * 0.15).toInt().coerceAtLeast(4 * 1024 * 1024)
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getSnapshot(tabId: String): Bitmap? {
        synchronized(memoryCache) {
            val inMemory = memoryCache.get(tabId)
            if (inMemory != null && !inMemory.isRecycled) {
                return inMemory
            }
        }

        val file = File(cacheDir, "$tabId.png")
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(tabId, bitmap)
                    }
                    return bitmap
                }
            } catch (_: Exception) {
                file.delete()
            }
        }
        return null
    }

    fun saveSnapshot(tabId: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            memoryCache.put(tabId, bitmap)
        }

        scope.launch {
            try {
                val file = File(cacheDir, "$tabId.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                }
            } catch (_: Exception) {
                // Ignore transient disk write errors
            }
        }
    }

    fun removeSnapshot(tabId: String) {
        synchronized(memoryCache) {
            memoryCache.remove(tabId)
        }
        scope.launch {
            val file = File(cacheDir, "$tabId.png")
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun clearAll() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        scope.launch {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun trimMemory(level: Int) {
        synchronized(memoryCache) {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                memoryCache.evictAll()
            } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                memoryCache.trimToSize(maxMemoryBytes / 2)
            }
        }
    }
}
