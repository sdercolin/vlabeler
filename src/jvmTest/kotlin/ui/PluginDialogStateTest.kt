package ui

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.PluginQuickLaunch
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPreset
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPresetItem
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginPresetTarget
import com.sdercolin.vlabeler.ui.dialog.plugin.PluginDialogState
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for [PluginDialogState] and the shared logic in
 * [com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginDialogState], using the bundled plugins loaded via [loadPlugins].
 *
 * Saved-params files are written to `RecordDir`, and the [AppRecordStore]-backed quick launch slots to
 * `AppRecordFile`; both live under `build/test-app-dir` because the test task sets the `VLABELER_APP_DIR` environment
 * variable, so nothing is written to the real application directory. Created files are removed in [teardown].
 *
 * When an [AppRecordStore] only needs to be read, it is created with an already-cancelled scope so that its
 * `collectAndWrite()` loop never runs (the safety of this pattern is asserted in [ProjectCreatorStateTest]). The
 * export-to-slot test needs `update` to be applied, so it uses a live scope that is cancelled in [teardown].
 */
class PluginDialogStateTest {

    private lateinit var storeScope: CoroutineScope
    private val tempDirs = mutableListOf<File>()
    private val recordFiles = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        RecordDir.mkdirs()
        storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        storeScope.cancel()
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        recordFiles.forEach { it.delete() }
        recordFiles.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    private class Callbacks {
        val submitted = mutableListOf<ParamMap?>()
        val loaded = mutableListOf<ParamMap>()
        val saved = mutableListOf<ParamMap>()
    }

    private fun createState(
        plugin: Plugin,
        callbacks: Callbacks = Callbacks(),
        paramMap: ParamMap = plugin.getDefaultParams(),
        savedParamMap: ParamMap? = plugin.getDefaultParams(),
        project: Project? = null,
        store: AppRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
        slot: Int? = null,
    ): PluginDialogState = PluginDialogState(
        plugin = plugin,
        appRecordStore = store,
        snackbarHostState = SnackbarHostState(),
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        project = project,
        submit = { callbacks.submitted += it },
        load = { callbacks.loaded += it },
        save = { callbacks.saved += it },
        executable = false,
        slot = slot,
    )

    private fun PluginDialogState.indexOf(name: String): Int = paramDefs.indexOfFirst { it.name == name }

    private fun memoryTarget(pluginName: String?, slot: Int?): BasePluginPresetTarget.Memory =
        BasePluginPresetItem.Memory(
            pluginName = pluginName,
            slot = slot,
            isCurrent = slot == null,
            available = true,
        ).resolve()

    /**
     * Runs a suspending dialog action ([PluginDialogState.import] / [PluginDialogState.export]) that finishes by
     * showing a snackbar. Without a composed `SnackbarHost` the `showSnackbar` call suspends until the snackbar is
     * dismissed, so this helper dismisses it and returns the shown message.
     */
    private fun runDialogAction(state: PluginDialogState, block: suspend () -> Unit): String? {
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

    private fun awaitRecord(store: AppRecordStore, predicate: (AppRecord) -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (predicate(store.value)) return
            Thread.sleep(10)
        }
        fail("The app record was not updated in time")
    }

    /* region params */

