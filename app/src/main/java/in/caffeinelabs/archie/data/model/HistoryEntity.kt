package `in`.caffeinelabs.archie.data.model

data class HistoryEntity(
    val url: String,
    val title: String,
    val visitCount: Int = 1,
    val lastVisitedTimestamp: Long = System.currentTimeMillis()
)
