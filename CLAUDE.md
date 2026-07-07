# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

vLabeler is an open-source desktop voice labeling application built with Kotlin + Compose Multiplatform (JVM-only, `src/jvmMain`). Label formats and batch operations are user-customizable via JavaScript-based "labelers" and "plugins" executed by an embedded GraalVM JS engine.

Requires **JDK 17+** to build.

## Commands

```bash
./gradlew build test                      # full build + tests (what PR CI runs)
./gradlew jvmTest                         # run tests only
./gradlew jvmTest --tests "EntryListDiffTest"   # run a single test class
./gradlew compileKotlinJvm                # quick compile check
./gradlew ktlintCheck                     # lint (ktlint 0.45.2, experimental rules on, wildcard imports allowed)
./gradlew ktlintFormat                    # auto-format
./gradlew run                             # run the app
./gradlew createDistributable             # build executable app for current OS
./gradlew packageDistributionForCurrentOS # build installer for current OS
./gradlew updateLicenseReport             # regenerate src/jvmMain/resources/licenses.json
```

Notes:
- The `test` task depends on `checkLicenseReportUpdate` — if you add/change dependencies in `build.gradle.kts`, run `./gradlew updateLicenseReport` or tests will fail.
- App version lives in `gradle.properties` (`app.version`).
- Dev feature flags: add `flag.xxx=true` to `local.properties` (read by `flag/FeatureFlags.kt` via system properties).

## Testing

Full guide: `docs/testing.md`. Keep both that file and this section updated when extending the test suite.

- Tests use kotlin.test on JUnit Platform, under `src/jvmTest/kotlin`, in short packages mirroring the production area (`io`, `model`, `util`, `plugins`, ...). Coverage via Kover (`./gradlew koverHtmlReport`).
- All bundled labelers and plugins have integration tests (`fixtures/`, `plugins/`) that execute their real JS scripts — when changing a bundled labeler/plugin, update its test in the same PR.
- Compose UI tests use `runComposeUiTest` (headless via Skiko, works on CI); state holders like `ProjectStore` are tested without rendering. See the "Compose UI tests" section of `docs/testing.md` for desktop-specific gotchas.
- Shared helpers in `src/jvmTest/kotlin/testutil/`: `TestLabelers` (loads real bundled labelers — the test task points `compose.application.resources.dir` at `resources/common`), `TestFixtures.deploy(...)` (copies a fixture project from `src/jvmTest/resources/fixtures/` and generates wav files at runtime — no binaries in git), `createTestProject(...)` (runs the real `projectOf` flow incl. JS scripts), `TestEnv.ensureLogDirectory()` (required before code that constructs `util.JavaScript()` with defaults; the log dir only exists where the app has run — missing on CI).
- Set `Log.muted = true`/`false` in setup/teardown when tested code logs.
- If a test reveals a suspected production bug, pin the current behavior with a comment and raise the bug for discussion instead of silently changing behavior.

## Git conventions

- Base branch for PRs is `dev` (not `main`). `main` is used for releases.

## Architecture

All application code is under `src/jvmMain/kotlin/com/sdercolin/vlabeler/`.

### State management

- `Main.kt` is the Compose `application` entry point: initializes logging, directories, repositories, migration, then builds the root `AppState` and hosts `App`, `Menu`, dialogs, and window listeners.
- `ui/AppState.kt` is the central state holder, composed by interface delegation (`AppErrorState`, `AppScreenState`, `ProjectStore`, `AppDialogState`, etc., each with an `...Impl`). State is plain Compose `mutableStateOf` throughout — no ViewModel/Flow architecture.
- `ui/App.kt` switches on `appState.screen` (`Starter` → `ProjectCreator` → `Editor`); `ui/editor/EditorState.kt` holds editor-local state (edited entries, canvas, tool, chart store).
- `ui/ProjectStore.kt` owns the open `Project` plus undo/redo history (`ProjectHistory`), entry CRUD, and auto-save.
- Persisted app-level records go through `AppRecordStore` / `savedMutableStateOf`.

### Domain model (`model/`)

All `@Serializable @Immutable` data classes using kotlinx.serialization JSON:

- `Project` (`.lbp` file): current `PROJECT_VERSION = 4`; contains one or more `Module`s (subprojects), the embedded `LabelerConf`, and labeler parameters. Version migrations are documented in `docs/project-file-updates.md`.
- `LabelerConf` (`labeler.json`): defines a label format — fields/points, parser/writer JS scripts, parameters, project constructor. Has its own `serialVersion` migration (`docs/labeler-structure-updates.md`).
- `Module`: a subproject with its own sample directory, entry list, and raw label file.
- `Entry`: one label — sample name, start/end in ms (end ≤ 0 means relative to sample end), points, extras, notes.
- `Plugin`: `Template` type (generates entries at project creation) or `Macro` type (batch-edits an open project).

### Scripting system

- `util/JavaScript.kt` wraps a GraalVM Polyglot JS context; data crosses the Kotlin↔JS boundary as JSON. Shared JS runtime libraries live in `src/jvmMain/resources/js/`.
- Executed from: labeler parser/writer (`io/RawLabels.kt`), macro plugins (`model/MacroPlugin.kt`), project constructors (`model/Project.kt`), property/filter expressions.
- Built-in labelers and plugins live in `resources/common/` (labelers/, plugins/template/, plugins/macro/) and are bundled into the distribution's resource dir. User-installed ones go to the app directory at runtime.
- Scripting APIs are documented in `docs/` (`scripting.md`, `plugin-development.md`, `labeler-development.md`, `parameter.md`, `env-api.md`, `file-api.md`, `command-line-api.md`).

### Audio & rendering

- `io/Wave.kt` loads samples via `javax.sound.sampled` in chunks (`SampleChunk`/`SampleInfo`); non-wav formats converted by `audio/conversion/`. Spectrogram/power/fundamental extractors in `io/`.
- `repository/` holds per-project cache singletons: `ChartRepository` (rendered waveform/spectrogram images, invalidated by painter config/algorithm version), `SampleInfoRepository`, `ConvertedAudioRepository`, etc.
- The editor canvas lives in `ui/editor/labeler/` with draggable markers in `ui/editor/labeler/marker/`.

### IPC

`ipc/IpcServer.kt` runs a ZeroMQ REP socket on `tcp://*:32342` accepting JSON requests (e.g. open/create project) so external tools can remote-control vLabeler.

### Localization

- UI strings: `ui/string/Strings.kt` is an enum of keys with per-language tables (`StringsEnglish.kt`, `StringsChineseSimplified.kt`, `StringsJapanese.kt`, `StringsKorean.kt`). Add a new key to the enum plus all language files (English is the fallback).
- Labeler/plugin metadata uses `LocalizedJsonString` (`docs/localized-string.md`).
- The README also exists in zh-CN/ja/ko under `readme/`; user-facing feature changes may need updates there.
