package ui

import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.getConflictingKeyBinds
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the conflict detection used by the keymap item edit dialog
 * ([com.sdercolin.vlabeler.ui.dialog.preferences.KeymapItemEditConflictDialog]), implemented by
 * [getConflictingKeyBinds].
 */
class KeymapConflictDetectionTest {

    private val keySet = KeySet(Key.J, setOf(Key.Ctrl))

    @Test
    fun `a null key set never conflicts`() {
        val binds = listOf(
            KeyActionKeyBind(KeyAction.NewProject, keySet),
            KeyActionKeyBind(KeyAction.OpenProject, keySet),
        )
        assertTrue(binds.getConflictingKeyBinds(null, KeyAction.SaveProject).isEmpty())
    }

    @Test
    fun `key binds with the same key set and another action conflict`() {
        val binds = listOf(
            KeyActionKeyBind(KeyAction.NewProject, keySet),
            KeyActionKeyBind(KeyAction.OpenProject, KeySet(Key.O, setOf(Key.Ctrl))),
            KeyActionKeyBind(KeyAction.SaveProject, null),
        )
        assertEquals(
            listOf(KeyActionKeyBind(KeyAction.NewProject, keySet)),
            binds.getConflictingKeyBinds(keySet, KeyAction.SaveProject),
        )
    }

    @Test
    fun `the edited action itself is not reported as a conflict`() {
        val binds = listOf(KeyActionKeyBind(KeyAction.NewProject, keySet))
        assertTrue(binds.getConflictingKeyBinds(keySet, KeyAction.NewProject).isEmpty())
    }

    @Test
    fun `mouse click actions only conflict within the same tool`() {
        val clickKeySet = KeySet(Key.MouseLeftClick, setOf(Key.Shift))
        // MoveParameter uses the cursor tool while PlayAudioUntilEnd uses the playback tool
        val cursorBind = MouseClickActionKeyBind(MouseClickAction.MoveParameter, clickKeySet)
        val playbackBind = MouseClickActionKeyBind(MouseClickAction.PlayAudioUntilEnd, clickKeySet)
        val binds = listOf(cursorBind, playbackBind)

        assertEquals(
            listOf(cursorBind),
            binds.getConflictingKeyBinds(clickKeySet, MouseClickAction.MoveParameterIgnoringConstraints),
        )
        assertEquals(
            listOf(playbackBind),
            binds.getConflictingKeyBinds(clickKeySet, MouseClickAction.PlayAudioFromStart),
        )
    }

    @Test
    fun `key binds with different key sets do not conflict`() {
        val binds = listOf(KeyActionKeyBind(KeyAction.NewProject, KeySet(Key.N, setOf(Key.Ctrl))))
        assertTrue(binds.getConflictingKeyBinds(keySet, KeyAction.SaveProject).isEmpty())
    }
}
