package plugins

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "sort-entries" macro plugin.
 *
 * The "C4" module contains, in raw label file order:
 * "- a" (sample _a_ka, start 10), "a ka" (_a_ka, start 450), "- i" (_i_ki, start 10), "i ki" (_i_ki, start 430).
 */
class MacroPluginSortEntriesTest : MacroPluginTestBase() {

    @Test
    fun testSortBySampleAndStartAscending() {
        val plugin = loadMacroPlugin("sort-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "descending" to false,
            "priority" to "sampleStart",
        )

        val result = runMacro(plugin, params, project)

        // the fixture is already ordered by sample name and start time
        assertEquals(
            listOf("- a", "a ka", "- i", "i ki"),
            result.currentModule.entries.map { it.name },
        )
    }

    @Test
    fun testSortBySampleAndStartDescending() {
        val plugin = loadMacroPlugin("sort-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "descending" to true,
            "priority" to "sampleStart",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("i ki", "- i", "a ka", "- a"),
            result.currentModule.entries.map { it.name },
        )
    }

    @Test
    fun testSortWithPrioritizedTag() {
        val plugin = loadMacroPlugin("sort-entries")
        val tagByName = mapOf("- a" to "b", "a ka" to "b", "- i" to "a", "i ki" to "a")
        val project = createUtauSingerProject().updateCurrentModule {
            copy(entries = entries.map { it.tagEdited(tagByName.getValue(it.name)) })
        }
        val params = plugin.paramsWith(
            "descending" to false,
            "useTag" to true,
            "prioritizeTag" to true,
            "priority" to "sampleStart",
        )

        val result = runMacro(plugin, params, project)

        // tag "a" group first, then tag "b" group; within a group, by sample name and start
        assertEquals(
            listOf("- i", "i ki", "- a", "a ka"),
            result.currentModule.entries.map { it.name },
        )
    }
}
