package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditorState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesItem
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesKeymapState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPages
import com.sdercolin.vlabeler.ui.string.Language
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [PreferencesKeymapState]. See [PreferencesEditorStateTest] for the safety argument of the uninitialized
 * [AppState] construction.
 */
class PreferencesKeymapStateTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        ColorPaletteRepository.directory.mkdirs()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun createEditorState(initConf: AppConf = AppConf()) = PreferencesEditorState(
        appState = uninitializedAppState(),
        initConf = initConf,
        submit = {},
        apply = {},
        initialPage = null,
        onViewPage = {},
        showSnackbar = {},
        launchArgs = null,
    )

    @Suppress("UNCHECKED_CAST")
    private val keyActionKeymapItem = PreferencesPages.KeymapKeyAction.content
        .flatMap { it.items }
        .filterIsInstance<PreferencesItem.Keymap<*>>()
        .single() as PreferencesItem.Keymap<KeyAction>

    private fun createKeymapState(initConf: AppConf = AppConf()) =
        PreferencesKeymapState(keyActionKeymapItem, createEditorState(initConf))

    private val customKeySet = KeySet(Key.J, setOf(Key.Ctrl, Key.Alt, Key.Shift))

    private fun confWithCustomBind(action: KeyAction, keySet: KeySet): AppConf = AppConf().let {
        it.copy(keymaps = it.keymaps.copy(keyActionMap = mapOf(action to keySet)))
    }

    @Test
    fun `all key binds cover every key action with its default key set`() {
        val state = createKeymapState()
        assertEquals(KeyAction.entries.size, state.allKeyBinds.size)
        assertEquals(
            KeyAction.entries.sortedBy { it.displayOrder }.map { KeyActionKeyBind(it, it.defaultKeySet) },
            state.allKeyBinds,
        )
        assertEquals(state.allKeyBinds, state.displayedKeyBinds)
        assertEquals("", state.searchText)
    }

    @Test
    fun `a custom key bind replaces the default one`() {
        val state = createKeymapState(confWithCustomBind(KeyAction.SaveProject, customKeySet))
        assertEquals(KeyAction.entries.size, state.allKeyBinds.size)
        assertEquals(
            KeyActionKeyBind(KeyAction.SaveProject, customKeySet),
            state.allKeyBinds.single { it.action == KeyAction.SaveProject },
        )
    }

    @Test
    fun `search filters the displayed key binds by title`() {
        val state = createKeymapState()
        state.search("save", Language.English)

        assertEquals("save", state.searchText)
        assertTrue(state.displayedKeyBinds.isNotEmpty())
        assertTrue(
            state.displayedKeyBinds.all {
                it.getTitle(Language.English).contains("save", ignoreCase = true)
            },
        )
        assertTrue(state.displayedKeyBinds.any { it.action == KeyAction.SaveProject })
        // the full list stays intact
        assertEquals(KeyAction.entries.size, state.allKeyBinds.size)
    }

    @Test
    fun `clearing the search restores all key binds`() {
        val state = createKeymapState()
        state.search("save", Language.English)
        state.search("", Language.English)
        assertEquals(state.allKeyBinds, state.displayedKeyBinds)
    }

    @Test
    fun `update refreshes the key binds and keeps the search text`() {
        val state = createKeymapState()
        state.search("save", Language.English)

        state.update(confWithCustomBind(KeyAction.SaveProject, customKeySet), Language.English)

        assertEquals("save", state.searchText)
        assertEquals(
            customKeySet,
            state.displayedKeyBinds.single { it.action == KeyAction.SaveProject }.keySet,
        )
    }
}
