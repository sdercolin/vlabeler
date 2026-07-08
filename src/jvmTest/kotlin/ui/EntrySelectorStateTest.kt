package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.LogicalExpression
import com.sdercolin.vlabeler.util.JavaScript
import testutil.TestEnv
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the non-composable logic behind the entry selector UI
 * ([com.sdercolin.vlabeler.ui.dialog.plugin.ParamEntrySelector]): the [LogicalExpression] parsing used to validate the
 * expression input, and the [EntrySelector] filter/selection model used to compute the preview summary.
 *
 * The `oto-plus.default` labeler ([TestLabelers.utauOto]) is used for property based filters. Its `points` are
 * `[fixed, preu, ovl, left]` and its properties are computed as `left = points[3]`, `ovl = points[2] - points[3]` and
 * `preu = points[1] - points[3]`.
 */
class EntrySelectorStateTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun <T> withJs(block: (JavaScript) -> T): T = JavaScript(System.out).use(block)

    private fun entry(
        name: String,
        sample: String = "$name.wav",
        points: List<Float> = listOf(300f, 250f, 150f, 100f),
        done: Boolean = false,
        star: Boolean = false,
        tag: String = "",
    ) = Entry(
        sample = sample,
        name = name,
        start = 0f,
        end = 1000f,
        points = points,
        extras = listOf("500"),
        notes = EntryNotes(done = done, star = star, tag = tag),
    )

    /* region LogicalExpression */

    @Test
    fun `logical expression parses placeholders and operators`() {
        val and = LogicalExpression.parse("#1 and #2").getOrThrow()
        assertEquals(2, and.requiredPlaceholderCount)
        assertTrue(and.evaluate(listOf(true, true)))
        assertFalse(and.evaluate(listOf(true, false)))

        val or = LogicalExpression.parse("#1 || #2").getOrThrow()
        assertTrue(or.evaluate(listOf(false, true)))
        assertFalse(or.evaluate(listOf(false, false)))

        val xor = LogicalExpression.parse("#1 xor #2").getOrThrow()
        assertTrue(xor.evaluate(listOf(true, false)))
        assertFalse(xor.evaluate(listOf(true, true)))

        val not = LogicalExpression.parse("not #1").getOrThrow()
        assertEquals(1, not.requiredPlaceholderCount)
        assertTrue(not.evaluate(listOf(false)))
        assertFalse(not.evaluate(listOf(true)))

        val uppercase = LogicalExpression.parse("#1 AND #2").getOrThrow()
        assertTrue(uppercase.evaluate(listOf(true, true)))
    }

    @Test
    fun `logical expression supports parentheses and left associativity`() {
        val nested = LogicalExpression.parse("#1 or (#2 and not #3)").getOrThrow()
        assertEquals(3, nested.requiredPlaceholderCount)
        assertTrue(nested.evaluate(listOf(true, false, true)))
        assertTrue(nested.evaluate(listOf(false, true, false)))
        assertFalse(nested.evaluate(listOf(false, true, true)))

        // without parentheses the expression is evaluated left to right: ((#1 and #2) or #3)
        val leftAssociative = LogicalExpression.parse("#1 and #2 or #3").getOrThrow()
        assertTrue(leftAssociative.evaluate(listOf(false, false, true)))
        assertTrue(leftAssociative.evaluate(listOf(true, true, false)))
        assertFalse(leftAssociative.evaluate(listOf(true, false, false)))

        val notFirst = LogicalExpression.parse("not #1 and #2").getOrThrow()
        assertTrue(notFirst.evaluate(listOf(false, true)))
        assertFalse(notFirst.evaluate(listOf(true, true)))
    }

    @Test
    fun `logical expression placeholder count is based on the maximum index`() {
        val expression = LogicalExpression.parse("#3").getOrThrow()
        assertEquals(3, expression.requiredPlaceholderCount)
        assertTrue(expression.evaluate(listOf(false, false, true)))
    }

    @Test
    fun `logical expression rejects malformed input`() {
        listOf(
            "",
            "#0",
            "#1 and",
            "#1 #2",
            "(#1",
            "()",
            "foo",
            "and #1",
        ).forEach { expression ->
            assertTrue(
                LogicalExpression.parse(expression).isFailure,
                "expected \"$expression\" to fail parsing",
            )
        }
    }

    @Test
    fun `default logical expression combines all placeholders with and`() {
        val default = LogicalExpression.parse("#1 and #2 and #3").getOrThrow()
        val built = LogicalExpression.default(3)
        assertEquals(3, built?.requiredPlaceholderCount)
        listOf(
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(false, false, false),
        ).forEach { values ->
            assertEquals(default.evaluate(values), built?.evaluate(values))
        }
        assertNull(LogicalExpression.default(0))
    }

    /* endregion */

    /* region EntrySelector validity */

    @Test
    fun `selector validity checks the expression against the filter count`() {
        val labeler = TestLabelers.utauOto
        val filter = EntrySelector.TextFilterItem("name", EntrySelector.TextMatchType.Contains, "a")
        assertTrue(EntrySelector(listOf(filter), "#1").isValid(labeler))
        assertFalse(EntrySelector(listOf(filter), "#1 and #2").isValid(labeler))
        assertTrue(EntrySelector(listOf(filter, filter), "#1 and #2").isValid(labeler))
        assertFalse(EntrySelector(listOf(filter), "#1 and").isValid(labeler))
        assertTrue(EntrySelector(emptyList()).isValid(labeler))
        assertTrue(EntrySelector(emptyList()).isEmpty())
    }

    @Test
    fun `text filter validity checks subject match type and matcher`() {
        val labeler = TestLabelers.utauOto
        fun filter(subject: String, matchType: EntrySelector.TextMatchType, text: String) =
            EntrySelector.TextFilterItem(subject, matchType, text)

        assertTrue(filter("name", EntrySelector.TextMatchType.Equals, "a").isValid(labeler))
        assertTrue(filter("sample", EntrySelector.TextMatchType.Contains, "a").isValid(labeler))
        assertTrue(filter("tag", EntrySelector.TextMatchType.Regex, "a+").isValid(labeler))
        assertFalse(filter("unknown", EntrySelector.TextMatchType.Equals, "a").isValid(labeler))
        assertFalse(filter("name", EntrySelector.TextMatchType.Equals, "").isValid(labeler))
        assertFalse(filter("name", EntrySelector.TextMatchType.Regex, "[").isValid(labeler))
    }

    @Test
    fun `number filter validity checks property names`() {
        val labeler = TestLabelers.utauOto
        val valid = EntrySelector.NumberFilterItem("left", EntrySelector.NumberMatchType.Equals, 0.0, null)
        assertTrue(valid.isValid(labeler))
        val withComparer = EntrySelector.NumberFilterItem("left", EntrySelector.NumberMatchType.Equals, 0.0, "preu")
        assertTrue(withComparer.isValid(labeler))
        val unknownSubject = EntrySelector.NumberFilterItem("nope", EntrySelector.NumberMatchType.Equals, 0.0, null)
        assertFalse(unknownSubject.isValid(labeler))
        val unknownComparer = EntrySelector.NumberFilterItem("left", EntrySelector.NumberMatchType.Equals, 0.0, "nope")
        assertFalse(unknownComparer.isValid(labeler))
    }

    @Test
    fun `boolean and script filter validity`() {
        val labeler = TestLabelers.utauOto
        assertTrue(EntrySelector.BooleanFilterItem("done", true).isValid(labeler))
        assertTrue(EntrySelector.BooleanFilterItem("star", false).isValid(labeler))
        assertFalse(EntrySelector.BooleanFilterItem("name", true).isValid(labeler))
        assertTrue(EntrySelector.ScriptFilterItem("entry.name === 'a'").isValid(labeler))
    }

    /* endregion */

    /* region EntrySelector selection */

    @Test
    fun `empty selector selects all entries`() {
        val entries = listOf(entry("a"), entry("b"), entry("c"))
        val selected = withJs { js ->
            EntrySelector(emptyList()).select(entries, TestLabelers.utauOto, js)
        }
        assertEquals(listOf(0, 1, 2), selected)
    }

    @Test
    fun `text filters select entries by name sample and tag`() {
        val entries = listOf(
            entry("ka", sample = "first.wav", tag = "todo"),
            entry("ki", sample = "second.wav", tag = "done"),
            entry("sa", sample = "second.wav", tag = ""),
        )
        val labeler = TestLabelers.utauOto
        withJs { js ->
            fun select(subject: String, matchType: EntrySelector.TextMatchType, text: String) =
                EntrySelector(listOf(EntrySelector.TextFilterItem(subject, matchType, text)))
                    .select(entries, labeler, js)

            assertEquals(listOf(0), select("name", EntrySelector.TextMatchType.Equals, "ka"))
            assertEquals(listOf(0, 1), select("name", EntrySelector.TextMatchType.StartsWith, "k"))
            assertEquals(listOf(0, 2), select("name", EntrySelector.TextMatchType.EndsWith, "a"))
            assertEquals(listOf(0, 1), select("name", EntrySelector.TextMatchType.Regex, "k."))
            assertEquals(listOf(1, 2), select("sample", EntrySelector.TextMatchType.Contains, "second"))
            assertEquals(listOf(1), select("tag", EntrySelector.TextMatchType.Equals, "done"))
        }
    }

    @Test
    fun `boolean filters select entries by done and star`() {
        val entries = listOf(
            entry("a", done = true, star = false),
            entry("b", done = false, star = true),
            entry("c", done = true, star = true),
        )
        val labeler = TestLabelers.utauOto
        withJs { js ->
            assertEquals(
                listOf(0, 2),
                EntrySelector(listOf(EntrySelector.BooleanFilterItem("done", true))).select(entries, labeler, js),
            )
            assertEquals(
                listOf(0),
                EntrySelector(listOf(EntrySelector.BooleanFilterItem("star", false))).select(entries, labeler, js),
            )
        }
    }

    @Test
    fun `number filters select entries by property values`() {
        // left = points[3], ovl = points[2] - points[3], preu = points[1] - points[3]
        val entries = listOf(
            entry("a", points = listOf(300f, 250f, 150f, 100f)), // left = 100, ovl = 50, preu = 150
            entry("b", points = listOf(300f, 200f, 260f, 50f)), // left = 50, ovl = 210, preu = 150
        )
        val labeler = TestLabelers.utauOto
        withJs { js ->
            val leftOver60 =
                EntrySelector.NumberFilterItem("left", EntrySelector.NumberMatchType.GreaterThan, 60.0, null)
            assertEquals(
                listOf(0),
                EntrySelector(listOf(leftOver60)).select(entries, labeler, js),
            )
            val ovlOverPreu =
                EntrySelector.NumberFilterItem("ovl", EntrySelector.NumberMatchType.GreaterThan, 0.0, "preu")
            assertEquals(
                listOf(1),
                EntrySelector(listOf(ovlOverPreu)).select(entries, labeler, js),
            )
            assertEquals(
                listOf(0, 1),
                EntrySelector(
                    listOf(EntrySelector.NumberFilterItem("preu", EntrySelector.NumberMatchType.Equals, 150.0, null)),
                ).select(entries, labeler, js),
            )
        }
    }

    @Test
    fun `script filters select entries by evaluating the expression`() {
        val entries = listOf(
            entry("ka", points = listOf(300f, 250f, 150f, 100f)),
            entry("sa", points = listOf(300f, 200f, 260f, 50f)),
        )
        val labeler = TestLabelers.utauOto
        withJs { js ->
            assertEquals(
                listOf(0),
                EntrySelector(listOf(EntrySelector.ScriptFilterItem("entry.name.startsWith('k')")))
                    .select(entries, labeler, js),
            )
            // properties are injected into the entry before evaluation
            assertEquals(
                listOf(0),
                EntrySelector(listOf(EntrySelector.ScriptFilterItem("entry.left > 60")))
                    .select(entries, labeler, js),
            )
            // a blank script accepts everything
            assertEquals(
                listOf(0, 1),
                EntrySelector(listOf(EntrySelector.ScriptFilterItem(""))).select(entries, labeler, js),
            )
        }
    }

    @Test
    fun `script filters require a boolean result`() {
        val entries = listOf(entry("a"))
        withJs { js ->
            assertFailsWith<IllegalStateException> {
                EntrySelector(listOf(EntrySelector.ScriptFilterItem("1 + 1")))
                    .select(entries, TestLabelers.utauOto, js)
            }
        }
    }

    @Test
    fun `raw expression combines filter results`() {
        val entries = listOf(
            entry("ka", star = true),
            entry("ki", star = false),
            entry("sa", star = true),
            entry("su", star = false),
        )
        val labeler = TestLabelers.utauOto
        val startsWithK = EntrySelector.TextFilterItem("name", EntrySelector.TextMatchType.StartsWith, "k")
        val starred = EntrySelector.BooleanFilterItem("star", true)
        withJs { js ->
            assertEquals(
                listOf(0),
                EntrySelector(listOf(startsWithK, starred)).select(entries, labeler, js),
            )
            assertEquals(
                listOf(0, 1, 2),
                EntrySelector(listOf(startsWithK, starred), "#1 or #2").select(entries, labeler, js),
            )
            assertEquals(
                listOf(2, 3),
                EntrySelector(listOf(startsWithK, starred), "not #1").select(entries, labeler, js),
            )
            assertEquals(
                listOf(1, 2),
                EntrySelector(listOf(startsWithK, starred), "#1 xor #2").select(entries, labeler, js),
            )
        }
    }

    @Test
    fun `select fails on an unparseable raw expression`() {
        val entries = listOf(entry("a"))
        val filter = EntrySelector.TextFilterItem("name", EntrySelector.TextMatchType.Contains, "a")
        val selector = EntrySelector(listOf(filter), "#1 and")
        assertFalse(selector.isValid(TestLabelers.utauOto))
        withJs { js ->
            // the incomplete expression is rejected by `error()` inside the parser
            assertFailsWith<IllegalStateException> {
                selector.select(entries, TestLabelers.utauOto, js)
            }
        }
    }

    /* endregion */
}
