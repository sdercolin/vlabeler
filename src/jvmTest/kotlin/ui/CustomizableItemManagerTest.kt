package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.CustomizableItemLoadingException
import com.sdercolin.vlabeler.io.getCustomLabelers
import com.sdercolin.vlabeler.io.install
import com.sdercolin.vlabeler.io.loadAvailableLabelerConfs
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialogState
import com.sdercolin.vlabeler.ui.dialog.customization.LabelerItem
import com.sdercolin.vlabeler.ui.dialog.customization.LabelerManagerDialogState
import com.sdercolin.vlabeler.ui.dialog.customization.MacroPluginItem
import com.sdercolin.vlabeler.ui.dialog.customization.MacroPluginManagerDialogState
import com.sdercolin.vlabeler.ui.dialog.customization.TemplatePluginItem
import com.sdercolin.vlabeler.ui.dialog.customization.TemplatePluginManagerDialogState
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultPluginDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import testutil.TestEnv
import testutil.TestLabelers
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for [CustomizableItem], [CustomizableItemManagerDialogState] and its subclasses.
 *
 * The custom item directories ([CustomLabelerDir], [CustomPluginDir]) and the app record file live under
 * `build/test-app-dir` because the test task sets the `VLABELER_APP_DIR` environment variable, so installing and
 * removing custom items never touches the real application directory. Everything installed by a test is removed in
 * [teardown].
 *
 * The dialog states only receive an [AppState] but never dereference it on the paths exercised here (it is only
 * touched by `reload`/error/finish paths, see the notes on the individual tests), so an uninitialized instance
 * allocated without running the constructor is passed, following the pattern proven safe in [ProjectCreatorStateTest].
 * `importNewItem` is invoked directly (it is protected, so via reflection or a test subclass) instead of through
 * `handleFileSelectorResult`, because the latter always ends in `reload()`, which needs a real [AppState].
 */
class CustomizableItemManagerTest {

    private lateinit var storeScope: CoroutineScope
    private val tempDirs = mutableListOf<File>()
    private val installedFiles = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        storeScope.cancel()
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        installedFiles.forEach { it.deleteRecursively() }
        installedFiles.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    private fun recordStore(record: AppRecord = AppRecord(), live: Boolean = false): AppRecordStore =
        AppRecordStore(record, if (live) storeScope else cancelledScope())

