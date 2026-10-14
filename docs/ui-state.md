# UI state conventions

## Scope

These are reusable conventions for Android applications using Compose and MVVM.
They prescribe state contracts, ownership, and delivery behavior; they do not
describe a particular application's current implementation. Keep feature-specific
state models, policies, and accepted exceptions in feature documentation or
decision records. Navigation events and destination lifetimes are governed by
[Navigation conventions](navigation.md).

Apply these conventions to new work and authorized refactors. Inspect existing
behavior before changing it; do not turn an unrelated bug fix into a state migration.

## Ownership and data flow

Use unidirectional data flow: state flows to the screen, actions flow to its owner,
and the owner produces updated state. Give each value one authoritative owner.

| State | Default owner |
| --- | --- |
| Screen inputs, business decisions, operation state, and screen-level selection | Destination ViewModel |
| Local presentation state such as focus, animation, or component expansion | Route-owned Compose state or a hoisted UI state holder |
| Authoritative shared data and durable settings | Repository and appropriate persistence |
| Navigation history and arguments | Navigation owner, as defined in navigation.md |

Move presentation state to the ViewModel when screen logic needs it, it coordinates
multiple components, or its lifetime requires that ownership. Hoist local state to
the route boundary as defined in [UI architecture](ui-architecture.md). Restoration needs alone do not require
moving all presentation state into a ViewModel.

Screens receive relevant immutable state and action callbacks. They do not obtain
ViewModels, collect repositories, or perform business operations during rendering.
Route wrappers collect state and connect callbacks to actions. Reusable components
receive the subset of data and callbacks they need.

## Modeling state

Use an immutable data class for the screen's UI state, with explicit fields and
nested types where useful. Group values by responsibility and lifecycle rather
than collecting unrelated state into one application-wide model.

- Use nested sealed types or enums for mutually exclusive modes, selections,
  workflow phases, editing states, or operation outcomes. This is not limited to
  loading, content, and error states.
- Put phase-specific payloads inside their phase where that prevents invalid
  combinations. Avoid independent flags that allow contradictory states.
- Keep independent concerns independent: content, refresh, selection, and a
  submission operation may coexist if the feature supports that combination.
- Distinguish not-yet-loaded from successfully loaded but empty. A default empty
  collection alone must not imply successful loading.
- Represent partial success, unavailable data, permissions, or offline status
  explicitly when the screen behaves differently for those conditions.
- Store stable identities for selection where possible; derive selected content
  from authoritative data. Define behavior when a selected item disappears.
- Derive values such as counts and action availability rather than maintaining
  redundant fields that can drift. Store a projection only when its owner keeps
  it consistent and there is a concrete need.

Immutability includes nested models and collections. Read-only collection types
do not guarantee immutable backing data; do not mutate objects after publication.
Publish new snapshots when values change. An annotation such as `@Immutable` is
a promise to Compose, not a way to make mutable objects safe.

Define a valid initial state and invariants for every model. Use meaningful error
categories or presentation models with recovery information; do not expose raw
exceptions or sensitive technical messages as user-facing state. Resolve localized
display text at an appropriate UI boundary.

## State production and collection

Expose a read-only `StateFlow<FeatureUiState>` from the ViewModel. Keep mutable
flows private. Produce state by combining authoritative sources with owned inputs
and operation state. Use `stateIn` for derived flows where appropriate, or private
mutable state with controlled updates for imperative workflows.

Define the initial value, sharing policy, and restart behavior deliberately.
Subscription-driven production must not accidentally repeat commands when a
collector returns. Avoid copying repository data into a second mutable source of
truth solely to expose it to the UI.

Route wrappers collect using `collectAsStateWithLifecycle`. Lifecycle-aware
collection does not automatically stop every ViewModel job or upstream producer;
define production and work lifetimes separately. Keep computation and blocking
work off the main thread using the appropriate data-layer or coroutine mechanisms.

Use atomic updates when concurrent operations can change owned state, and serialize
transitions that must occur together. Do not put effects inside an update lambda
that may execute more than once. StateFlow represents the latest snapshot and can
conflate intermediate values; do not rely on observing every state change to
deliver an event or trigger an essential operation.

## Actions and transitions

Callbacks express user intent, such as changing a filter, selecting an item, retrying,
or submitting. Use typed actions or explicit ViewModel methods consistently within
the feature. The owner validates the action against current state; disabling a
button in the UI is not sufficient protection against duplicate requests.

For each important action, define its preconditions, resulting state, asynchronous
work, success/failure behavior, and repeated-action policy. State updates should
leave the model valid even when operations overlap or complete out of order.
Purely local presentation actions may remain in their Compose owner.

## Loading, refresh, and error policies

Each feature chooses and documents its policy. The following are common scenarios,
not a mandatory visual pattern:

