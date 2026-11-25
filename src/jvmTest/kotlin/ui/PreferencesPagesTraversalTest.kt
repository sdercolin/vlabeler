package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.model.action.MouseScrollActionKeyBind
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesItem
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPage
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPages
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Exhaustive traversal of all [PreferencesPages] definitions.
 *
 * For every [PreferencesItem.Valued] item on every page, these tests assert that the item's `select`/`update`/`reset`
 * lambdas operate on the same [AppConf] field, so a copy-paste error in the big declarative page definitions (a
 * getter reading field A while the setter writes field B, an update touching more fields than it should, or two items
 * writing to the same field) fails the suite. The harness itself is verified by
 * [traversal harness detects an artificial select update field mismatch].
 */
class PreferencesPagesTraversalTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        // Make the repository-backed selection items (spectrogram color palette, view font family) provide their
        // real options. The app directory is redirected to the build directory via VLABELER_APP_DIR in tests, so
        // only preset/built-in entries are loaded and no user data is touched.
        ColorPaletteRepository.directory.mkdirs()
        ColorPaletteRepository.load()
        FontRepository.load()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun allPages(): List<PreferencesPage> {
        val result = mutableListOf<PreferencesPage>()
        fun visit(page: PreferencesPage) {
            result.add(page)
            page.children.forEach { visit(it) }
        }
        PreferencesPages.rootPages.forEach { visit(it) }
        return result
    }

    /** A [PreferencesItem] together with a human-readable location used in assertion messages. */
    private data class LocatedItem(val location: String, val item: PreferencesItem)

    private fun allItems(): List<LocatedItem> = allPages().flatMap { page ->
        // `content` is a `get() =` property, so it is captured exactly once here; all assertions of a test run
        // operate on the same item instances.
        page.content.flatMapIndexed { groupIndex, group ->
            group.items.mapIndexed { itemIndex, item ->
                val title = item.title?.name ?: item::class.simpleName
                LocatedItem("${page.name}[$groupIndex][$itemIndex]($title)", item)
            }
        }
    }

    private fun List<LocatedItem>.valued(): List<Pair<String, PreferencesItem.Valued<*>>> =
        mapNotNull { (location, item) -> (item as? PreferencesItem.Valued<*>)?.let { location to it } }

    /** Produces a value for the item that differs from its default value, or null if none can be derived. */
    private fun mutatedValueFor(item: PreferencesItem.Valued<*>): Any? = when (item) {
        is PreferencesItem.Switch -> item.defaultValue.not()
        is PreferencesItem.IntegerInput -> {
            val min = item.min
            val max = item.max
            listOfNotNull(item.defaultValue + 1, item.defaultValue - 1, min, max)
                .first { it != item.defaultValue && (min == null || it >= min) && (max == null || it <= max) }
        }
        is PreferencesItem.FloatInput -> {
            val min = item.min
            val max = item.max
            listOfNotNull(item.defaultValue + 1f, item.defaultValue - 1f, item.defaultValue / 2f, min, max)
                .first { it != item.defaultValue && (min == null || it >= min) && (max == null || it <= max) }
        }
        is PreferencesItem.ColorStringInput ->
            listOf(if (item.useAlpha) "#80123456" else "#123456", "#654321").first { it != item.defaultValue }
        is PreferencesItem.StringInput -> item.defaultValue + "-modified"
        is PreferencesItem.StringListInput -> item.defaultValue + "added-entry"
        is PreferencesItem.Selection -> item.options.firstOrNull { it != item.defaultValue }
        is PreferencesItem.Keymap<*> -> when (item.actionType) {
            ActionType.Key -> listOf(
                KeyActionKeyBind(KeyAction.NewProject, KeySet(Key.K, setOf(Key.Alt))),
            )
            ActionType.MouseClick -> listOf(
                MouseClickActionKeyBind(MouseClickAction.MoveParameter, KeySet(Key.MouseRightClick, setOf(Key.Alt))),
            )
            ActionType.MouseScroll -> listOf(
                MouseScrollActionKeyBind(MouseScrollAction.GoToNextEntry, KeySet(Key.MouseScrollUp, setOf(Key.Alt))),
            )
        }
    }

    /**
     * Asserts that setting a non-default value via the item's `update` is read back by the item's `select`, actually
     * changes the conf, and survives a serialization round trip. Returns the updated conf, or null if no alternative
     * value exists for the item (single-option selections).
     */
    @Suppress("UNCHECKED_CAST")
    private fun assertItemRoundTrip(location: String, item: PreferencesItem.Valued<*>): AppConf? {
        val valuedItem = item as PreferencesItem.Valued<Any?>
        val newValue = runCatching { mutatedValueFor(item) }
            .getOrElse { fail("$location: cannot generate a modified test value", it) }
            ?: return null
        assertNotEquals(item.defaultValue, newValue, "$location: generated test value equals the default")

        val updated = valuedItem.update(AppConf(), newValue)
        assertEquals(newValue, valuedItem.select(updated), "$location: select does not read back the updated value")
        assertNotEquals(AppConf(), updated, "$location: update did not change the conf")

        val reserialized = updated.stringifyJson().parseJson<AppConf>()
        assertEquals(newValue, valuedItem.select(reserialized), "$location: value lost in serialization round trip")

        val reset = valuedItem.reset(updated)
        assertEquals(item.defaultValue, valuedItem.select(reset), "$location: reset does not restore the default")
        return updated
    }

    @Test
    fun `all pages are reachable and distinct`() {
        val pages = allPages()
        assertEquals(pages.size, pages.distinct().size, "duplicated page in the tree")
        // 9 root pages + 6 charts children + 3 keymap children + 6 editor children
        assertEquals(24, pages.size)
        pages.forEach { page ->
            assertEquals(page.displayedName.name, page.name)
        }
    }

    @Test
    fun `every item enabled lambda evaluates on the default conf`() {
        allItems().forEach { (_, item) ->
            // executes the declarative `enabled` lambdas; must not throw
            item.enabled(AppConf())
        }
    }

    @Test
    fun `every valued item declares the default value of its own conf field`() {
        val items = allItems().valued()
        assertTrue(items.size >= 100, "expected the traversal to find at least 100 valued items, got ${items.size}")
        items
            .forEach { (location, item) ->
                assertEquals(
                    item.defaultValue,
                    item.select(AppConf()),
                    "$location: declared default value differs from the AppConf field default",
                )
            }
    }

    @Test
    fun `every valued item round-trips a modified value and resets back to its default`() {
        var mutated = 0
        allItems().valued().forEach { (location, item) ->
            if (assertItemRoundTrip(location, item) != null) mutated++
        }
        assertTrue(mutated >= 100, "expected at least 100 items to be mutated, got $mutated")
    }

    @Test
    fun `every valued item resets its page copy exactly back to the default conf`() {
        allItems().valued()
            .forEach { (location, item) ->
                @Suppress("UNCHECKED_CAST")
                val valuedItem = item as PreferencesItem.Valued<Any?>
                val newValue = mutatedValueFor(item) ?: return@forEach
                val updated = valuedItem.update(AppConf(), newValue)
                assertEquals(
                    AppConf(),
                    valuedItem.reset(updated),
                    "$location: reset does not restore the exact default conf, the update touches other fields",
                )
            }
    }

    @Test
    fun `valued items write to distinct conf fields`() {
        val updatedConfs = allItems().valued().mapNotNull { (location, item) ->
            @Suppress("UNCHECKED_CAST")
            val valuedItem = item as PreferencesItem.Valued<Any?>
            val newValue = runCatching { mutatedValueFor(item) }
                .getOrElse { fail("$location: cannot generate a modified test value", it) }
                ?: return@mapNotNull null
            location to valuedItem.update(AppConf(), newValue)
        }
        val collisions = updatedConfs.groupBy({ it.second }, { it.first }).filterValues { it.size > 1 }
        assertTrue(
            collisions.isEmpty(),
            "items produced identical updated confs (same field written): ${collisions.values}",
        )
    }

    @Test
    fun `traversal harness detects an artificial select update field mismatch`() {
        // select reads view.hideSampleExtension but update writes misc.useCustomFileDialog
        val mismatchedItem = PreferencesItem.Switch(
            title = Strings.PreferencesViewHideSampleExtension,
            description = null,
            clickableTags = listOf(),
            columnStyle = false,
            defaultValue = AppConf().view.hideSampleExtension,
            select = { it.view.hideSampleExtension },
            update = { copy(misc = misc.copy(useCustomFileDialog = it)) },
            enabled = { true },
        )
        assertFails { assertItemRoundTrip("artificial-mismatch", mismatchedItem) }
    }

    @Test
    fun `traversal harness detects an artificial update touching an extra field`() {
        // update writes the selected field but also flips an unrelated one, which reset cannot restore
        val overreachingItem = PreferencesItem.Switch(
            title = Strings.PreferencesViewHideSampleExtension,
            description = null,
            clickableTags = listOf(),
            columnStyle = false,
            defaultValue = AppConf().view.hideSampleExtension,
            select = { it.view.hideSampleExtension },
            update = {
                copy(
                    view = view.copy(hideSampleExtension = it),
                    misc = misc.copy(useCustomFileDialog = true),
                )
            },
            enabled = { true },
        )
        val newValue = AppConf().view.hideSampleExtension.not()
        val updated = overreachingItem.update(AppConf(), newValue)
        assertFails {
            assertEquals(AppConf(), overreachingItem.reset(updated))
        }
    }

    fun `keymap pages expose exactly one keymap item of their action type`() {
        val expectations = mapOf(
            PreferencesPages.KeymapKeyAction to ActionType.Key,
            PreferencesPages.KeymapMouseClickAction to ActionType.MouseClick,
            PreferencesPages.KeymapMouseScrollAction to ActionType.MouseScroll,
        )
        expectations.forEach { (page, actionType) ->
            val items = page.content.flatMap { it.items }
            val keymapItem = items.filterIsInstance<PreferencesItem.Keymap<*>>().singleOrNull()
                ?: fail("${page.name}: expected exactly one keymap item")
            assertEquals(actionType, keymapItem.actionType, page.name)
        }
    }
}
