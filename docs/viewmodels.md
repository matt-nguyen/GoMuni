# ViewModels

Apply these rules to new ViewModels and authorized refactors; preserve intended
behavior during fixes. [UI state](ui-state.md) owns state shape/naming,
presentation, delivery, and restoration.

## Structure and initialization

- Expose one typed `onAction`; it only dispatches to named private handlers, even
  for simple actions. Handlers validate prerequisites and own operations/updates.
  Avoid central reducer-style `current -> next -> commit` implementations.
- Keep mutable flows private. Follow UI-state rendering/effect contracts. Derived
  repository flows are allowed with deliberate sharing/restart policy so collection
  never repeats commands accidentally.
- Inject domain contracts. Repositories own authoritative data; use cases own
  meaningful business orchestration. Simple operations may call repositories.
  Never retain Activity, launcher, navigation stack, or UI controller references.
- Default to one ViewModel per destination entry, acquired by its route. Document
  shared scopes/participants/cleanup and visibility-dependent work separately.
- Validate/restore inputs before work. Explicitly supply destination inputs rather
  than assuming keys populate `SavedStateHandle`. Choose eager loading versus
  subscription-driven observation; recomposition/collector restart must not repeat
  initialization.

## Coroutines and request patterns

Launch owned work in `viewModelScope`; no GlobalScope/unmanaged work. Clearing the
ViewModel cancels it; hiding a retained entry does not. Use atomic updates or
serialization where needed and keep update lambdas effect-free. Data/domain APIs
must be main-safe; the blocking/expensive owner switches dispatchers, injected as
needed. See [Architecture](architecture.md#work-lifetimes-and-di).

Choose replace, ignore duplicates, queue, or independent execution per operation:

| Trigger | Pattern |
| --- | --- |
| Explicit replaceable one-shot action | One private Job; cancel it and launch the replacement. No generation/request ID by default. |
| UI-state-driven input | Update input, observe its selected value, `distinctUntilChanged`, then `flatMapLatest`. |
| External callbacks/recoverable requests | Correlate identity/phase where cancellation cannot control delivery. |

Both replacement patterns require cooperative cancellation without detached callbacks.
For one-shot work, keep call/publication sequential on Main; `ensureActive` before
publication with no intervening suspension. Completed Jobs may stay in their field.
Cancelled cleanup must not clear newer loading/job state. Use `cancelAndJoin` only
when prior cleanup must finish first.

For input-driven work, observe only relevant input so result updates cannot retrigger
requests. Optional debounce delays replacement until settled input reaches
`flatMapLatest`; `buffer(0)` avoids queued inner results. Handle expected failures
inside each request so observation continues; preserve cancellation. The observer
processes initial input and runs while hidden unless narrowed. Guard blank inputs
as needed. Use derived `stateIn` for subscription-driven work, avoiding circular
observation of its own output.

Preserve `CancellationException`, including broad catches/`runCatching`. Stale work
must not overwrite current state or clear its loading flag. Map expected failures
to meaningful UI errors; define reporting/safe handling of unexpected exceptions.
Choose `stateIn` initial/sharing/restart behavior deliberately; stopping collection
does not stop independently launched Jobs.

## Platform work and effect collection

Routes execute UI/platform requests and return correlated results through actions;
handlers validate them. The navigation owner applies navigation. Keep saved-state
keys/serialization in focused helpers and restore inputs before work. Durable work
needs a background owner; `onCleared` cannot guarantee critical persistence.

Collect effects once at the route with entry lifecycle awareness, RESUMED for
interactive navigation, and current callbacks via `rememberUpdatedState`. Launch
snackbar work as a child of the resumed scope so dismissal does not block navigation;
leaving RESUMED cancels collection/children. Screens report actions, never collect
effects. The host checks originating-entry eligibility.

Lossy transient streams may discard effects without collectors or on overflow,
including navigation. Choose the buffer policy according to each feature's
delivery contract.

## Verification

Every ViewModel requires observable state/output tests. Cover relevant initial/
restored state, preconditions, outcomes/retry, duplicates, stale/out-of-order results,
cancellation/cleanup, acknowledgments, effects/loss/overflow, visibility, and collector
restart. Use `runTest`, controlled domain fakes, and a rule-managed Main dispatcher
sharing the scheduler. Collect subscription-driven flows; test debounce and no result-
triggered request loops. Verify changed Android recreation contracts on devices;
unit reconstruction is insufficient. Identify owners/lifetimes before refactoring,
record exceptions, and report unverified behavior. Follow [testing](testing.md).
