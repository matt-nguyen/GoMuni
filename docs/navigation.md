# Navigation

These are reusable conventions for Android applications using Jetpack Compose
and Navigation 3. Apply to new work and authorized navigation refactors;
preserve intended behavior during fixes. Record feature flows and exceptions
separately; these conventions do not certify current implementation.

## Destinations and ownership

A destination represents a task/place with history, arguments, and Back behavior.
Loading/content/empty/error are states, not destinations; reusable widgets stay
inside screens. Follow [UI architecture](ui-architecture.md): host owns history,
route integrates ViewModel/lifecycle/platform work, screen renders state and reports
intent, ViewModel produces state/requests, components report callbacks.

Only the navigation owner mutates stacks. Routes forward typed requests; screens
and ViewModels never own stacks. Never mutate history during composition or infer
history from visibility.

## Keys and flow policy

- Use typed serializable `NavKey` keys with minimal stable identifiers/small inputs.
  Exclude repositories, ViewModels, callbacks, large objects, and mutable UI state.
- Validate destination arguments and define recovery for missing/deleted resources.
  Explicitly supply inputs through factories/saved state; Navigation 3 does not
  automatically copy key fields into `SavedStateHandle`.
- Define reuse, pop-to-existing, replacement, and duplicate-entry policy. Give
  independently coexisting equivalent destinations distinct supported entry identity;
  accidental key equality must not choose sharing.
- Each flow defines start/prerequisites, forward/completion/cancellation, duplicates,
  Back/Up/root, external entry, and restoration. Back follows history; Up follows
  hierarchy and may differ after external entry. Defer root Back to the platform
  unless documented otherwise.
- Dismiss transient overlays first when intended. Coordinate custom/predictive Back
  without competing handlers or premature gesture consumption.
- For independent top-level histories, define active stack, switching/restoration,
  reselection, and Back. Keep shared chrome at the coordinating owner.

## Scope and navigation delivery

Default to one ViewModel per entry, acquired in its route. Configure saveable-state
and ViewModel-store decorators using the installed version's ordering; lookup alone
does not establish entry scope. Retention does not imply visibility: define work
start/pause/cancel/resume and reject obsolete results.

Document shared ViewModel scope, participants, shared state, and clearing point.
Prefer repositories for independent screens' shared data. Work surviving removal
needs a background/application owner.

Use [UI state delivery/naming](ui-state.md) and [ViewModel patterns](viewmodels.md).
Collect events separately from rendering, forward them to the owner, and define:

- Eligible originating entries: normally interactive/resumed entries, possibly
  several panes, plus the host's eligibility check.
- Inactive-event discard/cancel/retention; revalidate retained intent before applying.
- Buffering, replay, consumption, repeated taps, and one logical consumer.
- Invalidation after argument, selection, identity, or entry removal changes.

Channel/SharedFlow do not guarantee exactly-once delivery or process recovery.
Recoverable navigation needs restorable pending identity, acknowledgment,
idempotent application, and reconciliation with restored history. Never replay
completed navigation as pending.

## Restoration and adaptive UI

Use `rememberNavBackStack` and saveable keys unless a documented alternative exists.

| State | Owner |
| --- | --- |
| History/arguments | Saveable navigation stack |
| Small presentation state | Entry-scoped saveable Compose state |
| Essential inputs/recoverable intent | ViewModel saved state |
| Authoritative data/durable work | Repository, persistence, background owner |

ViewModel instances survive scoped configuration changes, not process death. Save
minimal reconstruction inputs, reload data, and define draft/filter/selection/
overlay recovery. Restarts must not repeat permissions, navigation, or accepted
external operations.

Transient sheets/dialogs belong to destination state. Use entries for independently
addressable tasks needing history/scope/external entry; define dismissal, cancellation,
and restoration. Adaptive panes preserve selection, ownership, and history without
duplicate entries or unintended sharing. Define focus, eligibility, and Back.

## External entry and verification

Validate deep links/notifications into typed requests at the application boundary.
Use in-app destination contracts; define parent history, Back/Up, cold/warm launch,
restored-history precedence, deduplication, unavailable content, and prerequisites.
Preserve targets when required. External handlers address the navigation owner;
UI launchers correlate results with originating requests.

Before changes, inspect implementation/tests, transitions, owners, and lifetimes.
Test stack policies, ViewModel outputs, and screen actions at their boundaries.
Cover changed duplicates, Back/root, scope clearing, stale/inactive events,
recreation, external entry, overlays, and adaptive behavior. Use devices for
platform lifecycle/predictive Back; report gaps. See [testing](testing.md).
