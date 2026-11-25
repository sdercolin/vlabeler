package util

import com.sdercolin.vlabeler.util.Url
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the constants in [Url].
 */
class UrlTest {

    @Test
    fun testAllUrlsAreValidAbsoluteHttpsUris() {
        val urls = listOf(
            Url.HOME_PAGE,
            Url.PROJECT_GIT_HUB,
            Url.LATEST_RELEASE,
            Url.DISCORD_INVITATION,
            Url.GITHUB_API_ROOT,
            Url.TRACKING_DOCUMENT,
            Url.ENTRY_SELECTOR_SCRIPT_DOCUMENT,
        )
        for (url in urls) {
            val uri = URI(url)
            assertEquals(true, uri.isAbsolute, "Not absolute: $url")
            assertEquals("https", uri.scheme, "Not https: $url")
        }
    }
}
