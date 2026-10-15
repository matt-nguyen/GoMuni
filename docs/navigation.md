# Navigation conventions

## Scope

These are reusable conventions for Android applications using Jetpack Compose
and Navigation 3. They prescribe architectural boundaries and behavior; they do
not describe any application's current destinations or certify that existing
code follows them. Keep application-specific flows and accepted exceptions in
separate feature documentation or decision records.

Apply these conventions to new work and navigation refactors. For bug fixes,
preserve the affected flow's intended behavior. Do not expand an unrelated change
into a navigation migration.

## Destination boundaries

A destination represents a user task or place with meaningful navigation history,
entry arguments, and a defined Back behavior. Loading, empty, content, refresh,
and error states are states of that destination, not separate destinations.
Reusable lists, cards, and other components remain composables within screens.

Use the following default boundary:

| Layer | Responsibility |
| --- | --- |
| Navigation host | Owns back stacks, maps keys to entries, applies navigation requests, and coordinates external entry and shared navigation UI. |
| Route composable | Obtains the entry-scoped ViewModel, collects UI state and navigation events with lifecycle awareness, handles platform effects, and connects screen callbacks to actions. |
| Screen composable | Renders a relevant immutable UI state and exposes user-action callbacks. Supports previews and UI tests without navigation or dependency injection. |
| Destination ViewModel | Produces screen state, handles actions and screen logic, and emits typed navigation events. |
| Reusable component | Renders supplied data and reports interactions through callbacks. |

Use names such as `FeatureRoute`, `FeatureScreen`, and `FeatureViewModel` to make
these roles clear. Keep ViewModel lookup, back-stack mutation, and event collection
out of screen and reusable component composables. Screen callbacks describe user
intent rather than navigation-library operations.

## Keys, arguments, and entry identity

Use typed, serializable destination keys implementing `NavKey`. Pass the minimum
stable identifiers and small values needed to open a destination; load authoritative
data through its data layer. Do not put repositories, ViewModels, callbacks, large
objects, or mutable UI state in keys.

Validate arguments at the destination boundary and handle missing, invalid, or
deleted resources with a defined UI and recovery action. Explicitly supply key
arguments to the ViewModel through the project's factory or saved-state setup;
do not assume Navigation 3 automatically copies key fields into `SavedStateHandle`.

Define whether opening the same destination and arguments reuses an entry, pops
to an existing entry, replaces an entry, or creates a new one. If equivalent
destinations can coexist with independent state, give their entries distinct
identity using the entry-key mechanism supported by the installed library version.
Do not let accidental key equality determine state sharing.

## Back-stack ownership and operations

Keep each back stack under one navigation owner. Route wrappers forward typed
requests to that owner; only the owner applies push, pop, replacement, and reset
operations. Do not mutate a stack during composition or use composable visibility
as a substitute for navigation history.

For every flow, define:

- Its starting destination and any prerequisites.
- Forward navigation, duplicate-entry policy, and completion or cancellation behavior.
- System Back and toolbar Up behavior, including root behavior.
- External-entry behavior and restoration expectations.

Back normally returns through user history. Up follows the application's
hierarchy and can differ after external entry. Route both through explicit
policies; avoid separate implementations that disagree. At a root, defer system
Back to the platform unless a documented flow requires different behavior.

Dismiss an active transient overlay before leaving its owning destination when
that is the overlay's intended Back behavior. Coordinate custom Back handling
with Navigation 3 and predictive Back; avoid competing handlers or consuming a
gesture before the intended navigation action is committed.

For top-level destinations with separate histories, document which stack is active,
how switching restores it, and what reselection and Back do. Keep scaffolds and
navigation bars owned by the level that coordinates those destinations.

## ViewModel scopes and work lifetimes

Default to one ViewModel per destination entry, acquired inside that entry's route
boundary. Configure Navigation 3 entry decorators to provide saved Compose state
and an entry-owned ViewModel store. Follow the installed version's API and decorator
ordering; a ViewModel lookup alone does not establish entry scope.

A retained entry may retain its ViewModel even while another entry is visible.
Do not equate ViewModel lifetime with visibility. Define when polling, subscriptions,
and other work start, pause, cancel, and resume. Prevent outdated asynchronous
results from updating a different selection or initiating unexpected navigation.

Shared ViewModels are exceptions. Document the owning scope, participating
destinations, state shared, and the point at which that scope is cleared. Prefer
repositories or other appropriate state owners for data shared across independent
screens. Work that must survive removal of a destination belongs to an appropriate
background or application-level owner, not its screen ViewModel.

## ViewModel navigation events

