# Testing

This is the documentation of the automated test suite of `vLabeler`: how to run it, how it is structured, and the
conventions to follow when adding tests.

## Running tests

```bash
./gradlew jvmTest                                  # run all tests
./gradlew jvmTest --tests "io.RawLabelsTest"       # run a single test class
./gradlew build test                               # what PR CI runs (includes ktlint and license report check)
./gradlew koverHtmlReport                          # coverage report at build/reports/kover/html
```

CI (`.github/workflows/pull-request.yml`) runs the full build and tests for PRs targeting `main`, `dev` and
`feature/automated-tests`, and uploads test and coverage reports as workflow artifacts (also on failure).

## Test source layout

All tests live in `src/jvmTest/kotlin`, written with `kotlin.test` on the JUnit 5 platform. Packages are short names
mirroring the production area rather than full package paths:

| Location | Contents |
|---|---|
| `io/` | Label parsing/writing, reloading, project files, wave loading, DSP (power/spectrogram/fundamental) |
| `model/` | Domain model: `Module`, `ProjectHistory`, `LabelerConf`, `Entry`, serialization |
| `util/` | Pure helper functions |
| `env/`, `strings/` | Environment and localization helpers |
| `fixtures/` | Smoke tests creating projects from the fixture data with the bundled labelers |
| `testutil/` | Shared test helpers (see below) |
| (root) | Older tests for single utilities |

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

### Environment gotchas

- **Logging**: set `Log.muted = true` in `@BeforeTest` and back to `false` in `@AfterTest` when the tested code logs.
- **Log directory**: constructing `util.JavaScript()` with default arguments opens the info log file directly, which
  fails on machines where the app has never run (e.g. CI). Call `testutil.TestEnv.ensureLogDirectory()` first
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
3. **Phase 3 — integration tests**: bundled template/macro plugin execution, full project lifecycle
   (create → save → load → edit → export)
4. **Phase 4 — UI tests**: state holders (`ProjectStore`, dialog states) and Compose UI tests (`runComposeUiTest`)
5. **Phase 5 — CI polish**: split unit/integration steps, coverage thresholds

When adding tests in a new phase, keep this document and the Testing section of `CLAUDE.md` up to date.
