package testutil

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Arguments
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.tracking.TrackingService
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Builds a real [AppState] for tests through its actual constructor (no reflection). A [FakeIpcState] is injected so
 * no port is bound, and the collaborators do not touch the network or audio devices. The application directory is
 * already redirected to a build directory by the test task (see `VLABELER_APP_DIR` in `build.gradle.kts`).
 *
 * @param scope the coroutine scope for the app; pass a cancelled scope to keep all background flows inert, or an
 *     unconfined scope to run launched work synchronously.
 */
object TestAppState {

    fun create(
        scope: CoroutineScope,
        appConf: AppConf = AppConf(),
        availableLabelerConfs: List<LabelerConf> = listOf(TestLabelers.utauOto, TestLabelers.nnsvsSinger),
        plugins: List<Plugin> = emptyList(),
        launchArguments: Arguments = Arguments(),
    ): AppState {
        TestEnv.ensureLogDirectory()
        val appConfState = mutableStateOf(appConf)
        val appRecordStore = AppRecordStore(com.sdercolin.vlabeler.model.AppRecord(), scope)
        return AppState(
            mainScope = scope,
            keyboardViewModel = KeyboardViewModel(scope, appConf.keymaps),
            scrollFitViewModel = ScrollFitViewModel(scope),
            appRecordStore = appRecordStore,
            trackingService = TrackingService(appRecordStore, scope),
            snackbarHostState = SnackbarHostState(),
            appConf = appConfState,
            availableLabelerConfs = availableLabelerConfs,
            plugins = plugins,
            launchArguments = launchArguments,
            ipcStateFactory = { FakeIpcState() },
        )
    }
}
