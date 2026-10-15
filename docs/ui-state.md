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

This document owns state shape, naming, presentation policies, effect delivery,
and restoration contracts. [ViewModels](viewmodels.md) owns handlers, coroutine
implementation, initialization, and ViewModel testing. Feature documents record
application-specific decisions and exceptions.

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

## State contracts

Expose rendering data as a read-only `StateFlow<FeatureUiState>`. `FeatureUiState`
is an immutable data class containing only rendering inputs. Group values by
responsibility and lifetime rather than collecting unrelated state into one model.

Keep workflow progress and pending platform requests separate by default. Model
necessary operation phases in their operation owner and derive busy/error
presentation from that owner.

- Use nested sealed types or enums for mutually exclusive sub-UI modes, such as
  loading/content/error or editing states. Do not model the top-level rendering
  contract as a sealed workflow.
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

StateFlow represents the latest snapshot and may conflate intermediate values; do
not use rendering-state changes as a command queue. Derive rendering state from
its authoritative inputs without introducing independently mutable copies.

ViewModel production, sharing, and coroutine implementation follow
[Coroutines and concurrency](viewmodels.md#coroutines-and-concurrency), including
[UI-state-driven requests](viewmodels.md#ui-state-driven-requests-flatmaplatest).
Lifecycle-aware route collection follows [Compose conventions](compose.md#effects-and-lifecycle-integration).

## Actions and transitions

Callbacks express user intent, such as changing a filter, selecting an item, retrying,
or submitting. ViewModel input dispatch and validation follow
[the ViewModel implementation checklist](viewmodels.md#implementation-checklist).
Purely local presentation interactions belong to their Compose state owner.

For each important action, define its preconditions, resulting state, asynchronous
work, success/failure behavior, and repeated-action policy. State updates should
leave the model valid even when operations overlap or complete out of order.

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

A delayed result must not overwrite a newer selection, error, or loading state, or
trigger an effect for an obsolete interaction. Operation cancellation and request
implementation belong to [ViewModel concurrency](viewmodels.md#coroutines-and-concurrency),
with separate [one-shot](viewmodels.md#one-shot-requests-cancel-and-replace) and
[UI-state-driven](viewmodels.md#ui-state-driven-requests-flatmaplatest) examples.
External request correlation follows the delivery contract below.

## Naming

| Contract | Name | Examples |
| --- | --- | --- |
| ViewModel inputs | `<Feature>Action` | `StopsAction.StopSelected`, `Refresh` |
| Rendering data | `<Feature>UiState` | `StopsUiState` |
| Transient outputs | `<Feature>Effect` | `StopsEffect` |
| Navigation effects | `NavigateTo<Destination>`, `NavigateBack` | `NavigateToArrivals(stopId)` |
| UI effects | `Show<Meaning>`, `Request<Interaction>` | `ShowStopSelectionHint`, `RequestSearchFocus` |
| Recoverable requests | `<Feature>Request` | `TrackingRequest` |

Name actions reporting occurrences in past tense (`StopSelected`, `PermissionResolved`);
commands may use imperative names (`Refresh`, `StartTracking`). Effects use verb-led
names describing the requested behavior. Name UI effects by meaning so routes choose
the surface and localized text. Avoid `Event` as a contract suffix: it obscures input
versus output. Expose `effects` backed by private `_effects`.

## State and one-time effects

Keep rendering state separate from execution requests. Expose one typed
`<Feature>Effect` hierarchy through one read-only `effects` flow for transient
navigation and UI effects whose loss is acceptable, such as a nonessential snackbar
or immediate, repeatable navigation interaction. Keep acknowledged pending requests
separate when loss would interrupt the user's task or recovery is required.

This stream convention is project policy. Android's general UI-event guidance
prefers reducing ViewModel-originated events to state, particularly where delivery
matters; the transient-stream exception requires explicit loss semantics.
[Android UI event guidance](https://developer.android.com/topic/architecture/ui-layer/events)

For transient streams, define buffering, replay, inactive-consumer behavior, and
one logical consumer for the combined stream. UI effects must not block navigation;
choose overflow behavior deliberately because UI traffic can displace navigation.
Collect at a lifecycle-aware route boundary; see the
[collection example](viewmodels.md#collecting-effects-in-the-route). Do not assume
`Channel` or `SharedFlow` provides exactly-once processing or process-death recovery.
Avoid replaying a completed effect when a collector restarts.

For acknowledged effects:

- Give the pending request a stable identity and a defined phase.
- Save the minimum reconstructible request if process recovery is required.
- Specify whether acknowledgment means accepted, launched, completed, or dismissed.
- Accept results only for the matching request identity and operation phase;
  an old acknowledgment must not clear a newer request.
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
exists. Never restore completed effects as pending. Apply the acknowledgment and
uncertain-outcome rules above before retrying. Saved state is not durable storage
or an exactly-once guarantee. Follow [Navigation](navigation.md#state-restoration)
for back-stack restoration and [ViewModels](viewmodels.md#platform-integration)
for saved-state implementation.

## Feature contracts and verification

Feature documentation records owners, initial state, invariants, presentation
policies, and effect/restoration semantics. Record exceptions with their rationale.
Verify meaningful rendering states and interactions at the UI boundary, and effect
loss/replay and recreation behavior at their integration boundary. ViewModel tests
follow the [verification checklist](viewmodels.md#verification-checklist). Report
material unverified behavior.

## References

These conventions prescribe project defaults rather than copying every Android
recommendation. Consult official documentation for the installed library versions:

- [UI state production](https://developer.android.com/topic/architecture/ui-layer/state-production)
  explains state production and resource-aware pipelines.
- [Compose state](https://developer.android.com/develop/ui/compose/state)
  covers state observation, hoisting, and saveable presentation state.
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)
  explains event handling and state-based delivery.
