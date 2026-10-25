package `in`.caffeinelabs.archie.domain.shield

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldEngineTest {

    private val engine = ShieldEngine()

    @Test
    fun evaluate_blocksAdvertisingHosts() {
        val res1 = engine.evaluate("https://doubleclick.net/pagead/ads")
        assertTrue(res1.isBlocked)
        assertEquals(BlockedCategory.ADVERTISING, res1.category)

        val res2 = engine.evaluate("https://secure.adnxs.com/seg?add=1")
        assertTrue(res2.isBlocked)
        assertEquals(BlockedCategory.ADVERTISING, res2.category)
    }

    @Test
    fun evaluate_blocksAnalyticsAndTrackers() {
        val res1 = engine.evaluate("https://google-analytics.com/analytics.js")
        assertTrue(res1.isBlocked)
        assertEquals(BlockedCategory.TRACKER, res1.category)

        val res2 = engine.evaluate("https://connect.facebook.net/en_US/fbevents.js")
        assertTrue(res2.isBlocked)
        assertEquals(BlockedCategory.TRACKER, res2.category)
    }

    @Test
    fun evaluate_permitsLegitimateContent() {
        assertFalse(engine.evaluate("https://example.com/index.html").isBlocked)
        assertFalse(engine.evaluate("https://en.wikipedia.org/wiki/Browser").isBlocked)
        assertFalse(engine.evaluate("https://github.com/caffeinelabs/archie").isBlocked)
        assertFalse(engine.evaluate("https://news.ycombinator.com").isBlocked)
    }
}
