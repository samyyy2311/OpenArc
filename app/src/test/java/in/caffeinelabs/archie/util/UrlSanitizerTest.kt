package `in`.caffeinelabs.archie.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlSanitizerTest {

    @Test
    fun cleanUrl_stripsSingleTrackingParameter() {
        val input = "https://example.com/article?utm_source=twitter"
        val expected = "https://example.com/article"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_stripsMultipleTrackingParameters() {
        val input = "https://example.com/page?utm_source=google&utm_medium=cpc&fbclid=IwAR123&gclid=XYZ"
        val expected = "https://example.com/page"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_preservesLegitimateQueryParameters() {
        val input = "https://example.com/search?q=kotlin&page=2&category=dev&sort=recent"
        val expected = "https://example.com/search?q=kotlin&page=2&category=dev&sort=recent"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_removesOnlyTrackingParametersFromMixedQuery() {
        val input = "https://example.com/item?id=42&utm_source=newsletter&color=blue&fbclid=987&size=M"
        val expected = "https://example.com/item?id=42&color=blue&size=M"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_doesNotRemoveWhenTrackingNameIsValueNotKey() {
        val input = "https://example.com/search?q=utm_source&ref=utm_campaign"
        val expected = "https://example.com/search?q=utm_source&ref=utm_campaign"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_preservesFlagParametersWithoutValues() {
        val input = "https://example.com/dashboard?dark&utm_medium=email&compact"
        val expected = "https://example.com/dashboard?dark&compact"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_isCaseInsensitiveForTrackingKeys() {
        val input = "https://example.com/store?UTM_SOURCE=promo&FbClid=abc&id=10"
        val expected = "https://example.com/store?id=10"
        assertEquals(expected, UrlSanitizer.cleanUrl(input))
    }

    @Test
    fun cleanUrl_preservesUrlFragments() {
        val withTracking = "https://example.com/docs?utm_source=reddit#section-2"
        val expectedWithTracking = "https://example.com/docs#section-2"
        assertEquals(expectedWithTracking, UrlSanitizer.cleanUrl(withTracking))

        val mixed = "https://example.com/docs?topic=intro&utm_campaign=launch#getting-started"
        val expectedMixed = "https://example.com/docs?topic=intro#getting-started"
        assertEquals(expectedMixed, UrlSanitizer.cleanUrl(mixed))
    }

    @Test
    fun cleanUrl_ignoresNonHttpSchemes() {
        val mailto = "mailto:support@example.com?subject=Help&utm_source=crm"
        assertEquals(mailto, UrlSanitizer.cleanUrl(mailto))

        val js = "javascript:alert(1)"
        assertEquals(js, UrlSanitizer.cleanUrl(js))

        val about = "about:blank"
        assertEquals(about, UrlSanitizer.cleanUrl(about))
    }

    @Test
    fun cleanUrl_returnsUnmodifiedWhenNoQuery() {
        val url = "https://example.com/path/to/page#hash"
        assertEquals(url, UrlSanitizer.cleanUrl(url))
    }

    @Test
    fun cleanUrl_handlesAllCleanLinksTrackingParameters() {
        val trackingKeys = listOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
            "fbclid", "gclid", "gclsrc", "dclid", "msclkid", "mc_cid", "mc_eid",
            "_hsenc", "_hsmi", "mkt_tok", "yclid", "_openstat", "igshid", "si", "spref",
            "vero_id", "vero_conv"
        )

        for (key in trackingKeys) {
            val url = "https://example.com/test?$key=12345"
            assertEquals("Failed for key: $key", "https://example.com/test", UrlSanitizer.cleanUrl(url))
        }
    }

    @Test
    fun resolveQuery_handlesSmartAliases() {
        assertEquals("https://calendar.google.com/calendar/r/eventedit", UrlSanitizer.resolveQuery("cal.new"))
        assertEquals("https://docs.google.com/document/create", UrlSanitizer.resolveQuery("doc.new"))
        assertEquals("https://docs.google.com/spreadsheets/create", UrlSanitizer.resolveQuery("sheet.new"))
        assertEquals("https://docs.google.com/presentation/create", UrlSanitizer.resolveQuery("slide.new"))
        assertEquals("https://docs.google.com/forms/create", UrlSanitizer.resolveQuery("form.new"))
        assertEquals("https://notion.new", UrlSanitizer.resolveQuery("notion.new"))
        assertEquals("https://github.com/new", UrlSanitizer.resolveQuery("repo.new"))
        assertEquals("https://gist.github.com", UrlSanitizer.resolveQuery("gist.new"))
        assertEquals("https://meet.google.com/new", UrlSanitizer.resolveQuery("meet.new"))
    }

    @Test
    fun resolveQuery_handlesSmartAliasesWithWhitespaceAndCase() {
        assertEquals("https://docs.google.com/document/create", UrlSanitizer.resolveQuery("   DOC.NEW  "))
        assertEquals("https://github.com/new", UrlSanitizer.resolveQuery("  RePo.NeW "))
    }

    @Test
    fun resolveQuery_preservesDirectHttpAndHttpsUrls() {
        val https = "https://en.wikipedia.org/wiki/Kotlin"
        assertEquals(https, UrlSanitizer.resolveQuery(https))

        val http = "http://localhost:3000/dashboard"
        assertEquals(http, UrlSanitizer.resolveQuery(http))
    }

    @Test
    fun resolveQuery_resolvesDomainPatternsWithoutScheme() {
        assertEquals("https://wikipedia.org", UrlSanitizer.resolveQuery("wikipedia.org"))
        assertEquals("https://sub.domain.co.uk/path", UrlSanitizer.resolveQuery("sub.domain.co.uk/path"))
        assertEquals("https://github.com/trending", UrlSanitizer.resolveQuery("github.com/trending"))
    }

    @Test
    fun resolveQuery_convertsFreeformTextToSearch() {
        assertEquals("https://www.google.com/search?q=kotlin+coroutines", UrlSanitizer.resolveQuery("kotlin coroutines"))
        assertEquals("https://www.google.com/search?q=what+is+android%3F", UrlSanitizer.resolveQuery("what is android?"))
        assertEquals("https://www.google.com/search?q=singleword", UrlSanitizer.resolveQuery("singleword"))
    }

    @Test
    fun resolveQuery_withDifferentSearchEngines() {
        assertEquals("https://duckduckgo.com/?q=kotlin", UrlSanitizer.resolveQuery("kotlin", searchEngine = "DuckDuckGo"))
        assertEquals("https://search.brave.com/search?q=kotlin", UrlSanitizer.resolveQuery("kotlin", searchEngine = "Brave"))
        assertEquals("https://www.bing.com/search?q=kotlin", UrlSanitizer.resolveQuery("kotlin", searchEngine = "Bing"))
        assertEquals("https://www.ecosia.org/search?q=kotlin", UrlSanitizer.resolveQuery("kotlin", searchEngine = "Ecosia"))
    }

    @Test
    fun resolveQuery_emptyReturnsDefaultHomepage() {
        assertEquals("https://en.wikipedia.org", UrlSanitizer.resolveQuery(""))
        assertEquals("https://en.wikipedia.org", UrlSanitizer.resolveQuery("   "))
    }

    @Test
    fun resolveQuery_withBrowseForMe_convertsToPerplexitySearch() {
        assertEquals("https://www.perplexity.ai/search?q=quantum+computing", UrlSanitizer.resolveQuery("quantum computing", browseForMe = true))
        assertEquals("https://github.com", UrlSanitizer.resolveQuery("https://github.com", browseForMe = true))
    }

    @Test
    fun extractDisplayDomain_extractsCleanDomain() {
        assertEquals("en.wikipedia.org", UrlSanitizer.extractDisplayDomain("https://en.wikipedia.org/wiki/Kotlin"))
        assertEquals("google.com", UrlSanitizer.extractDisplayDomain("https://www.google.com/search?q=test"))
        assertEquals("github.com", UrlSanitizer.extractDisplayDomain("https://github.com/caffeinelabs/archie"))
        assertEquals("localhost", UrlSanitizer.extractDisplayDomain("http://localhost:8080/api"))
    }

    @Test
    fun extractDisplayDomain_handlesEdgeCases() {
        assertEquals("", UrlSanitizer.extractDisplayDomain(""))
        assertEquals("invalid url with spaces", UrlSanitizer.extractDisplayDomain("invalid url with spaces"))
    }
}
