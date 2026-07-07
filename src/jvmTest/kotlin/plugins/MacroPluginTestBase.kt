package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.MacroPluginExecutionListener
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.runMacroPlugin
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toParamMap
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for integration tests of the bundled macro plugins under `resources/common/plugins/macro`.
 *
 * The plugins are loaded through the real plugin loading flow ([loadPlugins]) and executed through the real macro
 * execution path ([runMacroPlugin]) against a project created by the real project creation flow.
 */
abstract class MacroPluginTestBase {

    protected lateinit var tempDir: File
    protected val reports = mutableListOf<LocalizedJsonString>()

    @BeforeTest
    fun setUpBase() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
        reports.clear()
    }

    @AfterTest
    fun tearDownBase() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    protected fun loadMacroPlugin(name: String): Plugin =
        requireNotNull(allMacroPlugins.find { it.name == name }) { "Macro plugin not found: $name" }

    /**
     * Creates a two-module ("A3", "C4") project from the `utau-singer` fixture with the bundled utau-singer-labeler.
     *
     * The "C4" module has entries "- a", "a ka", "- i", "i ki"; the "A3" module has "- aA3", "a kaA3".
     * The current module is set to [currentModuleName] so that module-scoped plugins run deterministically.
     */
    protected fun createUtauSingerProject(currentModuleName: String = "C4"): Project {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        return project.copy(currentModuleIndex = project.modules.indexOfFirst { it.name == currentModuleName })
    }

    protected fun runMacro(plugin: Plugin, params: ParamMap, project: Project): Project = runMacroPlugin(
        plugin = plugin,
        params = params,
        project = project,
        listener = MacroPluginExecutionListener(
            onAudioPlaybackRequest = {},
            onReport = { reports.add(it) },
        ),
    )

    protected fun Plugin.paramsWith(vararg overrides: Pair<String, Any>): ParamMap =
        (getDefaultParams() + overrides).toParamMap()

    protected fun selectorOf(vararg filters: EntrySelector.FilterItem) = EntrySelector(filters.toList())

    protected fun nameFilter(matchType: EntrySelector.TextMatchType, text: String) = EntrySelector.TextFilterItem(
        subject = "name",
        matchType = matchType,
        matcherText = text,
    )

    protected val LocalizedJsonString.en: String
        get() = getCertain(Language.English)

    companion object {
        private val allMacroPlugins: List<Plugin> by lazy {
            loadPlugins(Plugin.Type.Macro, Language.English)
        }
    }
}
