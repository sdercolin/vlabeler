package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.Version
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.repository.update.model.Asset
import com.sdercolin.vlabeler.repository.update.model.Release
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.repository.update.model.UpdateChannel
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.dialog.updater.UpdaterDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestEnv
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [UpdaterDialogState] and the update-selection logic in [Update.from]. No real download or network call is
 * made: the dialog is driven with an in-memory [Update], and version comparison is exercised through [Update.from]
 * with in-memory release data (assets for every platform are included so the resolved asset does not depend on the
 * host OS).
 *
 * [UpdaterDialogState.getDiffSummary] is `@Composable` and cannot be invoked outside a composition, so it is not
 * covered here.
 */
class UpdaterDialogStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var dialogScope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var appRecordStore: AppRecordStore

    private val update = Update(
        version = Version(9, 9, 9),
        date = "2024-01-01",
        assetUrl = "https://example.com/downloads/vlabeler-9.9.9-mac-arm64.dmg",
        diff = listOf(
            Update.Summary(Version(9, 9, 9), "https://example.com/releases/9.9.9", "2024-01-01"),
        ),
    )

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        scope = CoroutineScope(SupervisorJob())
        // Unconfined so that the coroutine launched by cancel()/cancelIgnored() runs synchronously
        dialogScope = CoroutineScope(Dispatchers.Unconfined)
        tempDir = createTempDirectory("vlabeler-test").toFile()
        appRecordStore = AppRecordStore(AppRecord(updateDownloadDirectory = tempDir.absolutePath), scope)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        dialogScope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun createState(onError: (Throwable) -> Unit = {}, onFinish: () -> Unit = {}) =
        UpdaterDialogState(update, dialogScope, appRecordStore, onError, onFinish)

    @Test
    fun testInitialState() {
        val state = createState()

        assertFalse(state.isDownloading)
        assertEquals(0f, state.progress)
        assertFalse(state.isShowingChoosingDownloadPositionDialog)
        assertEquals(tempDir.absolutePath, state.downloadDirectory.absolutePath)
        assertTrue(state.isDownloadPositionValid)
    }

    @Test
    fun testDownloadPositionValidityFollowsDirectory() {
        val state = createState()

        state.downloadDirectory = tempDir.resolve("missing")
        assertFalse(state.isDownloadPositionValid)
        // the new directory is persisted to the app record
        assertEquals(tempDir.resolve("missing").absolutePath, appRecordStore.value.updateDownloadDirectory)

        state.downloadDirectory = tempDir
        assertTrue(state.isDownloadPositionValid)
    }

    @Test
    fun testChooseDownloadPositionUpdatesDirectoryAndRecord() {
        val newDir = tempDir.resolve("downloads").apply { mkdirs() }
        val state = createState()

        state.openChooseDownloadPositionDialog()
        assertTrue(state.isShowingChoosingDownloadPositionDialog)

        state.handleChoosingDownloadPositionDialogResult(tempDir.absolutePath, "downloads")

        assertFalse(state.isShowingChoosingDownloadPositionDialog)
        assertEquals(newDir.absolutePath, state.downloadDirectory.absolutePath)
        assertEquals(newDir.absolutePath, appRecordStore.value.updateDownloadDirectory)
    }

    @Test
    fun testChooseDownloadPositionNullResultDoesNotChangeDirectory() {
        val state = createState()
        val before = state.downloadDirectory.absolutePath
        state.openChooseDownloadPositionDialog()

        state.handleChoosingDownloadPositionDialogResult(null, null)

        assertFalse(state.isShowingChoosingDownloadPositionDialog)
        assertEquals(before, state.downloadDirectory.absolutePath)
    }

    @Test
    fun testCancelInvokesFinish() {
        var finished = false
        val state = createState(onFinish = { finished = true })

        state.cancel()

        assertTrue(finished)
    }

    @Test
    fun testCancelIgnoredMarksVersionIgnored() {
        var finished = false
        val state = createState(onFinish = { finished = true })

        state.cancelIgnored()

        assertTrue(appRecordStore.value.isUpdateIgnored(update.version))
        assertTrue(finished)
    }

    // ---- Update.from: which updates are shown for a given current-vs-available version set ----

    private fun assetsFor(version: Version) = listOf(
        Asset("https://example.com/vlabeler-$version-win64.zip", "vlabeler-$version-win64.zip"),
        Asset("https://example.com/vlabeler-$version-mac-arm64.dmg", "vlabeler-$version-mac-arm64.dmg"),
        Asset("https://example.com/vlabeler-$version-mac-x64.dmg", "vlabeler-$version-mac-x64.dmg"),
        Asset("https://example.com/vlabeler-$version-amd64.deb", "vlabeler-$version-amd64.deb"),
    )

    private fun releaseOf(version: Version, prerelease: Boolean = false, draft: Boolean = false) = Release(
        htmlUrl = "https://example.com/releases/$version",
        tagName = version.toString(),
        publishedAt = "2024-01-01T00:00:00Z",
        prerelease = prerelease,
        draft = draft,
        assets = assetsFor(version),
    )

    private val older = Version(0, 0, 0)
    private val newer1 = appVersion.copy(major = appVersion.major + 1, stage = null, stageVersion = null)
    private val newer2 = appVersion.copy(major = appVersion.major + 2, stage = null, stageVersion = null)

    @Test
    fun testUpdateFromReturnsNewestWithDiffOfNewerVersions() {
        val releases = listOf(releaseOf(newer1), releaseOf(older), releaseOf(newer2))

        val result = Update.from(releases, UpdateChannel.Stable)

        assertNotNull(result)
        assertEquals(newer2, result.version)
        // diff lists the newer versions from newest to oldest, excluding the already-installed/older ones
        assertEquals(listOf(newer2, newer1), result.diff.map { it.version })
    }

    @Test
    fun testUpdateFromReturnsNullWhenNothingIsNewer() {
        val result = Update.from(listOf(releaseOf(older)), UpdateChannel.Stable)

        assertNull(result)
    }

    @Test
    fun testUpdateFromReturnsNullForNoReleases() {
        assertNull(Update.from(emptyList(), UpdateChannel.Stable))
    }

    @Test
    fun testUpdateFromExcludesDraftReleases() {
        val releases = listOf(releaseOf(newer1, draft = true), releaseOf(newer2))

        val result = Update.from(releases, UpdateChannel.Stable)

        assertNotNull(result)
        assertEquals(newer2, result.version)
        assertEquals(listOf(newer2), result.diff.map { it.version })
    }
}
