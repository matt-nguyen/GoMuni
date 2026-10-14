# ViewModel conventions

## Scope

These reusable conventions prescribe ViewModel implementation and lifecycle
contracts for Android applications using Compose and MVVM. They do not inventory
a particular application's ViewModels. Apply them to new work and authorized
refactors; preserve intended behavior and avoid unrelated migrations during fixes.

Use these companion documents for detailed rules:

- [Architecture](architecture.md): layer dependencies, repository/use-case contracts,
  failures, dispatcher ownership, and background work.
- [UI state](ui-state.md): state models, loading/error policies, effect delivery,
  asynchronous results, and restoration.
- [Navigation](navigation.md): destination scope and navigation event handling.
- [UI architecture](ui-architecture.md): route integration, typed screen actions,
  presentation state, and effect execution.

## Responsibility and scope

A ViewModel owns a destination's screen state and coordinates its actions with
domain operations. It is not an application-wide state container, a navigation
host, or a platform controller.

Default to a ViewModel per destination entry. Acquire it at the route boundary
under the configured entry-owned store. Shared scopes require a documented owner,
participants, lifetime, and reason. Do not scope by destination type alone when
multiple entries must retain independent state.

Keep authoritative shared data in repositories. Keep business rules and meaningful
business orchestration in domain use cases. Direct calls to domain repository
interfaces are allowed for simple operations. Route wrappers own presentation
controllers and execute UI/platform effects.

ViewModels must not retain Activities, composables, launchers, navigation stacks,
scaffold controllers, or other UI objects. Inject narrow domain contracts and
testable dependencies rather than locating them through global access.

## Public contract

Expose a read-only `StateFlow<FeatureUiState>` and one typed
`onAction(action: FeatureAction)` entry point. Keep mutable state and operation
helpers private. Screen callbacks use the feature action contract; route wrappers
handle presentation-only actions locally and forward ViewModel-owned actions.

Actions express intent and carry minimal values or identities. ViewModel-originated
navigation and effect requests use separate typed outputs with the delivery
contracts defined in the companion documents. Do not encode UI objects or callbacks
inside requests.

Document supported actions, initial state, essential invariants, and output delivery
semantics. Validate actions against current state even when UI controls are disabled.
Avoid exposing mutable collections, job handles, or internal reducers to callers.

## Initialization and inputs

Initialize argument-driven work in the ViewModel, using validated inputs supplied
by its factory or saved-state setup. Restore essential inputs before starting work.
Do not require a route to repeatedly invoke a load command merely because it recomposes.

Specify whether initialization performs a one-shot load, starts observation, or
builds a subscription-driven state pipeline. ViewModel creation is not equivalent
to visibility; expensive work may need to wait for an explicit visibility signal.

When work depends on active UI participation, the route sends explicit lifecycle
or visibility actions. Define repeated-signal behavior, start/stop ownership, and
resume policy. Avoid duplicate observers and repeated commands when collection
restarts. Do not pass a `LifecycleOwner` into the ViewModel or make it infer activity
from the presence of a subscriber alone.

Destination identity normally remains fixed for an entry. Treat changes to query,
filter, or selection as explicit input transitions, with documented cancellation
and content-retention policies. If arguments can change for a retained instance,
define how every dependent operation and saved input is reconciled.

## Internal implementation choices

There is no required reducer, actor, or state-machine framework. Choose a structure
that makes the feature's transitions and concurrency understandable:

| Approach | Useful when | Responsibility to preserve |
| --- | --- | --- |
| Private handlers and controlled state updates | Operations are few and their interactions are straightforward | Atomic updates and explicit operation ownership |
| Derived flows combining inputs and repositories | State is primarily a projection of observable sources | Deliberate sharing, initial values, and restart behavior |
| Pure reducer with separate work execution | Many transitions need consistent rules | Reducers stay side-effect-free; results remain correlated with requests |
| Explicit workflow/state machine | Phases and legal transitions matter | Valid transitions, phase payloads, and recovery rules |

These approaches may coexist for different concerns. Extract focused helpers when
they clarify a responsibility; do not move unrelated operations into a generic
manager just to shorten the ViewModel. Pure helpers must not acquire hidden scopes
or perform platform effects.

Follow ui-state.md for immutable snapshots and derived values. Keep one authoritative
owner per input, and avoid copying repository data into independently mutable state.

## Actions, concurrency, and results

Specify a policy per asynchronous operation rather than globally serializing every
action:

