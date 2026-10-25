package `in`.caffeinelabs.archie.data.model

data class TabEntity(
    val id: String,
    val url: String,
    val title: String,
    val lastSelectedTimestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isShieldEnabled: Boolean = true,
    val isDesktopMode: Boolean = false,
    val isForceDark: Boolean = false
)
