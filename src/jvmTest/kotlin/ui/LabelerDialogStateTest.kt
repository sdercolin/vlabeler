package ui

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPreset
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPresetItem
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPresetTarget
import com.sdercolin.vlabeler.ui.dialog.plugin.LabelerDialogState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import testutil.TestEnv
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [LabelerDialogState].
 *
 * The state is exercised with the bundled `utau-singer.default` labeler ([TestLabelers.utauSinger]), which declares
 * both changeable (`dragBase`, ...) and unchangeable (`useRootDirectory`, `forceRecursive`) parameters.
 *
 * The saved-params file returned by `getSavedParamsFile()` lives in `RecordDir`, which is redirected to
 * `build/test-app-dir/.record` by the `VLABELER_APP_DIR` environment variable set on the test task, so nothing is
 * written to the real application directory. Created files are removed in [teardown].
 */
class LabelerDialogStateTest {

    private val tempDirs = mutableListOf<File>()
    private val recordFiles = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        RecordDir.mkdirs()
    }

    @AfterTest
    fun teardown() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        recordFiles.forEach { it.delete() }
        recordFiles.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private val labeler = TestLabelers.utauSinger

    private class Callbacks {
        val submitted = mutableListOf<ParamMap?>()
        val loaded = mutableListOf<ParamMap>()
        val saved = mutableListOf<ParamMap>()
    }

    private fun createState(
        callbacks: Callbacks = Callbacks(),
        isExistingProject: Boolean = false,
        paramMap: ParamMap = labeler.getDefaultParams(),
        savedParamMap: ParamMap? = labeler.getDefaultParams(),
    ): LabelerDialogState = LabelerDialogState(
        labeler = labeler,
        isExistingProject = isExistingProject,
        snackbarHostState = SnackbarHostState(),
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        submit = { callbacks.submitted += it },
        save = { callbacks.saved += it },
        load = { callbacks.loaded += it },
    )

    private fun LabelerDialogState.indexOf(name: String): Int = paramDefs.indexOfFirst { it.name == name }

    /**
     * Runs a suspending dialog action ([LabelerDialogState.import] / [LabelerDialogState.export]) that finishes by
     * showing a snackbar. Without a composed `SnackbarHost` the `showSnackbar` call suspends until the snackbar is
     * dismissed, so this helper dismisses it and returns the shown message.
     */
    private fun runDialogAction(state: LabelerDialogState, block: suspend () -> Unit): String? {
        var message: String? = null
        runBlocking {
            val job = launch(Dispatchers.Default) { block() }
            withTimeout(10_000) {
                while (job.isActive) {
                    state.snackbarHostState.currentSnackbarData?.let {
                        message = it.message
                        it.dismiss()
                    }
                    delay(10)
                }
            }
        }
        return message
    }

    @Test
    fun `basic configuration`() {
        val state = createState()
        assertNull(state.project)
        assertEquals(labeler, state.basePlugin)
        assertTrue(state.hasParams)
        assertFalse(state.needJsClient)
        assertEquals(
            listOf(
                Parameter.IntParam.Type,
                Parameter.FloatParam.Type,
                Parameter.BooleanParam.Type,
                Parameter.StringParam.Type,
                Parameter.EnumParam.Type,
                Parameter.FileParam.Type,
                Parameter.RawFileParam.Type,
            ),
            state.acceptedParamTypes,
        )
        assertFalse(state.acceptParamType(Parameter.EntrySelectorParam.Type))
        assertTrue(state.acceptParamType(Parameter.EnumParam.Type))
    }

    @Test
    fun `is changeable returns declared flags only for existing projects`() {
        val newProjectState = createState(isExistingProject = false)
        assertTrue(newProjectState.isChangeable("dragBase"))
        assertTrue(newProjectState.isChangeable("useRootDirectory"))
        assertTrue(newProjectState.isChangeable("unknown"))

        val existingProjectState = createState(isExistingProject = true)
        assertTrue(existingProjectState.isChangeable("dragBase"))
        assertFalse(existingProjectState.isChangeable("useRootDirectory"))
        assertFalse(existingProjectState.isChangeable("forceRecursive"))
        assertFailsWith<IllegalArgumentException> { existingProjectState.isChangeable("unknown") }
    }

    @Test
    fun `reset retains unchangeable params for existing projects`() {
        val state = createState(isExistingProject = true)
        val dragBaseIndex = state.indexOf("dragBase")
        val useRootDirectoryIndex = state.indexOf("useRootDirectory")
        val dragBaseDefault = state.paramDefs[dragBaseIndex].defaultValue
        val useRootDirectoryDefault = state.paramDefs[useRootDirectoryIndex].defaultValue as Boolean

        state.update(dragBaseIndex, "Left")
        state.update(useRootDirectoryIndex, !useRootDirectoryDefault)
        assertEquals(0, state.resetKey)

        state.reset()

        assertEquals(1, state.resetKey)
        assertEquals(dragBaseDefault, state.params[dragBaseIndex])
        assertEquals(!useRootDirectoryDefault, state.params[useRootDirectoryIndex])
    }

    @Test
    fun `reset restores all params for new projects`() {
        val state = createState(isExistingProject = false)
        val useRootDirectoryIndex = state.indexOf("useRootDirectory")
        val default = state.paramDefs[useRootDirectoryIndex].defaultValue as Boolean

        state.update(useRootDirectoryIndex, !default)
        state.reset()

        assertEquals(default, state.params[useRootDirectoryIndex])
        assertEquals(labeler.getDefaultParams(), state.getCurrentParamMap())
    }

    @Test
    fun `can reset ignores unchangeable params for existing projects`() {
        val existingProjectState = createState(isExistingProject = true)
        assertFalse(existingProjectState.canReset())
        val useRootDirectoryIndex = existingProjectState.indexOf("useRootDirectory")
        val default = existingProjectState.paramDefs[useRootDirectoryIndex].defaultValue as Boolean
        existingProjectState.update(useRootDirectoryIndex, !default)
        assertFalse(existingProjectState.canReset())
        existingProjectState.update(existingProjectState.indexOf("dragBase"), "Left")
        assertTrue(existingProjectState.canReset())

        val newProjectState = createState(isExistingProject = false)
        newProjectState.update(useRootDirectoryIndex, !default)
        assertTrue(newProjectState.canReset())
    }

    @Test
    fun `apply and cancel submit the current params or null`() {
        val callbacks = Callbacks()
        val state = createState(callbacks)
        state.update(state.indexOf("dragBase"), "Left")
        state.apply()
        state.cancel()
        assertEquals(2, callbacks.submitted.size)
        assertEquals(
            (labeler.getDefaultParams() + mapOf("dragBase" to "Left")).toParamMap(),
            callbacks.submitted[0],
        )
        assertNull(callbacks.submitted[1])
    }

    @Test
    fun `can save requires changed and valid params`() {
        val callbacks = Callbacks()
        val state = createState(callbacks)
        assertFalse(state.canSave())

        val dragBaseIndex = state.indexOf("dragBase")
        state.update(dragBaseIndex, "Left")
        assertTrue(state.canSave())

        state.update(dragBaseIndex, "NotAnOption")
        assertFalse(state.isValid(dragBaseIndex))
        assertFalse(state.isAllValid())
        assertFalse(state.canSave())

        state.update(dragBaseIndex, "Left")
        state.save()
        assertEquals(
            listOf((labeler.getDefaultParams() + mapOf("dragBase" to "Left")).toParamMap()),
            callbacks.saved,
        )

        val stateWithoutSaved = createState(savedParamMap = null)
        stateWithoutSaved.update(dragBaseIndex, "Left")
        assertFalse(stateWithoutSaved.canSave())
    }

    @Test
    fun `export to file and import back round trips`() {
        val state = createState()
        state.update(state.indexOf("dragBase"), "Left")
        val file = newTempDir().resolve("preset.json")

        val exportMessage = runDialogAction(state) { state.export(BasePluginPresetTarget.File(file)) }
        assertEquals(stringStatic(Strings.PluginDialogExportSuccess), exportMessage)

        val preset = file.readText().parseJson<BasePluginPreset>()
        assertEquals(labeler.name, preset.pluginName)
        assertEquals(labeler.version, preset.pluginVersion)
        assertEquals("Left", preset.params?.get("dragBase")?.value)
        assertNull(preset.params?.get("useNegativeOvl"))

        val callbacks = Callbacks()
        val freshState = createState(callbacks)
        val importMessage = runDialogAction(freshState) { freshState.import(BasePluginPresetTarget.File(file)) }
        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), importMessage)
        assertEquals(
            listOf((labeler.getDefaultParams() + mapOf("dragBase" to "Left")).toParamMap()),
            callbacks.loaded,
        )
    }

    @Test
    fun `import from file retains unchangeable params for existing projects`() {
        val state = createState()
        val useRootDirectoryIndex = state.indexOf("useRootDirectory")
        val useRootDirectoryDefault = state.paramDefs[useRootDirectoryIndex].defaultValue as Boolean
        state.update(state.indexOf("dragBase"), "Left")
        state.update(useRootDirectoryIndex, !useRootDirectoryDefault)
        val file = newTempDir().resolve("preset.json")
        runDialogAction(state) { state.export(BasePluginPresetTarget.File(file)) }

        val callbacks = Callbacks()
        val existingProjectState = createState(callbacks, isExistingProject = true)
        val message = runDialogAction(existingProjectState) {
            existingProjectState.import(BasePluginPresetTarget.File(file))
        }

        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), message)
        val loaded = callbacks.loaded.single()
        assertEquals("Left", loaded["dragBase"])
        // the unchangeable value from the preset is replaced by the current value of the dialog
        assertEquals(useRootDirectoryDefault, loaded["useRootDirectory"])
    }

    @Test
    fun `import fails on labeler name mismatch`() {
        val file = newTempDir().resolve("preset.json")
        file.writeText(
            BasePluginPreset(pluginName = "some-other-labeler", pluginVersion = 1, params = null).stringifyJson(),
        )
        val callbacks = Callbacks()
        val state = createState(callbacks)

        val message = runDialogAction(state) { state.import(BasePluginPresetTarget.File(file)) }

        assertEquals(stringStatic(Strings.PluginDialogImportFailure), message)
        assertEquals(emptyList(), callbacks.loaded)
    }

    @Test
    fun `import fails on an unparseable file`() {
        val file = newTempDir().resolve("preset.json").apply { writeText("not a preset") }
        val callbacks = Callbacks()
        val state = createState(callbacks)

        val message = runDialogAction(state) { state.import(BasePluginPresetTarget.File(file)) }

        assertEquals(stringStatic(Strings.PluginDialogImportFailure), message)
        assertEquals(emptyList(), callbacks.loaded)
    }

    @Test
    fun `export to memory writes the saved params file and import reads it back`() {
        val savedParamsFile = labeler.getSavedParamsFile().also { recordFiles += it }
        savedParamsFile.delete()
        val state = createState()
        state.update(state.indexOf("dragBase"), "Left")
        val memoryTarget = BasePluginPresetItem.Memory(
            pluginName = labeler.name,
            slot = null,
            isCurrent = true,
            available = true,
        ).resolve()

        val exportMessage = runDialogAction(state) { state.export(memoryTarget) }
        assertEquals(stringStatic(Strings.PluginDialogExportSuccess), exportMessage)
        assertTrue(savedParamsFile.exists())

        val callbacks = Callbacks()
        val freshState = createState(callbacks)
        val importMessage = runDialogAction(freshState) { freshState.import(memoryTarget) }
        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), importMessage)
        assertEquals(
            listOf((labeler.getDefaultParams() + mapOf("dragBase" to "Left")).toParamMap()),
            callbacks.loaded,
        )
    }

    @Test
    fun `import from memory without a saved file loads the defaults`() {
        labeler.getSavedParamsFile().delete()
        val callbacks = Callbacks()
        val state = createState(callbacks)
        val memoryTarget = BasePluginPresetItem.Memory(
            pluginName = labeler.name,
            slot = null,
            isCurrent = true,
            available = true,
        ).resolve()

        val message = runDialogAction(state) { state.import(memoryTarget) }

        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), message)
        assertEquals(listOf(labeler.getDefaultParams()), callbacks.loaded)
    }

    @Test
    fun `preset targets of a labeler are memory and file`() {
        val state = createState()
        val record = AppRecord()
        val importable = state.getImportablePresets(record)
        assertEquals(2, importable.size)
        val memory = importable.first() as BasePluginPresetItem.Memory
        assertEquals(labeler.name, memory.pluginName)
        assertNull(memory.slot)
        assertTrue(memory.isCurrent)
        assertTrue(memory.available)
        assertEquals(BasePluginPresetItem.File, importable.last())
        assertEquals(importable.map { it::class }, state.getExportablePresets(record).map { it::class })
    }
}