| Policy | Typical use | Required behavior |
| --- | --- | --- |
| Cancel/replace | New query or selection supersedes earlier work | Cancel old work and reject stale success, failure, and cleanup results. |
| Ignore duplicates | Refresh or submit is already in progress | Define which request counts as a duplicate and when another becomes eligible. |
| Queue | Ordered operations must all run | Bound or otherwise control pending work; define cancellation and failure behavior. |
| Independent | Operations do not supersede one another | Isolate operation state and reconcile updates to shared fields. |

Use structured concurrency in the ViewModel's scope. Make operation ownership
visible through request identity, arguments, or generation tracking as appropriate.
Cancellation alone does not guarantee an old result cannot arrive. An obsolete
operation must not clear a newer loading flag, replace newer input, or emit an
effect for an abandoned interaction.

Use atomic state updates or serialized transitions where needed. Update lambdas
and reducers must not launch work or emit effects if they can be evaluated again.
Keep operation outcomes distinct from rendering snapshots; StateFlow can conflate
intermediate values and is not a command queue.

Map expected domain failures to the feature's presentation policy. Preserve coroutine
cancellation, and handle unexpected exceptions at an explicit boundary without
silently converting them into success. Main-safety and dispatcher switching belong
to the operation implementation as defined in architecture.md.

## Navigation and effects

ViewModels emit navigation intent rather than mutating history. Routes collect
requests and the navigation owner applies them according to navigation.md, including
active-entry eligibility, duplication, and stale-request policies.

Use a dedicated stream for transient effects whose loss is acceptable, and acknowledged
pending state for effects requiring recovery, as defined in ui-state.md. Document
buffering, replay, consumption, and inactive-consumer behavior. Streams alone do not
guarantee exactly-once delivery or process recovery.

Routes execute permissions, external operations, and UI effects. Return correlated
results as actions; verify their request identity and phase before accepting them.
An old acknowledgment must not clear a newer request. Do not expose a public method
that allows callers to replace arbitrary internal state in order to acknowledge work.

## Saved state and recovery

Inject `SavedStateHandle` when the ViewModel owns restorable inputs or pending
requests. Define keys, value types, defaults, and validation in one discoverable
place. Navigation keys are not assumed to populate saved state automatically.

Save minimal reconstruction inputs, such as a query, selected identity, or pending
request identity and phase. Avoid saving complete datasets, coroutine jobs, raw
exceptions, or platform objects. Keep durable user data and operations in appropriate
repository persistence or background owners.

On system recreation, rebuild state from saved inputs and authoritative data.
Revalidate selections and reconcile pending operations before retrying. Do not
restore a loading flag for work that no longer exists or replay completed navigation
and external operations. Define how uncertain external outcomes are resolved.

SavedStateHandle supports saved-instance recovery, not a guarantee that data is
durable across force stop, task removal, or every process interruption. Writes
must participate in the host's save lifecycle. Use durable persistence when the
workflow requires stronger guarantees.
[Android saved-state guidance](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)

## Cleanup and work outside the screen

Destination removal clears its ViewModel scope; temporary invisibility does not.
Release any independently acquired resources using the relevant lifecycle or
closeable mechanism. Do not rely on `onCleared` to persist critical data or complete
a network operation: process termination may not run that cleanup.

Polling and subscriptions need explicit visibility/lifetime policies. Work that must
outlive destination removal belongs to a documented background owner. ViewModels
observe that owner's status and issue commands through contracts rather than
becoming the durable owner themselves.

## Testing and refactoring

Test public state and output behavior with domain fakes, controlled coroutine
dispatchers, and a deterministic scheduler. Supply saved state when testing
reconstruction. Account for subscription-driven pipelines by collecting their state
in tests; do not assume an uncollected derived flow starts upstream work.

Cover cases relevant to the feature:

- Initialization with valid, invalid, missing, and restored inputs.
- Action preconditions and expected state changes.
- Success/failure and retry under the chosen data-retention policy.
- Duplicate actions, overlapping work, cancellation, and out-of-order completion.
- Navigation/effect output and stale or repeated acknowledgment.
- Visibility transitions, collector restart, and interrupted-work recovery.

Assert meaningful snapshots and invariants rather than every intermediate emission,
private method call, or reducer shape. Unit reconstruction tests do not prove Android
saved-state integration; use platform verification where that contract changes.

Before splitting a ViewModel, map its state, inputs, operations, outputs, consumers,
and work lifetimes. Assign each to its correct owner and identify shared data or
long-lived work. Preserve intended behavior unless the task authorizes changes.
Report relevant checks and any unverified lifecycle/recovery cases.

## References

These are project defaults; internal implementation patterns remain optional.

- [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
  explains lifecycle-aware state ownership.
- [ViewModel factories](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories)
  explains supplying dependencies and saved-state support.
- [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
  explains recovery inputs and saved-state limitations.
