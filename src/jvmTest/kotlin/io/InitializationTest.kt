package io

import com.sdercolin.vlabeler.env.Locale
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.io.ensureDirectories
import com.sdercolin.vlabeler.io.initializeGlobalRepositories
import com.sdercolin.vlabeler.io.loadAppConf
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.io.runMigration
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.AppRecordFile
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultAppConfFile
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the app initialization steps in `io/Initialization.kt`.
 *
 * These run against the application directory redirected by the `VLABELER_APP_DIR` environment variable (set to
 * `build/test-app-dir` by the test tasks), so no real user data is touched. Files that are modified are restored in
 * [teardown] so other tests sharing the directory are unaffected.
 */
class InitializationTest {

    private lateinit var scope: CoroutineScope
    private var originalAppConfText: String? = null
    private var originalAppRecordText: String? = null

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        ensureDirectories()
        originalAppConfText = CustomAppConfFile.takeIf { it.exists() }?.readText()
        originalAppRecordText = AppRecordFile.takeIf { it.exists() }?.readText()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            val job = requireNotNull(scope.coroutineContext[Job])
            job.cancel()
            job.join()
        }
        ensureDirectories()
        originalAppConfText?.let { CustomAppConfFile.writeText(it) } ?: CustomAppConfFile.delete()
        originalAppRecordText?.let { AppRecordFile.writeText(it) } ?: AppRecordFile.delete()
        Log.muted = false
    }

    private fun awaitCondition(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(condition(), "Condition not satisfied within $timeoutMs ms")
    }

    private fun recordStoreOf(appRecord: AppRecord) = AppRecordStore(appRecord, scope)

    private val defaultAppConf: AppConf get() = DefaultAppConfFile.readText().parseJson()

    @Test
    fun testEnsureDirectoriesCreatesMissingDirectories() {
        CustomPluginDir.deleteRecursively()
        assertFalse(CustomPluginDir.exists())

        ensureDirectories()

        assertTrue(AppDir.isDirectory)
        assertTrue(CustomLabelerDir.isDirectory)
        assertTrue(CustomPluginDir.isDirectory)
        assertTrue(RecordDir.isDirectory)
        Plugin.Type.entries.forEach {
            assertTrue(CustomPluginDir.resolve(it.directoryName).isDirectory)
        }

        // calling again on existing directories is a no-op
        ensureDirectories()
        assertTrue(CustomPluginDir.isDirectory)
    }

    @Test
    fun testLoadAppConfWithoutCustomFileUsesDefault() {
        CustomAppConfFile.delete()
        val store = recordStoreOf(AppRecord(hasSavedDetectedLanguage = true))

        val state = loadAppConf(scope, store)

        assertEquals(defaultAppConf, state.value)
        assertTrue(CustomAppConfFile.exists())
        assertEquals(defaultAppConf, CustomAppConfFile.readText().parseJson())
    }

    @Test
    fun testLoadAppConfUsesValidCustomFile() {
        val custom = defaultAppConf.let { it.copy(view = it.view.copy(language = Language.Japanese)) }
        CustomAppConfFile.writeText(custom.stringifyJson())
        val store = recordStoreOf(AppRecord(hasSavedDetectedLanguage = true))

        val state = loadAppConf(scope, store)

        assertEquals(custom, state.value)
    }

    @Test
    fun testLoadAppConfIgnoresInvalidCustomFile() {
        CustomAppConfFile.writeText("{ this is not a valid app conf")
        val store = recordStoreOf(AppRecord(hasSavedDetectedLanguage = true))

        val state = loadAppConf(scope, store)

        assertEquals(defaultAppConf, state.value)
        // the invalid file is overwritten with the loaded conf
        assertEquals(defaultAppConf, CustomAppConfFile.readText().parseJson())
    }

    @Test
    fun testLoadAppConfDetectsLanguageOnFirstLaunch() {
        CustomAppConfFile.delete()
        val store = recordStoreOf(AppRecord(hasSavedDetectedLanguage = false))

        val state = loadAppConf(scope, store)

        val detected = Language.find(Locale.toLanguageTag())
        if (detected != null) {
            assertEquals(detected, state.value.view.language)
            awaitCondition { store.value.hasSavedDetectedLanguage }
        } else {
            assertEquals(defaultAppConf.view.language, state.value.view.language)
            assertFalse(store.value.hasSavedDetectedLanguage)
        }
    }

    @Test
    fun testLoadAppConfPersistsStateChanges() {
        CustomAppConfFile.delete()
        val store = recordStoreOf(AppRecord(hasSavedDetectedLanguage = true))
        val state = loadAppConf(scope, store)

        val updated = state.value.let { it.copy(view = it.view.copy(language = Language.Korean)) }
        state.value = updated

        awaitCondition {
            runCatching { CustomAppConfFile.readText().parseJson<AppConf>() }.getOrNull() == updated
        }
    }

    @Test
    fun testRunMigrationUpdatesLastLaunchedVersion() {
        // same major and minor as the current version, so the minor-version migration is not triggered
        val previousRaw = "${appVersion.major}.${appVersion.minor}.${appVersion.patch + 1}"
        val store = recordStoreOf(
            AppRecord(
                appVersionLastLaunchedRaw = previousRaw,
                hasCheckedRosettaCompatibleMode = true,
            ),
        )

        runMigration(store)

        awaitCondition { store.value.appVersionLastLaunchedRaw == appVersion.toString() }
        assertEquals(appVersion, store.value.appVersionLastLaunched)
        assertTrue(store.value.hasCheckedRosettaCompatibleMode)
    }

    @Test
    fun testLoadPluginsLoadsBundledPlugins() {
        val plugins = runBlocking { loadPlugins(Language.English) }

        val names = plugins.map { it.name }
        assertTrue(plugins.isNotEmpty())
        assertTrue(names.contains("cv-oto-gen"))
        assertTrue(names.contains("batch-edit-entry-name"))
        assertTrue(plugins.first { it.name == "cv-oto-gen" }.type == Plugin.Type.Template)
        assertTrue(plugins.first { it.name == "batch-edit-entry-name" }.type == Plugin.Type.Macro)
        assertTrue(plugins.first { it.name == "cv-oto-gen" }.builtIn)
    }

    @Test
    fun testInitializeGlobalRepositories() {
        val store = recordStoreOf(AppRecord())

        initializeGlobalRepositories(store)

        assertTrue(ColorPaletteRepository.directory.isDirectory)
        assertTrue(
            ColorPaletteRepository.directory.listFiles().orEmpty().any { it.name.endsWith(".example.json") },
        )
        assertTrue(FontRepository.fontDirectory.isDirectory)
        assertTrue(FontRepository.fontDirectory.resolve("readme.txt").exists())
    }
}