    @Test
    fun `params are initialized from the param map`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        assertTrue(state.hasParams)
        assertEquals(plugin.parameterDefs, state.paramDefs)
        assertEquals(plugin.getDefaultParams(), state.getCurrentParamMap())
        assertTrue(state.isAllValid())
        assertTrue(state.isChangeable("bpm"))
    }

    @Test
    fun `update replaces a param value and validates it`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        val bpmIndex = state.indexOf("bpm")

        state.update(bpmIndex, 90f)
        assertEquals(90f, state.getCurrentParamMap()["bpm"])
        assertTrue(state.isValid(bpmIndex))

        // bpm has min 0
        state.update(bpmIndex, -1f)
        assertFalse(state.isValid(bpmIndex))
        assertFalse(state.isAllValid())

        // repeatSuffix is a non-optional string
        val repeatSuffixIndex = state.indexOf("repeatSuffix")
        state.update(repeatSuffixIndex, "")
        assertFalse(state.isValid(repeatSuffixIndex))

        // order is an enum
        val orderIndex = state.indexOf("order")
        state.update(orderIndex, "NotAnOption")
        assertFalse(state.isValid(orderIndex))
    }

    @Test
    fun `parse errors invalidate the dialog`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        assertTrue(state.isAllValid())
        state.setParseError(0, true)
        assertFalse(state.isAllValid())
        state.setParseError(0, false)
        assertTrue(state.isAllValid())
    }

    @Test
    fun `reset restores default values and bumps the reset key`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        assertFalse(state.canReset())
        val bpmIndex = state.indexOf("bpm")

        state.update(bpmIndex, 90f)
        assertTrue(state.canReset())
        assertEquals(0, state.resetKey)

        state.reset()

        assertEquals(1, state.resetKey)
        assertEquals(plugin.getDefaultParams(), state.getCurrentParamMap())
        assertFalse(state.canReset())
    }

    @Test
    fun `apply and cancel submit the current params or null`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks)
        state.update(state.indexOf("bpm"), 90f)

        state.apply()
        state.cancel()

        assertEquals(2, callbacks.submitted.size)
        assertEquals((plugin.getDefaultParams() + mapOf("bpm" to 90f)).toParamMap(), callbacks.submitted[0])
        assertNull(callbacks.submitted[1])
    }

    @Test
    fun `can save requires changed and valid params`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks)
        assertFalse(state.canSave())

        val bpmIndex = state.indexOf("bpm")
        state.update(bpmIndex, 90f)
        assertTrue(state.canSave())

        state.update(bpmIndex, -1f)
        assertFalse(state.canSave())

        state.update(bpmIndex, 90f)
        state.save()
        assertEquals(listOf((plugin.getDefaultParams() + mapOf("bpm" to 90f)).toParamMap()), callbacks.saved)

        val stateWithoutSaved = createState(plugin, savedParamMap = null)
        stateWithoutSaved.update(bpmIndex, 90f)
        assertFalse(stateWithoutSaved.canSave())
    }

    @Test
    fun `js client and entry selector params are only for macro plugins`() {
        val macroState = createState(macroPlugin("batch-remove-entry"))
        assertTrue(macroState.needJsClient)
        assertTrue(macroState.acceptParamType(Parameter.EntrySelectorParam.Type))

        val templateState = createState(templatePlugin("cvvc-oto-gen"))
        assertFalse(templateState.needJsClient)
        assertFalse(templateState.acceptParamType(Parameter.EntrySelectorParam.Type))
        assertTrue(templateState.acceptParamType(Parameter.FloatParam.Type))
    }

    @Test
    fun `entry selector params are laid out in their own row`() {
        val macroState = createState(macroPlugin("batch-edit-entry-name"))
        assertFalse(macroState.isParamInRow(macroState.indexOf("selector")))
        assertTrue(macroState.isParamInRow(macroState.indexOf("from")))

        val templateState = createState(templatePlugin("cvvc-oto-gen"))
        assertTrue(templateState.isParamInRow(templateState.indexOf("bpm")))
    }

    @Test
    fun `entry selector params require a project to be valid`() {
        val plugin = macroPlugin("batch-remove-entry")
        val stateWithoutProject = createState(plugin)
        val selectorIndex = stateWithoutProject.indexOf("selector")
        assertFalse(stateWithoutProject.isValid(selectorIndex))
        assertFalse(stateWithoutProject.isAllValid())

        val sampleDirectory = TestFixtures.deploy("oto", newTempDir(), wavFiles = listOf("_a_ka.wav"))
        val project = createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = sampleDirectory,
            inputFilePath = sampleDirectory.resolve("oto.ini").absolutePath,
        )
        val stateWithProject = createState(plugin, project = project)
        assertTrue(stateWithProject.isValid(selectorIndex))
        assertTrue(stateWithProject.isAllValid())
    }

    /* endregion */

    /* region presets */

    @Test
    fun `template plugins only offer memory and file preset targets`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        val importable = state.getImportablePresets(AppRecord())
        assertEquals(2, importable.size)
        val memory = importable.first() as BasePluginPresetItem.Memory
        assertEquals(plugin.name, memory.pluginName)
        assertNull(memory.slot)
        assertTrue(memory.isCurrent)
        assertTrue(memory.available)
        assertEquals(BasePluginPresetItem.File, importable.last())
        assertEquals(2, state.getExportablePresets(AppRecord()).size)
    }

    @Test
    fun `macro plugin preset availability follows the quick launch slots`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val other = macroPlugin("batch-remove-entry")
        val record = AppRecord(
            pluginQuickLaunchSlots = mapOf(
                0 to PluginQuickLaunch(pluginName = plugin.name, params = null),
                1 to PluginQuickLaunch(pluginName = other.name, params = null),
            ),
        )
        val state = createState(plugin)

        val importable = state.getImportablePresets(record)
        assertEquals(PluginQuickLaunch.SLOT_COUNT + 2, importable.size)
        val root = importable.first() as BasePluginPresetItem.Memory
        assertEquals(plugin.name, root.pluginName)
        assertNull(root.slot)
        assertTrue(root.isCurrent)
        assertTrue(root.available)
        assertEquals(BasePluginPresetItem.File, importable.last())
        val slots = importable.drop(1).dropLast(1).map { it as BasePluginPresetItem.Memory }
        assertEquals(List(PluginQuickLaunch.SLOT_COUNT) { it }, slots.map { it.slot })
        // slot 0 holds this plugin, slot 1 holds another plugin, the other slots are empty
        assertEquals(plugin.name, slots[0].pluginName)
        assertTrue(slots[0].available)
        assertEquals(other.name, slots[1].pluginName)
        assertFalse(slots[1].available)
        assertNull(slots[2].pluginName)
        assertFalse(slots[2].available)

        val exportableSlots = state.getExportablePresets(record)
            .drop(1).dropLast(1).map { it as BasePluginPresetItem.Memory }
        assertTrue(exportableSlots[0].available)
        assertFalse(exportableSlots[1].available)
        // empty slots can be exported to
        assertTrue(exportableSlots[2].available)
    }

    @Test
    fun `a dialog opened from a slot marks it as current`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val state = createState(plugin, slot = 3)
        val importable = state.getImportablePresets(AppRecord())
        val root = importable.first() as BasePluginPresetItem.Memory
        assertFalse(root.isCurrent)
        val slots = importable.drop(1).dropLast(1).map { it as BasePluginPresetItem.Memory }
        assertTrue(slots[3].isCurrent)
        assertTrue(slots[3].available)
        assertFalse(slots[2].isCurrent)
    }

    /* endregion */

    /* region import and export */

    @Test
    fun `export to file and import back round trips`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val state = createState(plugin)
        state.update(state.indexOf("bpm"), 90f)
        val file = newTempDir().resolve("preset.json")

        val exportMessage = runDialogAction(state) { state.export(BasePluginPresetTarget.File(file)) }
        assertEquals(stringStatic(Strings.PluginDialogExportSuccess), exportMessage)

        val preset = file.readText().parseJson<BasePluginPreset>()
        assertEquals(plugin.name, preset.pluginName)
        assertEquals(plugin.version, preset.pluginVersion)
        val params = assertNotNull(preset.params)
        assertEquals(90f, params.get("bpm")?.value)
        // default values are not exported
        assertNull(params.get("offset"))

        val callbacks = Callbacks()
        val freshState = createState(plugin, callbacks)
        val importMessage = runDialogAction(freshState) { freshState.import(BasePluginPresetTarget.File(file)) }
        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), importMessage)
        assertEquals(
            listOf((plugin.getDefaultParams() + mapOf("bpm" to 90f)).toParamMap()),
            callbacks.loaded,
        )
    }

    @Test
    fun `import fails on plugin name mismatch`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val file = newTempDir().resolve("preset.json")
        file.writeText(
            BasePluginPreset(pluginName = "some-other-plugin", pluginVersion = 1, params = null).stringifyJson(),
        )
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks)

        val message = runDialogAction(state) { state.import(BasePluginPresetTarget.File(file)) }

        assertEquals(stringStatic(Strings.PluginDialogImportFailure), message)
        assertEquals(emptyList(), callbacks.loaded)
    }

    @Test
    fun `import fails on an unparseable file`() {
        val plugin = templatePlugin("cvvc-oto-gen")
        val file = newTempDir().resolve("preset.json").apply { writeText("not a preset") }
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks)

        val message = runDialogAction(state) { state.import(BasePluginPresetTarget.File(file)) }

        assertEquals(stringStatic(Strings.PluginDialogImportFailure), message)
        assertEquals(emptyList(), callbacks.loaded)
    }

    @Test
    fun `export to memory writes the saved params file and import reads it back`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val savedParamsFile = plugin.getSavedParamsFile().also { recordFiles += it }
        savedParamsFile.delete()
        val state = createState(plugin)
        state.update(state.indexOf("from"), "^abc$")

        val exportMessage = runDialogAction(state) { state.export(memoryTarget(plugin.name, slot = null)) }
        assertEquals(stringStatic(Strings.PluginDialogExportSuccess), exportMessage)
        assertTrue(savedParamsFile.exists())

        val callbacks = Callbacks()
        val freshState = createState(plugin, callbacks)
        val importMessage = runDialogAction(freshState) { freshState.import(memoryTarget(plugin.name, slot = null)) }
        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), importMessage)
        assertEquals(
            listOf((plugin.getDefaultParams() + mapOf("from" to "^abc$")).toParamMap()),
            callbacks.loaded,
        )
    }

    @Test
    fun `import from memory without a saved file loads the defaults`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        plugin.getSavedParamsFile().delete()
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks)

        val message = runDialogAction(state) { state.import(memoryTarget(plugin.name, slot = null)) }

        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), message)
        assertEquals(listOf(plugin.getDefaultParams()), callbacks.loaded)
    }

    @Test
    fun `export to a quick launch slot saves it in the app record`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val store = AppRecordStore(AppRecord(), storeScope)
        val state = createState(plugin, store = store)
        state.update(state.indexOf("from"), "slot-value")

        val exportMessage = runDialogAction(state) { state.export(memoryTarget(plugin.name, slot = 2)) }
        assertEquals(stringStatic(Strings.PluginDialogExportSuccess), exportMessage)

        awaitRecord(store) { it.pluginQuickLaunchSlots[2]?.pluginName == plugin.name }
        val quickLaunch = assertNotNull(store.value.pluginQuickLaunchSlots[2])
        assertEquals("slot-value", quickLaunch.params?.get("from")?.value)
        assertFalse(quickLaunch.skipDialog)

        val callbacks = Callbacks()
        val freshState = createState(plugin, callbacks, store = store)
        val importMessage = runDialogAction(freshState) { freshState.import(memoryTarget(plugin.name, slot = 2)) }
        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), importMessage)
        assertEquals(
            listOf((plugin.getDefaultParams() + mapOf("from" to "slot-value")).toParamMap()),
            callbacks.loaded,
        )
    }

    @Test
    fun `import from a quick launch slot loads the saved params`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val slotParams = (plugin.getDefaultParams() + mapOf("from" to "xyz")).toParamMap()
        val record = AppRecord(
            pluginQuickLaunchSlots = mapOf(
                0 to PluginQuickLaunch(
                    pluginName = plugin.name,
                    params = ParamTypedMap.from(slotParams, plugin.parameterDefs),
                ),
            ),
        )
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks, store = AppRecordStore(record, cancelledScope()))

        val message = runDialogAction(state) { state.import(memoryTarget(plugin.name, slot = 0)) }

        assertEquals(stringStatic(Strings.PluginDialogImportSuccess), message)
        assertEquals(listOf(slotParams), callbacks.loaded)
    }

    @Test
    fun `import from a quick launch slot of another plugin fails`() {
        val plugin = macroPlugin("batch-edit-entry-name")
        val record = AppRecord(
            pluginQuickLaunchSlots = mapOf(
                0 to PluginQuickLaunch(pluginName = "someone-else", params = null),
            ),
        )
        val callbacks = Callbacks()
        val state = createState(plugin, callbacks, store = AppRecordStore(record, cancelledScope()))

        val message = runDialogAction(state) { state.import(memoryTarget(plugin.name, slot = 0)) }

        assertEquals(stringStatic(Strings.PluginDialogImportFailure), message)
        assertEquals(emptyList(), callbacks.loaded)
    }

    /* endregion */

    companion object {

        private val macroPlugins: List<Plugin> by lazy {
            TestEnv.ensureLogDirectory()
            loadPlugins(Plugin.Type.Macro, Language.English)
        }

        private val templatePlugins: List<Plugin> by lazy {
            TestEnv.ensureLogDirectory()
            loadPlugins(Plugin.Type.Template, Language.English)
        }

        private fun macroPlugin(name: String): Plugin = macroPlugins.first { it.name == name }

        private fun templatePlugin(name: String): Plugin = templatePlugins.first { it.name == name }
    }
}