    private fun awaitRecord(store: AppRecordStore, predicate: (AppRecord) -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (predicate(store.value)) return
            Thread.sleep(10)
        }
        fail("The app record was not updated in time")
    }

    /**
     * Invokes the protected `importNewItem` of [state] directly. The implementations have no suspension points, so
     * the reflective call returns the result synchronously.
     */
    private fun importItem(state: CustomizableItemManagerDialogState<*>, configFile: File): String {
        val method = generateSequence<Class<*>>(state.javaClass) { it.superclass }
            .mapNotNull { cls ->
                cls.declaredMethods.firstOrNull { it.name == "importNewItem" && it.parameterCount == 2 }
            }
            .first()
        method.isAccessible = true
        val continuation = object : Continuation<Any?> {
            override val context: CoroutineContext = EmptyCoroutineContext
            override fun resumeWith(result: Result<Any?>) = Unit
        }
        val result = try {
            method.invoke(state, configFile, continuation)
        } catch (e: InvocationTargetException) {
            throw requireNotNull(e.targetException)
        }
        check(result !== COROUTINE_SUSPENDED) { "importNewItem unexpectedly suspended" }
        return result as String
    }

    /* region test doubles */

    private class TestItem(
        name: String,
        rootFile: File,
        canRemove: Boolean = true,
        disabled: Boolean = false,
        email: String = "",
        website: String = "",
        private val executable: Boolean = false,
    ) : CustomizableItem(
        name = name,
        author = "author",
        version = 1,
        displayedName = name.toLocalized(),
        description = "".toLocalized(),
        email = email,
        website = website,
        rootFile = rootFile,
        canRemove = canRemove,
        disabled = disabled,
    ) {
        var executed: Int = 0

        override fun canExecute(): Boolean = executable

        override fun execute() {
            executed++
        }
    }

    private class TestManagerState(
        appState: AppState,
        appRecordStore: AppRecordStore,
        directory: File,
        allowExecution: Boolean = true,
    ) : CustomizableItemManagerDialogState<TestItem>(
        title = Strings.LabelerManagerTitle,
        importDialogTitle = Strings.LabelerManagerImportDialogTitle,
        definitionFileExtension = "json",
        directory = directory,
        allowExecution = allowExecution,
        appState = appState,
        appRecordStore = appRecordStore,
    ) {
        var reloadCount: Int = 0
        val savedDisabled = mutableListOf<Pair<String, Boolean>>()
        val imported = mutableListOf<File>()

        override fun reload() {
            reloadCount++
        }

        override fun saveDisabled(index: Int) {
            savedDisabled += items[index].name to items[index].disabled
        }

        override suspend fun importNewItem(configFile: File): String {
            imported += configFile
            return configFile.nameWithoutExtension
        }
    }

    private fun createTestManager(allowExecution: Boolean = true): TestManagerState = TestManagerState(
        appState = uninitializedAppState(),
        appRecordStore = recordStore(),
        directory = newTempDir(),
        allowExecution = allowExecution,
    )

    private fun testItem(
        name: String,
        canRemove: Boolean = true,
        disabled: Boolean = false,
        executable: Boolean = false,
    ): TestItem = TestItem(
        name = name,
        rootFile = newTempDir().resolve("$name.json").apply { writeText("{}") },
        canRemove = canRemove,
        disabled = disabled,
        executable = executable,
    )

    /* endregion */

    /* region CustomizableItem */

    @Test
    fun `customizable item exposes email and website availability`() {
        val plain = testItem("plain")
        assertFalse(plain.hasEmail())
        assertFalse(plain.hasWebsite())
        assertFalse(plain.canExecute())

        val rich = TestItem(
            name = "rich",
            rootFile = newTempDir().resolve("rich.json").apply { writeText("{}") },
            email = "someone@example.com",
            website = "https://example.com",
        )
        assertTrue(rich.hasEmail())
        assertTrue(rich.hasWebsite())
    }

    @Test
    fun `toggle disabled flips the state`() {
        val item = testItem("item")
        assertFalse(item.disabled)
        item.toggleDisabled()
        assertTrue(item.disabled)
        item.toggleDisabled()
        assertFalse(item.disabled)
    }

    @Test
    fun `remove deletes the root file recursively`() {
        val root = newTempDir().resolve("item").apply { mkdirs() }
        root.resolve("content.txt").writeText("content")
        val item = TestItem(name = "item", rootFile = root)

        assertTrue(item.remove())

        assertFalse(root.exists())
    }

    /* endregion */

    /* region CustomizableItemManagerDialogState */

    @Test
    fun `load items keeps the order on first load and moves new items to the bottom`() {
        val state = createTestManager()
        val a = testItem("a")
        val b = testItem("b")
        val c = testItem("c")
        state.loadItems(listOf(a, b, c))
        assertEquals(listOf("a", "b", "c"), state.items.map { it.name })
        assertNull(state.selectedIndex)

        val new = testItem("new")
        state.loadItems(listOf(a, new, b, c))

        assertEquals(listOf("a", "b", "c", "new"), state.items.map { it.name })
        assertEquals(3, state.selectedIndex)
        assertEquals(new, state.selectedItem)
    }

    @Test
    fun `select and cancel selection`() {
        val state = createTestManager()
        state.loadItems(listOf(testItem("a"), testItem("b", canRemove = false)))
        assertNull(state.selectedItem)
        assertFalse(state.canRemoveCurrentItem())

        state.selectItem(0)
        assertEquals("a", state.selectedItem?.name)
        assertTrue(state.canRemoveCurrentItem())

        state.selectItem(1)
        assertEquals("b", state.selectedItem?.name)
        assertFalse(state.canRemoveCurrentItem())

        state.cancelSelection()
        assertNull(state.selectedIndex)
        assertNull(state.selectedItem)
    }

    @Test
    fun `toggle item disabled flips the item and saves it`() {
        val state = createTestManager()
        state.loadItems(listOf(testItem("a"), testItem("b")))

        state.toggleItemDisabled(1)
        assertTrue(state.items[1].disabled)
        state.toggleItemDisabled(1)
        assertFalse(state.items[1].disabled)

        assertEquals(listOf("b" to true, "b" to false), state.savedDisabled)
    }

    @Test
    fun `remove item deletes its files clears the selection and reloads`() {
        val state = createTestManager()
        val item = testItem("a")
        state.loadItems(listOf(item))
        state.selectItem(0)

        state.removeItem(item)

        assertFalse(item.rootFile.exists())
        assertNull(state.selectedIndex)
        assertEquals(1, state.reloadCount)
    }

    @Test
    fun `remove item requires a removable item in the list`() {
        val state = createTestManager()
        val fixed = testItem("fixed", canRemove = false)
        state.loadItems(listOf(fixed))
        assertFailsWith<IllegalArgumentException> { state.removeItem(fixed) }

        val notInList = testItem("other")
        assertFailsWith<IllegalArgumentException> { state.removeItem(notInList) }
    }

    @Test
    fun `execution is gated by the dialog and the item`() {
        val state = createTestManager(allowExecution = true)
        val executable = testItem("executable", executable = true)
        state.loadItems(listOf(executable, testItem("plain")))
        assertFalse(state.canExecuteSelectedItem())

        state.selectItem(0)
        assertTrue(state.canExecuteSelectedItem())
        state.executeSelectedItem()
        assertEquals(1, executable.executed)

        state.selectItem(1)
        assertFalse(state.canExecuteSelectedItem())
        assertFailsWith<IllegalArgumentException> { state.executeSelectedItem() }

        val noExecutionState = createTestManager(allowExecution = false)
        val item = testItem("executable", executable = true)
        noExecutionState.loadItems(listOf(item))
        noExecutionState.selectItem(0)
        assertFalse(noExecutionState.canExecuteSelectedItem())
    }

    @Test
    fun `file selector flow imports the selected file and reloads`() {
        val state = createTestManager()
        assertFalse(state.isShowingFileSelector)

        state.openFileSelectorForNewItem()
        assertTrue(state.isShowingFileSelector)

        runBlocking { state.handleFileSelectorResult(null) }
        assertFalse(state.isShowingFileSelector)
        assertEquals(emptyList(), state.imported)
        assertEquals(0, state.reloadCount)

        val file = newTempDir().resolve("new-item.json").apply { writeText("{}") }
        state.openFileSelectorForNewItem()
        runBlocking { state.handleFileSelectorResult(file) }
        assertFalse(state.isShowingFileSelector)
        assertEquals(listOf(file), state.imported)
        assertEquals(1, state.reloadCount)
    }

    /* endregion */

    /* region LabelerManagerDialogState */

    @Test
    fun `labeler manager configuration`() {
        val state = LabelerManagerDialogState(uninitializedAppState(), recordStore())
        assertEquals(Strings.LabelerManagerTitle, state.title)
        assertEquals(Strings.LabelerManagerImportDialogTitle, state.importDialogTitle)
        assertEquals("labeler.json", state.definitionFileExtension)
        assertEquals(CustomLabelerDir, state.directory)
        assertFalse(state.allowExecution)
    }

    @Test
    fun `labeler items map the labeler conf`() {
        val labeler = TestLabelers.utauOto
        val item = LabelerItem(labeler, disabled = false)
        assertEquals(labeler.name, item.name)
        assertEquals(labeler.author, item.author)
        assertEquals(labeler.version, item.version)
        assertEquals(labeler.rootFile, item.rootFile)
        // built-in labelers cannot be removed
        assertFalse(item.canRemove)
        assertFalse(item.canExecute())
    }

    @Test
    fun `import installs a custom labeler and remove uninstalls it`() {
        val name = "test-custom-oto"
        val renamed = TestLabelers.utauOto.copy(name = name)
        val stagedFile = renamed.install(newTempDir()).getOrThrow()
        val installTarget = CustomLabelerDir.resolve(stagedFile.parentFile.name).also { installedFiles += it }
        CustomLabelerDir.mkdirs()
        val state = LabelerManagerDialogState(uninitializedAppState(), recordStore())

        val importedName = importItem(state, stagedFile)

        assertEquals(name, importedName)
        assertTrue(installTarget.resolve("labeler.json").isFile)

        // the labeler is listed among the available labelers as a custom (removable) one
        val available = runBlocking { loadAvailableLabelerConfs() }
        val installed = available.first { it.name == name }
        assertFalse(installed.builtIn)
        val item = LabelerItem(installed, disabled = false)
        assertTrue(item.canRemove)
        assertEquals(installTarget, item.rootFile)

        // removing the item deletes it from the custom directory
        assertTrue(item.remove())
        assertFalse(installTarget.exists())
        assertTrue(getCustomLabelers().none { it.parentFile == installTarget })
        assertTrue(runBlocking { loadAvailableLabelerConfs() }.none { it.name == name })
    }

    @Test
    fun `labeler import fails with a loading exception on an invalid file`() {
        val state = LabelerManagerDialogState(uninitializedAppState(), recordStore())
        val invalidFile = newTempDir().resolve("invalid.labeler.json").apply { writeText("not a labeler") }

        assertFailsWith<CustomizableItemLoadingException> { importItem(state, invalidFile) }
    }

    @Test
    fun `toggling a labeler saves its disabled state to the app record`() {
        val store = recordStore(live = true)
        val state = LabelerManagerDialogState(uninitializedAppState(), store)
        val labeler = TestLabelers.utauOto
        state.loadItems(listOf(LabelerItem(labeler, disabled = false)))

        state.toggleItemDisabled(0)
        assertTrue(state.items[0].disabled)
        awaitRecord(store) { labeler.name in it.disabledLabelerNames }

        state.toggleItemDisabled(0)
        assertFalse(state.items[0].disabled)
        awaitRecord(store) { labeler.name !in it.disabledLabelerNames }
    }

    /* endregion */

    /* region PluginManagerDialogState */

    @Test
    fun `plugin manager configurations`() {
        val macroState = MacroPluginManagerDialogState(uninitializedAppState(), recordStore())
        assertEquals(Strings.MacroPluginManagerTitle, macroState.title)
        assertEquals(Strings.MacroPluginManagerImportDialogTitle, macroState.importDialogTitle)
        assertEquals("json", macroState.definitionFileExtension)
        assertEquals(CustomPluginDir.resolve("macro"), macroState.directory)
        assertTrue(macroState.allowExecution)

        val templateState = TemplatePluginManagerDialogState(uninitializedAppState(), recordStore())
        assertEquals(Strings.TemplatePluginManagerTitle, templateState.title)
        assertEquals(Strings.TemplatePluginManagerImportDialogTitle, templateState.importDialogTitle)
        assertEquals(CustomPluginDir.resolve("template"), templateState.directory)
        assertFalse(templateState.allowExecution)
    }

    @Test
    fun `plugin items map the plugin`() {
        val macroPlugin = loadPlugins(Plugin.Type.Macro, Language.English).first { it.builtIn }
        val macroItem = MacroPluginItem(macroPlugin, uninitializedAppState(), disabled = false)
        assertEquals(macroPlugin.name, macroItem.name)
        assertEquals(macroPlugin.directory, macroItem.rootFile)
        assertFalse(macroItem.canRemove)

        val templatePlugin = loadPlugins(Plugin.Type.Template, Language.English).first { it.builtIn }
        val templateItem = TemplatePluginItem(templatePlugin, disabled = true)
        assertEquals(templatePlugin.name, templateItem.name)
        assertEquals(templatePlugin.directory, templateItem.rootFile)
        assertFalse(templateItem.canRemove)
        assertTrue(templateItem.disabled)
        assertFalse(templateItem.canExecute())
    }

    @Test
    fun `import installs a custom macro plugin and remove uninstalls it`() {
        val name = "test-custom-macro"
        val stagedConfig = stageRenamedPlugin("batch-remove-entry", Plugin.Type.Macro, name)
        val installTarget = CustomPluginDir.resolve("macro").resolve(name).also { installedFiles += it }
        val state = MacroPluginManagerDialogState(uninitializedAppState(), recordStore())

        val importedName = importItem(state, stagedConfig)

        assertEquals(name, importedName)
        assertTrue(installTarget.resolve("plugin.json").isFile)

        // the plugin is listed as a custom (removable) one
        val installed = loadPlugins(Plugin.Type.Macro, Language.English).first { it.name == name }
        assertFalse(installed.builtIn)
        val item = MacroPluginItem(installed, uninitializedAppState(), disabled = false)
        assertTrue(item.canRemove)
        assertEquals(installTarget, item.rootFile)

        assertTrue(item.remove())
        assertFalse(installTarget.exists())
        assertTrue(loadPlugins(Plugin.Type.Macro, Language.English).none { it.name == name })
    }

    @Test
    fun `plugin import rejects a plugin of the wrong type`() {
        val templateConfig = DefaultPluginDir.resolve("template").resolve("cv-oto-gen").resolve("plugin.json")
        assertTrue(templateConfig.isFile)
        val state = MacroPluginManagerDialogState(uninitializedAppState(), recordStore())

        assertFailsWith<CustomizableItemLoadingException> { importItem(state, templateConfig) }

        assertFalse(CustomPluginDir.resolve("macro").resolve("cv-oto-gen").exists())
    }

    @Test
    fun `plugin import fails with a loading exception on an invalid file`() {
        val state = TemplatePluginManagerDialogState(uninitializedAppState(), recordStore())
        val invalidFile = newTempDir().resolve("plugin.json").apply { writeText("not a plugin") }

        assertFailsWith<CustomizableItemLoadingException> { importItem(state, invalidFile) }
    }

    @Test
    fun `toggling a plugin saves its disabled state to the app record`() {
        val store = recordStore(live = true)
        val state = TemplatePluginManagerDialogState(uninitializedAppState(), store)
        val plugin = loadPlugins(Plugin.Type.Template, Language.English).first { it.builtIn }
        state.loadItems(listOf(TemplatePluginItem(plugin, disabled = false)))

        state.toggleItemDisabled(0)
        assertTrue(state.items[0].disabled)
        awaitRecord(store) { plugin.name in it.disabledPluginNames }

        state.toggleItemDisabled(0)
        assertFalse(state.items[0].disabled)
        awaitRecord(store) { plugin.name !in it.disabledPluginNames }
    }

    /* endregion */

    /**
     * Copies the bundled plugin [sourceName] of [type] to a temporary directory and renames it to [newName], so that
     * installing it does not shadow the bundled plugin.
     */
    private fun stageRenamedPlugin(sourceName: String, type: Plugin.Type, newName: String): File {
        val source = DefaultPluginDir.resolve(type.directoryName).resolve(sourceName)
        val target = newTempDir().resolve(newName)
        source.copyRecursively(target)
        val configFile = target.resolve("plugin.json")
        val jsonObject = Json.parseToJsonElement(configFile.readText()).jsonObject
        val edited = JsonObject(jsonObject.toMutableMap().apply { put("name", JsonPrimitive(newName)) })
        configFile.writeText(edited.toString())
        return configFile
    }
}
