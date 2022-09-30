package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Locale
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultAppConfFile
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.getCustomLabelers
import com.sdercolin.vlabeler.util.getDefaultLabelers
import com.sdercolin.vlabeler.util.or
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.savedMutableStateOf
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun loadAppConf(mainScope: CoroutineScope, appRecord: AppRecordStore): MutableState<AppConf> {
    val customAppConf = if (CustomAppConfFile.exists()) {
        Log.info("Custom app conf found")
        val customAppConfText = CustomAppConfFile.readText()
        runCatching { customAppConfText.parseJson<AppConf>() }.getOrElse {
            Log.debug("Failed to parse custom app conf: $customAppConfText. Error message: {${it.message}}.")
            null
        }
    } else null
    val appConf = customAppConf
        .or(DefaultAppConfFile.readText().parseJson())
        .runIf(appRecord.value.hasSavedDetectedLanguage.not()) {
            val languageTag = Locale.toLanguageTag()
            val detectedLanguage = Language.find(languageTag)
            if (detectedLanguage != null) {
                appRecord.update { copy(hasSavedDetectedLanguage = true) }
                copy(view = view.copy(language = detectedLanguage))
            } else {
                this
            }
        }
    CustomAppConfFile.writeText(appConf.stringifyJson())
    Log.info("AppConf: $appConf")
    return savedMutableStateOf(appConf) { value ->
        mainScope.launch(Dispatchers.IO) {
            runCatching {
                CustomAppConfFile.writeText(value.stringifyJson())
            }.onFailure {
                Log.error(it)
            }
        }
    }
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
        it.first.copyTo(CustomLabelerDir.resolve(it.first.name), overwrite = true)
    }
    duplicated.forEach { default ->
        val custom = validCustomLabelers.first { it.first.name == default.first.name }
        if (default.second.version > custom.second.version || isDebug) {
            // update with default
            availableLabelers.add(default.second)
            default.first.copyTo(CustomLabelerDir.resolve(custom.first.name), overwrite = true)
            if (default.second.version > custom.second.version) {
                Log.debug("Update ${custom.first.name} to version ${default.second.version}")
            }
        } else {
            availableLabelers.add(custom.second)
        }
    }
    availableLabelers.addAll(
        validCustomLabelers.filter { it.first.name !in defaultLabelerNames }.map { it.second },
    )
    if (availableLabelers.isEmpty()) {
        throw IllegalStateException("No labeler configuration files found.")
    }
    availableLabelers.forEach {
        Log.info("Loaded labeler: ${it.name}")
    }
    availableLabelers.sortedBy { it.name }
}

suspend fun loadPlugins(language: Language): List<Plugin> = withContext(Dispatchers.IO) {
    loadPlugins(Plugin.Type.Template, language) + loadPlugins(Plugin.Type.Macro, language)
}

fun File.asLabelerConf(): Result<LabelerConf> {
    val text = readText()
    val result = runCatching {
        text.parseJson<LabelerConf>()
            .copy(name = name.removeSuffix(".${LabelerConf.LabelerFileExtension}"))
            .validate()
    }
    result.exceptionOrNull()?.let {
        Log.debug("Failed to parse labeler conf: $text. Error message: {${it.message}}.")
    }
    return result
}

fun ensureDirectories() {
    val directories = listOf(AppDir, CustomLabelerDir, CustomPluginDir, RecordDir) +
        Plugin.Type.values().map { CustomPluginDir.resolve(it.directoryName) }

    directories.forEach {
        if (it.exists().not()) {
            it.mkdir()
            Log.info("$it created")
        }
    }
}

suspend fun produceAppState(
    mainScope: CoroutineScope,
    appConf: MutableState<AppConf>,
    appRecordStore: AppRecordStore,
): AppState {
    val availableLabelerConfs = loadAvailableLabelerConfs()
    val plugins = loadPlugins(appConf.value.view.language)

    val scrollFitViewModel = ScrollFitViewModel(mainScope)
    val snackbarHostState = SnackbarHostState()
    val keyboardViewModel = KeyboardViewModel(mainScope, appConf.value.keymaps)

    return AppState(
        mainScope,
        keyboardViewModel,
        scrollFitViewModel,
        appRecordStore,
        snackbarHostState,
        appConf,
        availableLabelerConfs,
        plugins,
    )
}
