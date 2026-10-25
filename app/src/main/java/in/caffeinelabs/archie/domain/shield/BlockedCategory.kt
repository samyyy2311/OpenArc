package `in`.caffeinelabs.archie.domain.shield

enum class BlockedCategory {
    ADVERTISING,
    ANALYTICS,
    TRACKER,
    TELEMETRY
}

data class BlockResult(
    val isBlocked: Boolean,
    val category: BlockedCategory? = null,
    val matchedRule: String? = null
)
