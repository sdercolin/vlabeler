package plugins

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "execute-scripts" (module scope) and "execute-scripts-multi-module" (project
 * scope) macro plugins.
 */
class MacroPluginExecuteScriptsTest : MacroPluginTestBase() {

    @Test
    fun testExecuteScriptsOnCurrentModule() {
        val plugin = loadMacroPlugin("execute-scripts")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "scripts" to """
                entries.forEach((entry, index) => { entry.name = entry.name + "_" + index })
                currentEntryIndex = 2
            """.trimIndent(),
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("- a_0", "a ka_1", "- i_2", "i ki_3"),
            result.currentModule.entries.map { it.name },
        )
        assertEquals(2, result.currentModule.currentIndex)
        // the other module is untouched
        assertEquals(
            listOf("- aA3", "a kaA3"),
            result.modules.first { it.name == "A3" }.entries.map { it.name },
        )
    }

    @Test
    fun testExecuteScriptsOnAllModules() {
        val plugin = loadMacroPlugin("execute-scripts-multi-module")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "scripts" to """
                modules.forEach((module) => {
                    module.entries.forEach((entry) => { entry.notes.tag = module.name })
                })
            """.trimIndent(),
        )

        val result = runMacro(plugin, params, project)

        result.modules.forEach { module ->
            module.entries.forEach { entry ->
                assertEquals(module.name, entry.notes.tag)
            }
        }
        // names and counts are unchanged
        assertEquals(
            listOf("- a", "a ka", "- i", "i ki"),
            result.modules.first { it.name == "C4" }.entries.map { it.name },
        )
    }

    @Test
    fun testExecuteScriptsCanSwitchCurrentModule() {
        val plugin = loadMacroPlugin("execute-scripts-multi-module")
        val project = createUtauSingerProject(currentModuleName = "C4")
        val a3Index = project.modules.indexOfFirst { it.name == "A3" }
        val params = plugin.paramsWith(
            "scripts" to "currentModuleIndex = $a3Index",
        )

        val result = runMacro(plugin, params, project)

        assertEquals("A3", result.currentModule.name)
    }
}
