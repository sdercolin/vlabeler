package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Locale
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.env.isFileSystemCaseSensitive
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Arguments
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.tracking.TrackingService
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultAppConfFile
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.or
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.savedMutableStateOf
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

suspend fun loadPlugins(language: Language): List<Plugin> = withContext(Dispatchers.IO) {
    loadPlugins(Plugin.Type.Template, language) + loadPlugins(Plugin.Type.Macro, language)
}

fun ensureDirectories() {
    val directories = listOf(AppDir, CustomLabelerDir, CustomPluginDir, RecordDir) +
        Plugin.Type.entries.map { CustomPluginDir.resolve(it.directoryName) }

    directories.forEach {
        if (it.exists().not()) {
            it.mkdir()
            Log.info("$it created")
        }
    }

    // check case sensitivity
    Log.info("isFileSystemCaseSensitive: $isFileSystemCaseSensitive")
}

fun initializeGlobalRepositories(appRecordStore: AppRecordStore) {
    ColorPaletteRepository.initialize(appRecordStore.value)
    FontRepository.initialize(appRecordStore.value)
}

fun runMigration(appRecordStore: AppRecordStore) {
    val previousVersion = appRecordStore.value.appVersionLastLaunched
    // Migrations
    if (appVersion.isMinorNewerThan(previousVersion)) {
        // Check if the app is compatible with Rosetta every minor version
        appRecordStore.update { copy(hasCheckedRosettaCompatibleMode = false) }
    }
    // end of migrations
    appRecordStore.update { onAppVersionLaunched() }
}

suspend fun produceAppState(
    mainScope: CoroutineScope,
    appConf: MutableState<AppConf>,
    appRecordStore: AppRecordStore,
    arguments: Arguments,
): AppState {
    val availableLabelerConfs = loadAvailableLabelerConfs()
    val plugins = loadPlugins(appConf.value.view.language)

    val scrollFitViewModel = ScrollFitViewModel(mainScope)
    val snackbarHostState = SnackbarHostState()
    val keyboardViewModel = KeyboardViewModel(mainScope, appConf.value.keymaps)

    val trackingService = TrackingService(appRecordStore, mainScope)

    return AppState(
        mainScope,
        keyboardViewModel,
        scrollFitViewModel,
        appRecordStore,
        trackingService,
        snackbarHostState,
        appConf,
        availableLabelerConfs,
        plugins,
        arguments,
    )
}