Navigation output types, naming, and transient versus acknowledged delivery follow
[UI state contracts](ui-state.md#state-and-one-time-effects) and
[naming conventions](ui-state.md#naming). See the
[ViewModel example](viewmodels.md#main-viewmodel-example) for implementation.
Requests express destinations or intent without giving ViewModels access to
`NavDisplay`, a mutable back stack, an Activity, or composable callbacks.

Screen interactions are callbacks to route-level action handlers. The ViewModel
evaluates actions and emits an event when navigation is appropriate. Route wrappers
collect events using a lifecycle-aware coroutine and forward them to the navigation
owner. Keep collection separate from rendering UI state.

Each implementation must define these delivery semantics:

- Which entry may originate an event and when it is eligible for navigation.
  Normally this is the active interactive entry; multi-pane layouts may have more
  than one eligible entry. Collect ordinary navigation events while the entry is
  resumed and additionally enforce the host's eligibility policy.
- Whether events produced while inactive are discarded, cancelled, or retained.
  Recheck retained events when the entry becomes eligible; do not allow a queued
  result from an obsolete interaction to redirect the user.
- Buffering, replay, consumption, and repeated-tap behavior. Give each request one
  logical consumer and avoid replaying completed navigation after recreation.
- How pending requests are invalidated when arguments, selection, or entry identity
  change, or when their originating entry is removed.

Do not assume a `Channel` or `SharedFlow` guarantees delivery exactly once or
survives process death. If navigation must survive recreation or process death,
represent the pending intent in restorable state with a request identity and an
acknowledgment policy. Apply it idempotently and reconcile it with the restored
back stack before retrying. Never persist a completed event as an unconsumed request.

The stream type is an implementation choice; its behavior must satisfy the flow's
delivery contract and be covered by relevant tests.

## State restoration

Restore the back stack and essential UI state after configuration changes and
system-driven process recreation. Use `rememberNavBackStack` with saveable keys
unless the project has a documented alternative.

Separate state by ownership:

| State | Default owner |
| --- | --- |
| Destination history and arguments | Saveable navigation back stack |
| Small local presentation state, such as scroll position | Entry-scoped saveable Compose state |
| Essential screen inputs and recoverable pending intent | ViewModel saved state |
| Authoritative data and durable work | Repository, persistence, or appropriate background owner |

ViewModels survive configuration changes within their scope; their instances do
not survive process death. Save the minimum inputs needed to reconstruct state
and reload data. Define whether drafts, filters, selection, and overlays are
restored. Do not relaunch permissions, duplicate navigation, or restart an already
accepted external operation merely because composition or collection restarted.

## Sheets, dialogs, and adaptive layouts

Use local screen state for a transient sheet or dialog whose lifetime belongs to
one destination. Use a navigation entry for an independently addressable task
that needs its own history, scope, or external entry. Document dismissal,
cancellation, and restoration behavior for either choice.

Responsive layouts may display multiple destinations or render a destination's
content in different panes. Preserve logical selection, state ownership, and
navigation history when the layout changes. Resizing must not push duplicate
entries, clear unrelated state, or instantiate unintended shared ViewModels.
Define focus, active-entry eligibility, and Back behavior for multi-pane flows.

## Deep links and notifications

Translate external input into validated typed navigation requests at the
application boundary. Use the same destination contracts as in-app navigation.
Define the parent history and Back/Up behavior rather than depending on whichever
screen happens to be visible.

Handle both cold launch and an already running application. Reconcile incoming
requests with restored history and define precedence when both exist. Deduplicate
repeated delivery where appropriate. Resolve unavailable content, authentication,
and permission prerequisites explicitly, preserving the intended target when the
flow requires it.

An Activity or external-entry handler should address the navigation owner rather
than retaining a reference to a particular screen ViewModel. Platform permission
launchers and external operations belong at a lifecycle-aware UI boundary; their
results must be correlated with the originating request.

## Refactors, features, and bug fixes

Before changing an affected flow, inspect its implementation and tests and record
the current transitions, intended behavior, state owners, and work lifetimes.
Identify any differences from these conventions. Preserve user-visible behavior
unless the task authorizes a change; internal boundaries may change as needed.

For new destinations, specify arguments, scope, state restoration, events, Back/Up,
and duplicate-entry policy. For bugs, trace the originating action through event
delivery, stack mutation, entry lifecycle, and rendering. Document accepted
exceptions with their rationale and scope rather than silently weakening the
conventions.

Use regression checks appropriate to the change:

- Forward navigation, Back/Up, completion, cancellation, and root behavior.
- Repeated taps, duplicate destinations, and repeated external requests.
- Returning to retained entries and clearing popped entries' ViewModels.
- Events completing while inactive and stale asynchronous results.
- Rotation, system process recreation, restored inputs, and pending requests.
- Cold and warm external entry, invalid arguments, and unavailable resources.
- Overlay dismissal and layout changes between compact and multi-pane states.

Test ViewModel event behavior, navigation-owner stack policies, and rendered screen
interactions at their respective boundaries. Use device or emulator checks where
platform lifecycle, predictive Back, or external-entry behavior matters. Report
which checks ran and any unverified cases.

## References

These conventions are project policy. Consult official documentation for APIs
matching the installed Navigation 3 version:

- [Navigation 3 basics](https://developer.android.com/guide/navigation/navigation-3/basics)
  explains back-stack ownership, keys, entries, and `NavDisplay`.
- [Saving state and scoping ViewModels](https://developer.android.com/guide/navigation/navigation-3/save-state)
  describes saveable keys, `rememberNavBackStack`, and entry decorators.
