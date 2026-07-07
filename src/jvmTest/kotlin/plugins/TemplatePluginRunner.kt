package plugins

import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.ModuleDefinition
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.TemplatePluginResult
import com.sdercolin.vlabeler.model.runTemplatePlugin
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.util.toParamMap
import testutil.TestEnv
import java.io.File

/**
 * Loads the bundled template plugins from `resources/common/plugins/template` (via the real [loadPlugins] flow, which
 * also injects `file::` default parameter values) and runs them through the real [runTemplatePlugin] execution path.
 */
object TemplatePluginRunner {

    private val plugins: List<Plugin> by lazy {
        TestEnv.ensureLogDirectory()
        loadPlugins(Plugin.Type.Template, Language.English)
    }

    fun getPlugin(name: String): Plugin = requireNotNull(plugins.find { it.name == name }) {
        "Bundled template plugin `$name` was not loaded"
    }

    /**
     * Runs the bundled template plugin named [pluginName] in the same way as the project creation flow.
     *
     * @param sampleFileNames Names of the sample (wav) files in [sampleDirectory]. Template plugins only read the
     *     names, so the files do not need to exist.
     * @param paramOverrides Values overriding the plugin's default parameter values.
     */
    fun run(
        pluginName: String,
        labeler: LabelerConf,
        sampleDirectory: File,
        sampleFileNames: List<String>,
        paramOverrides: Map<String, Any> = emptyMap(),
        encoding: String = "UTF-8",
    ): TemplatePluginResult {
        TestEnv.ensureLogDirectory()
        val plugin = getPlugin(pluginName)
        val sampleFiles = sampleFileNames.map { sampleDirectory.resolve(it) }
        val moduleDefinition = ModuleDefinition(
            name = "",
            sampleDirectory = sampleDirectory,
            sampleFiles = sampleFiles,
            inputFiles = null,
            labelFile = null,
        )
        return runTemplatePlugin(
            plugin = plugin,
            params = (plugin.getDefaultParams() + paramOverrides).toParamMap(),
            encoding = encoding,
            sampleFiles = sampleFiles,
            labelerConf = labeler,
            labelerParams = labeler.getDefaultParams(),
            rootSampleDirectory = sampleDirectory.absolutePath,
            moduleDefinition = moduleDefinition,
        )
    }
}
