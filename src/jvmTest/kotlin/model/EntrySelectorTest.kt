package model

import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for serialization of [EntrySelector].
 */
class EntrySelectorTest {

    @Test
    fun testSerialization() {
        val selector = EntrySelector(
            listOf(
                EntrySelector.TextFilterItem(
                    "name",
                    matchType = EntrySelector.TextMatchType.Contains,
                    matcherText = "a",
                ),
                EntrySelector.NumberFilterItem(
                    "fixed",
                    matchType = EntrySelector.NumberMatchType.Equals,
                    comparerValue = 1.1,
                    comparerName = null,
                ),
                EntrySelector.NumberFilterItem(
                    "preu",
                    matchType = EntrySelector.NumberMatchType.LessThan,
                    comparerValue = 0.0,
                    comparerName = "overlap",
                ),
            ),
        )
        val json = selector.stringifyJson()
        val deserialized = json.parseJson<EntrySelector>()
        assertEquals(selector, deserialized)
    }
}
