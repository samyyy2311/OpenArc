package `in`.caffeinelabs.archie.domain.shield

import java.net.URI

class ShieldEngine {

    companion object {
        private val ADS_HOSTS = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "ads.facebook.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "adnxs.com",
            "rubiconproject.com",
            "advertising.com",
            "pubmatic.com",
            "openx.net",
            "smartadserver.com",
            "bidswitch.net",
            "amazon-adsystem.com",
            "adroll.com",
            "adsymptotic.com",
            "serving-sys.com",
            "adcolony.com",
            "applovin.com"
        )

        private val TRACKERS_HOSTS = setOf(
            "google-analytics.com",
            "analytics.google.com",
            "connect.facebook.net",
            "pixel.facebook.com",
            "analytics.facebook.com",
            "scorecardresearch.com",
            "quantserve.com",
            "moatads.com",
            "hotjar.com",
            "clarity.ms",
            "segment.io",
            "segment.com",
            "mixpanel.com",
            "amplitude.com",
            "appsflyer.com",
            "adjust.com",
            "branch.io",
            "chartbeat.com",
            "chartbeat.net",
            "omtrdc.net",
            "demdex.net",
            "everesttech.net"
        )
    }

    fun evaluate(url: String): BlockResult {
        val host = extractHost(url) ?: return BlockResult(isBlocked = false)
        val normalized = host.lowercase().trim().trimEnd('.')

        if (matchesHost(normalized, ADS_HOSTS)) {
            return BlockResult(isBlocked = true, category = BlockedCategory.ADVERTISING, matchedRule = normalized)
        }

        if (matchesHost(normalized, TRACKERS_HOSTS)) {
            return BlockResult(isBlocked = true, category = BlockedCategory.TRACKER, matchedRule = normalized)
        }

        return BlockResult(isBlocked = false)
    }

    private fun matchesHost(host: String, blocklist: Set<String>): Boolean {
        if (blocklist.contains(host)) return true
        for (blocked in blocklist) {
            if (host.endsWith(".$blocked")) {
                return true
            }
        }
        return false
    }

    private fun extractHost(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        return try {
            val uri = URI(trimmed)
            uri.host
        } catch (_: Exception) {
            val withoutScheme = when {
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
                else -> trimmed
            }
            val slashIndex = withoutScheme.indexOfAny(charArrayOf('/', '?', '#'))
            if (slashIndex != -1) withoutScheme.substring(0, slashIndex) else withoutScheme
        }
    }
}
