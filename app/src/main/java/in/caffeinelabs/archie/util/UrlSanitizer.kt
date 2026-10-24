package `in`.caffeinelabs.archie.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object UrlSanitizer {

    private val TRACKING_PARAMS = setOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "fbclid",
        "gclid",
        "gclsrc",
        "dclid",
        "msclkid",
        "mc_cid",
        "mc_eid",
        "_hsenc",
        "_hsmi",
        "mkt_tok",
        "yclid",
        "_openstat",
        "igshid",
        "si",
        "spref",
        "vero_id",
        "vero_conv"
    )

    private val SMART_ALIASES = mapOf(
        "cal.new" to "https://calendar.google.com/calendar/r/eventedit",
        "doc.new" to "https://docs.google.com/document/create",
        "sheet.new" to "https://docs.google.com/spreadsheets/create",
        "slide.new" to "https://docs.google.com/presentation/create",
        "form.new" to "https://docs.google.com/forms/create",
        "notion.new" to "https://notion.new",
        "repo.new" to "https://github.com/new",
        "gist.new" to "https://gist.github.com",
        "meet.new" to "https://meet.google.com/new"
    )

    fun cleanUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return rawUrl
        }

        val queryStartIndex = trimmed.indexOf('?')
        if (queryStartIndex == -1) {
            return rawUrl
        }

        val hashIndex = trimmed.indexOf('#', startIndex = queryStartIndex)
        val queryString = if (hashIndex != -1) {
            trimmed.substring(queryStartIndex + 1, hashIndex)
        } else {
            trimmed.substring(queryStartIndex + 1)
        }

        if (queryString.isEmpty()) {
            return rawUrl
        }

        val pairs = queryString.split('&')
        var hasTracking = false
        val preservedPairs = ArrayList<String>(pairs.size)

        for (pair in pairs) {
            if (pair.isEmpty()) continue
            val equalsIndex = pair.indexOf('=')
            val rawKey = if (equalsIndex != -1) pair.substring(0, equalsIndex) else pair
            val key = try {
                URLDecoder.decode(rawKey, "UTF-8").lowercase()
            } catch (_: Exception) {
                rawKey.lowercase()
            }

            if (TRACKING_PARAMS.contains(key)) {
                hasTracking = true
            } else {
                preservedPairs.add(pair)
            }
        }

        if (!hasTracking) {
            return rawUrl
        }

        val base = trimmed.substring(0, queryStartIndex)
        val fragment = if (hashIndex != -1) trimmed.substring(hashIndex) else ""

        return if (preservedPairs.isEmpty()) {
            base + fragment
        } else {
            base + "?" + preservedPairs.joinToString("&") + fragment
        }
    }

    fun resolveQuery(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return "https://en.wikipedia.org"

        val alias = SMART_ALIASES[trimmed.lowercase()]
        if (alias != null) return alias

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }

        val domainPattern = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:[0-9]+)?(/.*)?$")
        if (domainPattern.matches(trimmed) && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }

        val encoded = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (_: Exception) {
            trimmed
        }
        return "https://www.google.com/search?q=$encoded"
    }

    fun extractDisplayDomain(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""

        return try {
            val uri = URI(trimmed)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                if (host.startsWith("www.", ignoreCase = true)) {
                    host.substring(4)
                } else {
                    host
                }
            } else {
                val domainRegex = Regex("^(?:https?://)?(?:www\\.)?([^/:?#]+)", RegexOption.IGNORE_CASE)
                val match = domainRegex.find(trimmed)
                match?.groupValues?.get(1) ?: trimmed
            }
        } catch (_: Exception) {
            val domainRegex = Regex("^(?:https?://)?(?:www\\.)?([^/:?#]+)", RegexOption.IGNORE_CASE)
            val match = domainRegex.find(trimmed)
            match?.groupValues?.get(1) ?: trimmed
        }
    }
}
