package env

import com.sdercolin.vlabeler.env.Version
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTest {

    @Test
    fun testVersionComparison() {
        val versions = listOf(
            "1.0.0",
            "1.0.1-alpha1",
            "1.0.1-alpha2",
            "1.0.1-beta1",
            "1.0.1-beta2",
            "1.0.1",
            "1.1.0",
            "1.1.1",
            "1.2.0",
            "1.2.1",
        ).map { requireNotNull(Version.from(it)) }

        val sorted = versions.sortedBy { it }

        assertEquals(versions, sorted)
    }

    @Test
    fun testStages() {
        val versions = listOf(
            "1.0.0",
            "1.0.1-alpha1",
            "1.0.1-beta1",
        ).map { requireNotNull(Version.from(it)) }

        assertEquals(listOf(true, false, false), versions.map { it.isStable })
        assertEquals(listOf(false, true, false), versions.map { it.isAlpha })
        assertEquals(listOf(false, false, true), versions.map { it.isBeta })
    }
}
