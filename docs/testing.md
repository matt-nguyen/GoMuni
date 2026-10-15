# Testing

These conventions define the project's testing policy. Apply them to new work
and authorized refactors.

## Required coverage

- Meaningful behavior changes require automated tests. Cover observable outcomes,
  rules, mapping, failures, concurrency, and lifecycle behavior as applicable.
- Add regression tests for bug fixes when practical. For refactors, run relevant
  tests and fill gaps protecting the affected contract.
- Documentation and trivial styling changes need review or visual checks, not new
  tests. Avoid coverage-only tests, private-method tests, and duplicate assertions
  across layers. Keep arrange, act, and assert clear.
- Follow the owning contracts: [Architecture](architecture.md),
  [ViewModels](viewmodels.md#verification), [UI state](ui-state.md),
  [UI architecture](ui-architecture.md), [Compose](compose.md), and
  [Navigation](navigation.md).

## File organization

- One primary production file per subject-specific test file; one test class per
  file. Match the subject's name with a `Test` suffix and mirror its package.
  Related declarations in one production file may have subject-named suites.
- Split independent subjects even within one feature. JVM and instrumented suites
  may cover the same production file. Collaborators are allowed; keep production
  boundaries intact.
- Integration suites may span files for one focused collaboration or flow.
  Document that scope in class KDoc and place the suite in its owning package.
  Platform smoke tests may target application configuration.
- Put reusable fakes, rules, and factories in support files without test methods.
  Keep mutable fixtures fresh per test and setup/cleanup owned by each suite.
  Keep private helpers local; avoid inheritance solely to share setup.

## Naming

Name tests by scenario and observable outcome; avoid vague or numbered names.
Use role-based variables such as `viewModel`, `expected`, and `actual`.

| Element | Convention |
| --- | --- |
| Class/file | `StopArrivalsViewModelTest` / `StopArrivalsViewModelTest.kt` |
| Integration suite | Focused flow, such as `ArrivalsSheetIntegrationTest` |
| Fake | `FakeTransitRepository` |
| Mock variable | `mockNotificationLauncher` |
| Fixture factory | `createTransitStop(id = "123")` |
| Rule | `mainDispatcherRule`, `composeRule` |

| Test location | Method style | Compatibility |
| --- | --- | --- |
| `src/test` (local JVM) | Prefer sentences in backticks, with spaces. | JVM compatible; reads as a specification. |
| `src/androidTest` (instrumented/UI) | Require `scenario_whenCondition_expectedOutcome`, using camelCase segments. Omit the condition when unnecessary. | Compatible with all supported Android versions (API 26+). |
| Existing tests in either source set | Retain ordinary camelCase/snake_case names unless renaming is authorized. | Compatible without spaces. |

Spaced backtick names are exclusive to local JVM tests. **Never use them in
`src/androidTest`**, even when testing only on newer devices: Android support
starts at API 30, and older devices/emulators can fail with `Bad Descriptor` or
DEX validation/build errors. See
[Kotlin's naming guidance](https://kotlinlang.org/docs/coding-conventions.html#names-for-test-methods).

```kotlin
// src/test
@Test
fun `given cached arrivals when refresh fails then retain cached arrivals`() = runTest {
    // Arrange, act, assert.
}

// src/androidTest
@Test
fun selectStop_whenStopIsVisible_emitsSelectionAction() {
    // Arrange, act, assert.
}
```

## Setup and test boundaries

The `:app` module uses JUnit 4 and coroutine tests locally; AndroidJUnitRunner,
AndroidX JUnit, Espresso, and Compose rules on devices. Dependencies live in
[app/build.gradle.kts](../app/build.gradle.kts) and the
[version catalog](../gradle/libs.versions.toml). Mocking libraries, Turbine,
Robolectric, Hilt testing, and screenshot tooling are not currently declared;
add tools only for a concrete need.

| Behavior | Test boundary |
| --- | --- |
| Pure rules, mapping, filtering, navigation policy | Local unit test |
| Repository coordination, caching, failure translation | Controlled sources; real storage/transport where their semantics matter |
| ViewModel state, actions, outputs, async decisions | Local coroutine test through domain contracts |
| Screen/component rendering and interactions | Instrumented Compose test with supplied state and recorded actions |
| Route wiring, sheets, navigation lifetimes | Focused instrumented integration test |
| Permissions, recreation, services | Device/emulator tests; targeted manual checks when automation is impractical |

Use real isolated databases for Room semantics and real transport/parsing boundaries
for HTTP contracts; repository fakes cannot prove either.

## Fakes versus mocks

Prefer small handwritten fakes for owned interfaces: controlled flows, results,
failures, and completion gates. Preserve cancellation and stream contracts; model
non-cooperative completion only when testing it. Construct subjects directly unless
DI wiring is under test.

Assert outcomes; record calls only when arguments, duplicates, or handoffs are the
contract. Use mocks for narrow interactions when simpler than a fake; avoid
incidental call-order assertions. Use real value objects, Compose, StateFlow, and
scheduling semantics. Share helpers only for meaningful reuse.

Examples: [FakeTransitRepository](../app/src/test/java/com/nghianguyen/gomuni/features/transit/testing/FakeTransitRepository.kt)
and [StopArrivalsViewModelTest](../app/src/test/java/com/nghianguyen/gomuni/features/transit/arrivals/StopArrivalsViewModelTest.kt).

## Coroutine tests

- Use `runTest`, default to `StandardTestDispatcher`, and share `testScheduler`
  across injected dispatchers. Inject clocks/dispatchers where needed; virtual time
  does not control real I/O or unrelated dispatchers.
- Install/reset Main through a rule before constructing local Main-dependent
  subjects, including failure cleanup. Never replace Android Main in Compose tests.
- Start collectors before non-replayed events. Keep subscription-driven state
  collected; `UnconfinedTestDispatcher(testScheduler)` may start a collector
  immediately. Avoid using it to conceal ordering bugs.
- Read `StateFlow.value` for current state. Collect transition sequences only when
  contractual; StateFlow conflates values.
- Release gates, cancel owned jobs/scopes, and clear ViewModels through their owner.
  Leave no unowned work. Follow the ViewModel checklist for cancellation, stale
  results, effects, restoration, and collector restarts.

| Tool | Purpose |
| --- | --- |
| `runCurrent()` | Execute queued work at current virtual time; inspect intermediate state. |
| `advanceTimeBy(duration)` then `runCurrent()` | Test delay/debounce boundaries. |
| `advanceUntilIdle()` | Finish bounded work; avoid for recurring polling. |
| `CompletableDeferred` | Hold/release requests deliberately. |
| `backgroundScope` | Automatically clean up long-lived test collectors. |

Never use sleeps, real-time delays, or repeated yields to fix ordering.

## Compose tests

- Supply immutable state, the app theme, and recorded typed actions/callbacks.
  Replace expensive integrations through slots when testing wiring; test the real
  integration separately when it changes.
- Select meaningful semantics; use stable tags when needed. Avoid private node
  structure, child indexes, or pixel assertions unless geometry is the contract.
- Use synchronized Compose actions/assertions and `runOnIdle`. Use bounded
  `waitUntil` for external work; larger timeouts do not fix races.
- Use `mainClock` for frames/animations. It is separate from coroutine virtual time;
  neither clock completes platform/network work automatically.
- Cover relevant loading/content/empty/error states and changed interactions.
  Test effects/lifecycles at integration boundaries.
- `StateRestorationTester` proves saveable Compose restoration, not process recovery.
  Verify Android recreation/recovery separately when changed. Previews and
  `DESIGN.md`/`screenshots/` comparisons supplement interaction/recovery tests.

Examples: [screen actions](../app/src/androidTest/java/com/nghianguyen/gomuni/features/transit/stops/RouteStopsScreenTest.kt),
[sheet lifetimes](../app/src/androidTest/java/com/nghianguyen/gomuni/features/transit/arrivals/ArrivalsSheetIntegrationTest.kt),
[restoration](../app/src/androidTest/java/com/nghianguyen/gomuni/app/navigation/TransitNavigationRestorationTest.kt).

## Running and reporting

### Formatting

Spotless uses the pinned ktfmt Kotlin style for application/test Kotlin sources
and Gradle Kotlin scripts. Generated files, local configuration, and vendored
skills are outside its targets. The Android module's `check` task also runs
`spotlessCheck`.

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck
```

Apply formatting before submitting Kotlin changes. On macOS/Linux use `./gradlew`.

### Tests

Run from the repository root with the Gradle wrapper, configured daemon JVM
(currently Java 21), and Android SDK 36. See [local configuration](../README.md#configuration).
Dependencies may need network access; behavior tests must avoid live services and
production credentials. Devices/emulators require API 26+.

```powershell
# Local suite / one class
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:testDebugUnitTest --tests "com.nghianguyen.gomuni.features.transit.arrivals.StopArrivalsViewModelTest"

# Compile instrumented tests / run suite / run one class
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.nghianguyen.gomuni.features.transit.arrivals.ArrivalsSheetIntegrationTest"
```

On macOS/Linux use `./gradlew`; Android Studio supports gutter runs.

| HTML report | Location |
| --- | --- |
| Local | `app/build/reports/tests/testDebugUnitTest/` |
| Connected devices | `app/build/reports/androidTests/connected/` |

Run focused tests, then the affected suite; run device tests for Android/Compose
contracts. Report commands, results, blockers, and unverified behavior. Compilation
is not execution. Distinguish assertion failures from build/device setup failures.

## Flaky tests

Preserve the first failure, stack trace, device/API, and whether it fails alone or
in a suite. Diagnose scheduling, collectors, event order, gates, leaks, shared state,
real clocks/network, polling, and UI synchronization. Use controlled dependencies
or bounded waits; keep tests independent and clean up files, databases, jobs, and
platform state.

Retries help diagnosis and fix verification; they do not erase unexplained failures.
Never weaken assertions or add retries just to pass. Temporary quarantine requires
a reason, tracked follow-up, owner, restoration condition, and explicit reporting
of lost coverage.
