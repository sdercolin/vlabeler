# Testing

This is the documentation of the automated test suite of `vLabeler`: how to run it, how it is structured, and the
conventions to follow when adding tests.

## Running tests

```bash
./gradlew jvmTest                                  # run all tests
./gradlew jvmTest --tests "io.RawLabelsTest"       # run a single test class
./gradlew unitTest                                 # only unit tests (util, model, env, strings, testutil)
./gradlew integrationTest                          # only integration tests (io, fixtures, plugins)
./gradlew uiTest                                   # only UI tests (ui)
./gradlew build test koverVerify                   # what PR CI runs (includes ktlint and license report check)
./gradlew koverHtmlReport                          # coverage report at build/reports/kover/html
```

CI (`.github/workflows/pull-request.yml`) runs the full build and tests for PRs targeting `main` and `dev`, uploads
test and coverage reports as workflow artifacts (also on failure), and enforces a minimal line coverage bound via
`koverVerify` (see `koverReport` in `build.gradle.kts`; raise the bound as coverage grows). Coverage data is only
collected from the full `jvmTest` task — the subset tasks are a local convenience and are not instrumented.

## Test source layout

All tests live in `src/jvmTest/kotlin`, written with `kotlin.test` on the JUnit 5 platform. Packages are short names
mirroring the production area rather than full package paths:

| Location | Contents |
|---|---|
| `io/` | Label parsing/writing, reloading, project files/lifecycle, wave loading, DSP (power/spectrogram/fundamental) |
| `model/` | Domain model: `Module`, `ProjectHistory`, `LabelerConf`, `Entry`, serialization |
| `plugins/` | Integration tests executing all bundled template and macro plugins through the real plugin runner |
| `repository/` | Cache repositories: versioning/invalidation, move/clear |
| `ipc/` | Remote-control API: wire-level JSON contract and real ZeroMQ round trips |
| `ui/` | State-holder tests (`ProjectStore`, project creator wizard, preferences editor, plugin/customization dialogs, app states) and Compose UI tests (`*UiTest`) |
| `com.sdercolin.vlabeler.ui.editor.labeler.marker` | Editor marker drag/constraint logic (`MarkerStateFactory` builds real states) |
| `util/` | Pure helper functions |
| `env/`, `strings/` | Environment and localization helpers |
| `fixtures/` | Smoke tests creating projects from the fixture data with the bundled labelers |
| `testutil/` | Shared test helpers (see below) |

A few tests use the full production package name (e.g. `com.sdercolin.vlabeler.ui.editor...`) instead of a short
package; prefer short packages for new tests so that the subset tasks pick them up (the `uiTest` subset covers both
forms for `ui`).

## Test infrastructure

### Bundled labelers and plugins are available in tests

The `jvmTest` task sets the `compose.application.resources.dir` system property to `resources/common` (see
`build.gradle.kts`), so production code that reads `DefaultLabelerDir` / `DefaultPluginDir` works in tests exactly as
in a packaged app. Use `testutil.TestLabelers` to load a bundled labeler (`utauSinger`, `utauOto`, `nnsvsSinger`,
`audacity`, `sinsy`) through the real loading path (`asLabelerConf`).

### Fixtures

`src/jvmTest/resources/fixtures/` contains text label files for small sample projects:

- `utau-singer/` — a voicebank with two pitch folders (`C4`, `A3`) with `oto.ini` files
- `nnsvs-singer/` — `lab/` folder with HTS-style `.lab` files (100 ns units)
- `oto/` — a flat single-`oto.ini` voicebank
- `plugins/<plugin-name>/` — inputs for plugin tests (UST files, Audacity/Sinsy label files, oto files, prefix maps)

Wav files are **not committed**; they are generated at test runtime. Deploy a fixture to a temp directory with:

```kotlin
val sampleDir = TestFixtures.deploy(
    "utau-singer",
    tempDir.resolve("utau-singer"),
    wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
)
```

`testutil.TestWav.write(file, durationMs, sampleRate, frequency)` writes real 16-bit mono PCM sine wavs;
`testutil.TestWaves` builds in-memory `Wave.Channel` objects for DSP tests.

Note: the fixture root is located on the classpath via the `fixtures/root.marker` file, because a plain directory
lookup can be shadowed by a package of the same name.

### Creating projects

`testutil.createTestProject(labeler, sampleDirectory, ...)` runs the real project creation flow (`projectOf`),
including the labeler's project constructor and parser scripts in the GraalVM JS engine. See
`fixtures/UtauSingerProjectFixtureTest.kt` for a complete example including temp directory setup/teardown.

### Running plugins

