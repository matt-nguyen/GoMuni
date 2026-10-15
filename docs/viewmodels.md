# ViewModel conventions

Apply these rules to new ViewModels and authorized refactors. Preserve intended
behavior during fixes; do not expand unrelated changes into architectural migrations.

This document owns ViewModel structure, initialization, coroutine execution, and
ViewModel test patterns. [UI state](ui-state.md) owns state shape, naming, presentation
policies, effect delivery, and restoration contracts. Examples here illustrate those
contracts; do not redefine them here.

## Implementation checklist

- Expose one typed `onAction` entry point.
  `onAction` only dispatches to named private handlers, including for simple actions.
  Handlers validate prerequisites and own their operations and state updates.
- Expose state and outputs according to [state contracts](ui-state.md#state-contracts)
  and [effect delivery](ui-state.md#state-and-one-time-effects). Keep mutable backing
  flows private. Follow the shared [naming conventions](ui-state.md#naming).
- Use private handlers rather than a central reducer-style `current -> next -> commit`
  implementation. Derived repository flows are allowed; configure their sharing and
  restart behavior deliberately so collection does not repeat commands.
- Inject domain contracts. Keep authoritative data in repositories and meaningful
  business orchestration in use cases. Simple operations may call repositories
  directly. Do not retain Activities, launchers, navigation stacks, or UI controllers.
- Default to one ViewModel per destination entry, acquired by its route. Document
  shared scope owners, participants, and lifetimes. Creation does not imply visibility;
  define when polling and other visibility-dependent work starts and stops.
- Validate supplied arguments and restore essential inputs before starting work.
  Do not depend on recomposition to trigger initialization or assume navigation keys
  automatically populate `SavedStateHandle`. Choose initial loading or subscription-driven
  observation explicitly; collector restarts must not repeat initialization.

## Coroutines and concurrency

- Launch ViewModel-owned asynchronous work in `viewModelScope`; do not use
  `GlobalScope` or unmanaged scopes. Clearing the ViewModel cancels this work;
  hiding a retained destination does not. Define visibility-dependent work separately.
- Use atomic updates or explicit serialization where needed.
- Suspend functions do not automatically run in the background. Data/domain APIs
  must be main-safe; the class doing blocking I/O or expensive computation owns the
  dispatcher switch. Inject dispatchers where needed for testing; see
  [Architecture](architecture.md#coroutines-dispatchers-and-work-lifetimes).
- Choose concurrency per operation: replace, ignore duplicates, queue, or independent.
  For replaceable one-shot work, cancel the private Job and launch the new request;
  do not add request IDs or generation counters by default. For UI-state-driven work,
  select the relevant input, apply `distinctUntilChanged`, and use `flatMapLatest`.
  Both patterns require cancellation-cooperative APIs and no detached callbacks.
  Correlate external callbacks or recoverable requests when cancellation cannot
  control their delivery. State-update lambdas must be effect-free.
- Preserve `CancellationException`, including in broad catches and `runCatching`.
  Cancellation must not become an error or overwrite newer operation state. Only the
  current operation may clear its loading state during cleanup.
- Map expected domain failures to meaningful UI errors. Define where unexpected
  exceptions are reported and safely handled; never silently convert every exception
  into an ordinary failure. Follow [failure boundaries](architecture.md#failures-across-boundaries).
- Choose `stateIn` initial values and sharing/restart policy deliberately, using
  `SharingStarted.WhileSubscribed` when appropriate. Stopping UI collection does not
  automatically stop independently launched jobs. See [state production](ui-state.md#state-production-and-collection).

## Platform integration

Publish requests using the [delivery contract](ui-state.md#state-and-one-time-effects)
and [restoration rules](ui-state.md#restoration). Routes execute platform/UI work and
return results through `onAction`; private handlers validate them before updating
operations. The navigation owner applies navigation requests; see
[Navigation](navigation.md#viewmodel-navigation-events).

Keep saved-state keys and serialization in focused private helpers. Restore inputs
before launching work. Work that must outlive destination removal belongs to a
background owner; do not rely on `onCleared` to finish or persist critical work.

## Main ViewModel example

Keep the main structure small: rendering state, one effects flow, and private action
handlers. Imports, DI, and screen rendering are omitted. Request patterns below are
separate extensions to this example, not steps to combine into one implementation.

```kotlin
data class StopsUiState(
    val searchQuery: String = "",
    val stopIds: List<String> = emptyList(),
    val searchUnavailable: Boolean = false,
)

sealed interface StopsAction {
    data class SearchQueryChanged(val query: String) : StopsAction
    data class StopSelected(val stopId: String) : StopsAction
}

sealed interface StopsEffect {
    data class NavigateToArrivals(val stopId: String) : StopsEffect
    data object ShowStopSelectionHint : StopsEffect
}

class StopsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StopsEffect>(
        replay = 0, extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects = _effects.asSharedFlow()

    fun onAction(action: StopsAction) {
        when (action) {
            is StopsAction.SearchQueryChanged -> updateSearchQuery(action.query)
            is StopsAction.StopSelected -> selectStop(action.stopId)
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun selectStop(stopId: String) {
        if (stopId.isBlank()) {
            _effects.tryEmit(StopsEffect.ShowStopSelectionHint)
            return
        }
        _effects.tryEmit(StopsEffect.NavigateToArrivals(stopId))
    }
}
```

## One-shot requests: cancel and replace

For an explicit request action, keep one private Job per replaceable operation.
The handler cancels the previous job and launches the new request in `viewModelScope`.
No request identity or generation counter is needed. This snippet assumes an injected,
main-safe repository returning typed `StopSearchResult.Success` or `Unavailable`.
The action dispatches to `searchStops`; do not also install the observer below.

```kotlin
private var searchJob: Job? = null

private fun searchStops(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        val result = repository.search(query)
        ensureActive()
        _uiState.update {
            when (result) {
                is StopSearchResult.Success -> it.copy(
                    stopIds = result.stopIds.toList(), searchUnavailable = false,
                )
                StopSearchResult.Unavailable -> it.copy(searchUnavailable = true)
            }
        }
    }
}
```

Keep calls and publication sequential on Main, with no suspension between the
cancellation check and state update. A completed Job may stay in the private field;
the next request can cancel it harmlessly. Do not unconditionally clear shared
loading flags or job fields in a cancelled job's `finally` block. Use `cancelAndJoin`
inside structured orchestration only when old cleanup must finish before new work.

## UI-state-driven requests: flatMapLatest

When an input such as search text drives requests, the handler only updates UI state.
Start one observer in the ViewModel; select and deduplicate that input so publishing
results does not trigger another request. `flatMapLatest` cancels the previous inner
flow and starts the latest request without a manually managed Job.

```kotlin
init {
    observeSearchQuery()
}

private fun observeSearchQuery() {
    _uiState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .debounce(300) // Optional: wait for the input to settle.
        .flatMapLatest { query ->
            flow { emit(repository.search(query)) }
        }
        .buffer(0)
        .onEach { result ->
            _uiState.update {
                when (result) {
                    is StopSearchResult.Success -> it.copy(
                        stopIds = result.stopIds.toList(), searchUnavailable = false,
                    )
                    StopSearchResult.Unavailable -> it.copy(searchUnavailable = true)
                }
            }
        }
        .launchIn(viewModelScope)
}
```

Remove `debounce(300)` for immediate requests. With debounce, the previous request
continues until the settled input reaches `flatMapLatest`, which cancels and replaces
it. `buffer(0)` avoids queued inner results. These operators
may require coroutine opt-ins for the installed version. Handle expected failures
within each request so the observer can process future inputs; do not swallow
cancellation. This observer runs for the ViewModel lifetime, including while hidden,
and processes the initial query. Guard blank input or apply a narrower lifetime if
required. Use a derived `stateIn` pipeline instead when subscription-driven work is
intended; do not observe a UI state that itself depends on that same pipeline.

## Collecting effects in the route

The example uses a lossy transient stream: no replay, no retention without a
collector, and oldest-effect dropping on overflow. Even navigation may be dropped.
This illustrates the [transient delivery contract](ui-state.md#state-and-one-time-effects)
for immediate, repeatable interactions; it is not the default buffer policy for every
feature. The navigation owner handles repeated taps and destination duplication.

Collect once at the entry's route boundary. Launch snackbar work in the resumed
scope so waiting for dismissal does not block subsequent navigation.

```kotlin
// Inside the route; selectStopMessage is resolved with stringResource(...).
// onNavigate accepts a stop ID and forwards intent to the navigation owner.
val lifecycle = LocalLifecycleOwner.current.lifecycle
val currentNavigate by rememberUpdatedState(onNavigate)
val currentMessage by rememberUpdatedState(selectStopMessage)
LaunchedEffect(viewModel, lifecycle, snackbarHostState) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StopsEffect.NavigateToArrivals -> currentNavigate(effect.stopId)
                StopsEffect.ShowStopSelectionHint -> launch {
                    snackbarHostState.showSnackbar(currentMessage)
                }
            }
        }
    }
}
```

The flow has one logical consumer. Leaving RESUMED cancels collection and snackbar
children; repeated hints queue through SnackbarHostState while resumed. The navigation
owner checks the originating entry before changing history. Screens send actions
through `onAction` and do not collect effects.

## Verification checklist

- Every ViewModel requires tests of observable state and outputs, not private handlers.
  Verify emitted effect types and payloads, including the chosen loss/overflow policy.
- Cover initial/restored state, action preconditions, success/failure, and retry.
  Include duplicates, stale results, cancellation, pending-request acknowledgment,
  and visibility/collector restart behavior where applicable.
- Use `runTest`, controllable domain fakes, and a test Main dispatcher (set/reset
  through a rule) sharing the test scheduler. Test cancellation and out-of-order
  completion, including stale cleanup. Test debounce timing and that result updates
  do not retrigger input observers. Collect subscription-driven flows in tests. Verify
  Android recreation on a device/emulator when that contract changes; unit
  reconstruction alone does not prove platform recovery.
- Before refactoring, identify state owners, consumers, and work lifetimes. Run relevant
  checks and report material unverified behavior. Record feature-specific exceptions
  with their rationale; do not silently weaken these rules.

## Related guidance

- [Architecture](architecture.md): dependencies, domain contracts, failures, and background work.
- [UI state](ui-state.md): state production, concurrency, effect delivery, and restoration.
- [UI architecture](ui-architecture.md): route integration and platform effect execution.
- [Navigation](navigation.md): entry scope, eligibility, history, and navigation requests.
