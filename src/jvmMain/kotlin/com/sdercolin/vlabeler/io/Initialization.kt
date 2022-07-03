package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.DefaultAppConfFile
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.getCustomLabelers
import com.sdercolin.vlabeler.util.getDefaultLabelers
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun loadAppConf(): MutableState<AppConf> = withContext(Dispatchers.IO) {
    val customAppConf = if (CustomAppConfFile.exists() && !isDebug) {
        Log.info("Custom app conf found")
        val customAppConfText = CustomAppConfFile.readText()
        runCatching { parseJson<AppConf>(customAppConfText) }.getOrElse {
            Log.debug("Failed to parse custom app conf: $customAppConfText. Error message: {${it.message}}.")
            null
        }
    } else null
    val appConf = customAppConf ?: parseJson(DefaultAppConfFile.readText())
    CustomAppConfFile.writeText(toJson(appConf))
    Log.info("AppConf: $appConf")
    mutableStateOf(appConf)
}

suspend fun loadAvailableLabelerConfs(): List<LabelerConf> = withContext(Dispatchers.IO) {
    val defaultLabelers = getDefaultLabelers().associateWith {
        it.asLabelerConf().getOrThrow() // default items should always be parsed
    }.toList()
    val defaultLabelerNames = defaultLabelers.map { it.first.name }
    val customLabelers = getCustomLabelers().associateWith {
        it.asLabelerConf().getOrNull()
    }.toList()
    val validCustomLabelers = customLabelers.mapNotNull { (file, result) -> result?.let { file to it } }
    val validCustomLabelerNames = validCustomLabelers.map { it.first.name }

    val availableLabelers = mutableListOf<LabelerConf>()
    val (duplicated, new) = defaultLabelers.partition { it.first.name in validCustomLabelerNames }
    new.forEach {
        availableLabelers.add(it.second)
        it.first.copyTo(CustomLabelerDir.resolve(it.first.name))
    }
    duplicated.forEach { default ->
        val custom = validCustomLabelers.first { it.first.name == default.first.name }
        if (default.second.version > custom.second.version || isDebug) {
            // update with default
            availableLabelers.add(default.second)
            default.first.copyTo(CustomLabelerDir.resolve(custom.first.name), overwrite = true)
            Log.debug("Update ${custom.first.name} to version ${default.second.version}")
        } else {
            availableLabelers.add(custom.second)
        }
    }
    availableLabelers.addAll(
        validCustomLabelers.filter { it.first.name !in defaultLabelerNames }.map { it.second }
    )
    if (availableLabelers.isEmpty()) {
        throw IllegalStateException("No labeler configuration files found.")
    }

    Log.info("Labelers: ${availableLabelers.joinToString { it.toString() }}")
    availableLabelers.sortedBy { it.name }
}

suspend fun loadPlugins(): List<Plugin> = withContext(Dispatchers.IO) {
    loadPlugins(Plugin.Type.Template)
}

private fun File.asLabelerConf(): Result<LabelerConf> {
    val text = readText()
    val result = runCatching {
        parseJson<LabelerConf>(text).copy(
            name = name.removeSuffix(".${LabelerConf.LabelerFileExtension}")
        )
    }
    result.exceptionOrNull()?.let {
        Log.debug("Failed to parse custom app conf: $text. Error message: {${it.message}}.")
    }
    return result
}

suspend fun ensureDirectories() = withContext(Dispatchers.IO) {
    if (AppDir.exists().not()) {
        AppDir.mkdir()
        Log.info("$AppDir created")
    }
    if (CustomLabelerDir.exists().not()) {
        CustomLabelerDir.mkdir()
        Log.info("$CustomLabelerDir created")
    }
    if (RecordDir.exists().not()) {
        RecordDir.mkdir()
        Log.info("$RecordDir created")
    }
}

suspend fun produceAppState(mainScope: CoroutineScope, appRecordStore: AppRecordStore): AppState {
    val playerState = PlayerState()
    val player = Player(mainScope, playerState)
    val keyboardViewModel = KeyboardViewModel(mainScope)
    val scrollFitViewModel = ScrollFitViewModel(mainScope)
    val snackbarHostState = SnackbarHostState()
    ensureDirectories()
    Log.init()

    val appConf = loadAppConf()
    val availableLabelerConfs = loadAvailableLabelerConfs()
    val plugins = loadPlugins()

    return AppState(
        playerState,
        player,
        keyboardViewModel,
        scrollFitViewModel,
        appRecordStore,
        snackbarHostState,
        appConf,
        availableLabelerConfs,
        plugins
    )
}
