package ui

import com.sdercolin.vlabeler.ui.AppUnsavedChangesStateImpl
import com.sdercolin.vlabeler.ui.ProjectWriteStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUnsavedChangesStateTest {

    @Test
    fun `initial state is updated without unsaved changes`() {
        val state = AppUnsavedChangesStateImpl()
        assertEquals(ProjectWriteStatus.Updated, state.projectWriteStatus)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.hasLoadedAutoSavedProject)
    }

    @Test
    fun `projectContentChanged marks unsaved changes`() {
        val state = AppUnsavedChangesStateImpl()
        state.projectContentChanged()
        assertEquals(ProjectWriteStatus.Changed, state.projectWriteStatus)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `requestProjectSave sets update requested`() {
        val state = AppUnsavedChangesStateImpl()
        state.projectContentChanged()
        state.requestProjectSave()
        assertEquals(ProjectWriteStatus.UpdateRequested, state.projectWriteStatus)
        // only `Changed` counts as unsaved changes
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `projectSaved resets the status and the auto-saved flag`() {
        val state = AppUnsavedChangesStateImpl()
        state.hasLoadedAutoSavedProject = true
        state.projectContentChanged()
        state.projectSaved()
        assertEquals(ProjectWriteStatus.Updated, state.projectWriteStatus)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.hasLoadedAutoSavedProject)
    }

    @Test
    fun `projectPathChanged keeps updated status for a normal project`() {
        val state = AppUnsavedChangesStateImpl()
        state.projectPathChanged()
        assertEquals(ProjectWriteStatus.Updated, state.projectWriteStatus)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `projectPathChanged marks changed for a loaded auto-saved project`() {
        val state = AppUnsavedChangesStateImpl()
        state.hasLoadedAutoSavedProject = true
        state.projectPathChanged()
        assertEquals(ProjectWriteStatus.Changed, state.projectWriteStatus)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `projectClosed resets the status`() {
        val state = AppUnsavedChangesStateImpl()
        state.projectContentChanged()
        state.projectClosed()
        assertEquals(ProjectWriteStatus.Updated, state.projectWriteStatus)
        assertFalse(state.hasUnsavedChanges)
    }
}
