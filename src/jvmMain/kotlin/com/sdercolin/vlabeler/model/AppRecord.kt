package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Version
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.ui.starter.ProjectCreatorState
import com.sdercolin.vlabeler.util.DefaultDownloadDir
import com.sdercolin.vlabeler.util.asPathRelativeToHome
import com.sdercolin.vlabeler.util.asSimplifiedPaths
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A serializable data class containing implicit user environment settings. Compared to [AppConf], this class contains
 * settings that may frequently change.
 */
@Immutable
@Serializable
data class AppRecord(
    val recentProjects: List<String> = listOf(),
    val windowSizeDp: Pair<Float, Float> = Pair(1200f, 850f),
    val pluginDialogSizeDp: Pair<Float, Float> = Pair(900f, 700f),
    val isPropertyViewDisplayed: Boolean = false,
    val isEntryListPinned: Boolean = true,
    val pinnedEntryListSplitPanePositionLocked: Boolean = false,
    val pinnedEntryListSplitPanePositions: Map<AppConf.ViewSidePosition, Float> = mapOf(
        AppConf.ViewSidePosition.Left to 0.3f,
        AppConf.ViewSidePosition.Right to 0.7f,
        AppConf.ViewSidePosition.Top to 0.3f,
        AppConf.ViewSidePosition.Bottom to 0.7f,
    ),
    val isToolboxDisplayed: Boolean = false,
    val sampleDirectory: String? = null,
    val workingDirectory: String? = null,
    val labelerCategory: String? = null,
    val labelerName: String? = null,
    val projectContentType: ProjectCreatorState.ContentType? = null,
    val projectCreatorDetailsExpanded: List<Boolean> = listOf(false, false, false),
    val disabledLabelerNames: Set<String> = setOf(),
    val disabledPluginNames: Set<String> = setOf(),
    val autoExport: Boolean = false,
    val ignoredUpdateVersions: Set<String> = setOf(),
    val updateDownloadDirectory: String = DefaultDownloadDir.absolutePath,
    val hasSavedDetectedLanguage: Boolean = false,
    val pluginQuickLaunchSlots: Map<Int, PluginQuickLaunch> = mapOf(),
    val hasAskedForTrackingPermission: Boolean = false,
    val trackingId: String? = null,
    val showDisabledMacroPluginItems: Boolean = false,
    val isTimescaleBarDisplayed: Boolean = false,
    val includeInfoLog: Boolean = false,
    val appVersionLastLaunchedRaw: String? = null,
    val hasCheckedRosettaCompatibleMode: Boolean = false,
    val recentQuickProjectBuilderNames: List<Pair<String, String>> = listOf(),
) {
    val recentProjectPathsWithDisplayNames
        get() = recentProjects.zip(
            recentProjects.map { it.asPathRelativeToHome() }.asSimplifiedPaths(),
        )

    fun addRecent(path: String) = copy(
        recentProjects = (listOf(path) + recentProjects).distinct().take(MAX_RECENT_PROJECT_COUNT),
    )

    fun setLabelerDisabled(name: String, disabled: Boolean) = copy(
        disabledLabelerNames = if (disabled) disabledLabelerNames + name else disabledLabelerNames - name,
    )

    fun setPluginDisabled(name: String, disabled: Boolean) = copy(
        disabledPluginNames = if (disabled) disabledPluginNames + name else disabledPluginNames - name,
    )

    fun setPinnedEntryListSplitPanePosition(position: AppConf.ViewSidePosition, positionPercentage: Float) = copy(
        pinnedEntryListSplitPanePositions = pinnedEntryListSplitPanePositions.toMutableMap().apply {
            this[position] = positionPercentage
        }.toMap(),
    )

    fun getPinnedEntryListSplitPanePosition(position: AppConf.ViewSidePosition) =
        pinnedEntryListSplitPanePositions[position] ?: 0.5f

    fun isUpdateIgnored(version: Version) = ignoredUpdateVersions.contains(version.toString())

    fun versionIgnored(version: Version) = copy(
        ignoredUpdateVersions = ignoredUpdateVersions + version.toString(),
    )

    fun getPluginQuickLaunch(slot: Int) = pluginQuickLaunchSlots[slot]

    fun getUsedPluginQuickLaunchSlots() =
        (0 until PluginQuickLaunch.SLOT_COUNT).filter { pluginQuickLaunchSlots.containsKey(it) }

    fun saveQuickLaunch(slot: Int, pluginQuickLaunch: PluginQuickLaunch?) =
        copy(
            pluginQuickLaunchSlots = pluginQuickLaunchSlots.toMutableMap().apply {
                if (pluginQuickLaunch != null) {
                    this[slot] = pluginQuickLaunch
                } else {
                    this.remove(slot)
                }
            }.toMap(),
        )

    fun clearTrackingId() = copy(trackingId = null)
    fun generateTrackingId() = copy(trackingId = UUID.randomUUID().toString())

    fun toggleProjectCreatorDetailsExpanded(index: Int) = copy(
        projectCreatorDetailsExpanded = projectCreatorDetailsExpanded.toMutableList().apply {
            val currentValue = this.getOrNull(index) ?: return@apply
            this[index] = !currentValue
        },
    )

    fun addRecentQuickProjectBuilder(labelerName: String, name: String) = copy(
        recentQuickProjectBuilderNames = (listOf(labelerName to name) + recentQuickProjectBuilderNames)
            .distinct(),
    )

    val appVersionLastLaunched: Version get() = appVersionLastLaunchedRaw?.let { Version.from(it) } ?: Version.zero

    fun onAppVersionLaunched() = copy(appVersionLastLaunchedRaw = appVersion.toString())
}

private const val MAX_RECENT_PROJECT_COUNT = 10