| Scenario | Common state and presentation choices |
| --- | --- |
| Initial load without usable data | Show an initial loading state; failure can replace it with an error and recovery action. |
| Successful empty result | Show an empty state with context or a next action; distinguish it from loading or failure. |
| Refresh with usable data | Retain data with a refresh indicator, or temporarily replace it when stale content would mislead the user. |
| Refresh failure with retained data | Keep content and expose a nonblocking error, freshness information, or retry action. |
| Cached or offline content | Represent availability and freshness separately; define whether refresh is automatic, manual, or unavailable. |
| Partial or independently loaded content | Use section-level operation states so one failure does not necessarily replace the entire screen. |
| Submission or mutation | Track the operation separately from content; define disabled actions and whether changes are optimistic or await confirmation. |
| Optimistic update failure | Roll back, reconcile, or mark the change pending according to the feature's documented policy. |

Document what happens to existing data when an operation starts, whether concurrent
refreshes are ignored, cancelled, queued, or coalesced, and when an error is cleared.
Retry must target the failed operation and its current arguments. Define whether a
new query or resource identity retains previous content and, if so, how the UI makes
its relationship to the new request clear.

## Asynchronous work and stale results

Scope screen work to its ViewModel and define any narrower visibility or request
lifetime. Work that must survive destination removal needs an appropriate owner
outside that screen.

Cancel obsolete jobs when inputs change or newer work supersedes them. Also verify
request identity, generation, or current arguments before accepting results; cancellation
alone does not prove that an outdated callback or response cannot arrive. Apply this
check to success, errors, and cleanup so old work cannot clear a newer loading state.

Preserve coroutine cancellation rather than converting it into a user-visible error.
Define retry, debounce, polling, and duplicate-action policies per operation. A delayed
result must not update a different selection, overwrite newer user input, or trigger
an effect for an obsolete interaction.

## State and one-time effects

Use state for information needed to faithfully render or resume the screen. Use a
dedicated typed effect stream for transient actions whose loss is acceptable under
the feature's delivery policy, such as a nonessential snackbar. Use acknowledged
pending state for effects that must survive recreation or whose loss would leave
the workflow incomplete.

This stream convention is project policy. Android's general UI-event guidance
prefers reducing ViewModel-originated events to state, particularly where delivery
matters; the transient-stream exception requires explicit loss semantics.
[Android UI event guidance](https://developer.android.com/topic/architecture/ui-layer/events)

For transient streams, define buffering, replay, inactive-consumer behavior, and
one logical consumer. Collect at a lifecycle-aware route boundary. Do not assume
`Channel` or `SharedFlow` provides exactly-once processing or process-death recovery.
Avoid replaying a completed effect when a collector restarts.

For acknowledged effects:

- Give the pending request a stable identity and a defined phase.
- Save the minimum reconstructible request if process recovery is required.
- Specify whether acknowledgment means accepted, launched, completed, or dismissed.
- Correlate results and acknowledgment with the request identity; an old result
  must not clear a newer request.
- Reconcile pending requests with external operation state before retrying.
  Define idempotency, duplicate prevention, or recovery when completion is uncertain.

Permission prompts and external app launches execute at the UI/platform boundary.
Persist their workflow phase when required and account for Activity Result recovery;
recomposition alone must not relaunch them. A UI state field is not durable unless
the necessary inputs are actually saved. Navigation effects follow navigation.md.

## Restoration

Define which inputs, drafts, filters, selections, and pending operations survive
configuration changes, system process recreation, and a fresh launch. These are
different lifetimes; saved instance state is not a replacement for durable storage.

Use saveable Compose state for small restorable presentation values and
`SavedStateHandle` for essential ViewModel inputs or pending requests. Use repository
persistence for durable user data and work. Save minimal reconstruction inputs
rather than full datasets, exceptions, or platform objects.

Reload authoritative data and revalidate restored identities. Reconstruct operation
state instead of restoring a permanent loading flag for a coroutine that no longer
exists. Define how interrupted submissions or external operations are reconciled
before retrying. Follow navigation.md for back-stack and entry restoration.

## Feature contracts and verification

Feature documentation should identify the state owner, initial state, invariants,
actions, common transitions, loading/error policies, effect delivery contract, and
restoration behavior. Record accepted exceptions with their rationale.

Before a refactor, map existing state to its owners and consumers. Preserve intended
user-visible behavior unless the task authorizes a change. For a bug, trace the action,
operation identity, state transition, and rendered result before changing the model.

Verify behavior at the relevant boundary:

- Valid initial, empty, content, partial, and failure states supported by the feature.
- Refresh and retry behavior with and without existing data.
- Repeated actions, concurrent operations, cancellation, and out-of-order results.
- Selection changes or removal while requests are pending.
- Effect loss/replay policy, acknowledgment, and stale-result correlation.
- Collector restart, configuration changes, and process recovery where required.

Test observable outcomes and invariants rather than every transient emission or
private implementation detail. Use screen previews or UI tests for meaningful
rendering states. Report checks performed and any unverified recovery behavior.

## References

These conventions prescribe project defaults rather than copying every Android
recommendation. Consult official documentation for the installed library versions:

- [UI state production](https://developer.android.com/topic/architecture/ui-layer/state-production)
  explains state production and resource-aware pipelines.
- [Compose state](https://developer.android.com/develop/ui/compose/state)
  covers state observation, hoisting, and saveable presentation state.
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)
  explains event handling and state-based delivery.