Plugin tests load the bundled plugins through the real `loadPlugins(type, language)` path (which also exercises
`file::` default-parameter injection) and execute them with `runTemplatePlugin` / `runMacroPlugin`. Shared helpers:
`plugins/TemplatePluginRunner.kt` and `plugins/MacroPluginTestBase.kt` (the latter builds a two-module `utau-singer`
project and captures plugin reports via `MacroPluginExecutionListener`). Tests assert exact output entries; when
adding or changing a bundled plugin, add or update its test accordingly.

### Compose UI tests

UI tests use the JUnit-independent `runComposeUiTest` (`androidx.compose.ui.test`, opt-in `ExperimentalTestApi`) from
the `compose.uiTest` dependency. It renders offscreen through Skiko, so it works on the JUnit 5 platform and on
headless CI machines without a display. See `ui/ComposeUiTestSmokeTest.kt` for the base pattern
(`runComposeUiTest { setContent { AppTheme { ... } } }`) and the `ui/*UiTest.kt` classes for component examples.
Notes:

- A `Modifier.testTag` on a component wrapper usually lands on a `Box`, not the interactive descendant — find
  interactive nodes with matchers such as `hasSetTextAction()` or `hasClickAction()`.
- On desktop, a disabled `BasicTextField` still exposes `SetText` semantics (unlike Android) — assert with
  `isNotEnabled()` instead of action absence.
- `string(Strings.X)` resolves without extra setup (`LocalLanguage` defaults to English).
- "Cannot find font family Default" log lines in headless runs are benign.
- Screen-level dialogs wrapped in an AWT `DialogWindow` (e.g. `PluginDialog`, `ColorPickerDialog`) do not mount in
  `runComposeUiTest`; test their inner content composable or drive the flow through the public state holder instead.

State-holder classes (`ui/ProjectStore.kt`, `AppErrorState`, dialog states, ...) are plain classes over Compose
`mutableStateOf` and are tested without rendering — see `ui/ProjectStoreTest.kt`, which drives the real
implementations against fixture projects. For classes that require an `AppState` (which cannot be constructed in
tests because it binds the IPC port and starts analytics/audio), `ui/EditorStateTest.kt` shows the furthest-going
pattern: an uninitialized instance via `sun.misc.Unsafe` with real members injected reflectively, giving a
functional editor against real sample loading and chart rendering.

### Environment gotchas

- **Application directory**: the test tasks set the `VLABELER_APP_DIR` environment variable to `build/test-app-dir`,
  so `AppDir` (logs, records, custom labelers/plugins) never points at the real user directory. Tests must still not
  write outside temp directories or that build directory.
- **Logging**: set `Log.muted = true` in `@BeforeTest` and back to `false` in `@AfterTest` when the tested code logs.
- **Log directory**: constructing `util.JavaScript()` with default arguments opens the info log file directly, which
  fails if the (redirected) log directory does not exist yet. Call `testutil.TestEnv.ensureLogDirectory()` first
  (`createTestProject` already does).
- **License report**: the `test` Gradle task depends on `checkLicenseReportUpdate`; run
  `./gradlew updateLicenseReport` after changing dependencies.

## Conventions

- Deterministic, exact assertions; no sleeps (except where file timestamp resolution requires it), no randomness, no
  network, no native dependencies (FFmpeg, VLC, LWJGL) in tests.
- DSP assertions verify meaningful properties (shapes, ranges, energy at the expected frequency bin) instead of exact
  floating point equality.
- ktlint applies to test sources: run `./gradlew ktlintFormat` before committing.
- Tests must not change production behavior. If a test reveals a suspected bug, write the test against the **current**
  behavior with a comment, and raise the bug separately for discussion before fixing it.

## Roadmap

The test effort is tracked on the `feature/automated-tests` branch (sub-parts are merged into it via PRs):

1. ✅ **Phase 1 — foundations**: Kover coverage, fixtures, `testutil` helpers, labeler smoke tests, CI reports
2. ✅ **Phase 2 — unit tests**: `io` (raw labels round-trips, reload, backups, wave/DSP), `model`, `util`
3. ✅ **Phase 3 — integration tests**: all bundled template/macro plugins, full project lifecycle
   (create → save → load → edit → export → reload)
4. ✅ **Phase 4 — UI tests**: state holders (`ProjectStore`, app states) and Compose UI tests for common
   components and a standalone dialog. Not covered yet: the editor canvas/marker UI, dialogs requiring a full
   `AppState`, and full-application flows.
5. ✅ **Phase 5 — CI polish**: per-level test subset tasks, minimal line coverage bound in CI, GitHub Actions
   upgrades. (Splitting CI into per-level steps was evaluated and skipped: Kover only instruments `jvmTest`, so a
   split would double-run the whole suite for coverage — not worth it at the current suite runtime.)

When adding tests in a new phase, keep this document and the Testing section of `CLAUDE.md` up to date.
