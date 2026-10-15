# UI state

These are reusable conventions for Android applications using Compose and MVVM.
They prescribe state contracts, ownership, and delivery behavior;
[ViewModels](viewmodels.md) owns implementation/testing;
[Navigation](navigation.md) owns history/lifetimes. Apply to new work and authorized
refactors; preserve intended behavior during fixes.

## Ownership and state shape

State flows down; actions flow to its owner. Give each value one authoritative owner.

| State | Owner |
| --- | --- |
| Screen inputs, selection, decisions, operation state | Destination ViewModel |
| Focus, animation, expansion, presentation controllers | Route-owned/hoisted Compose state |
| Shared authoritative data/durable settings | Repository/persistence |
| History/arguments | Navigation owner |

Move presentation state to a ViewModel when logic, coordination, or lifetime needs
it; restoration alone does not require that move. Routes collect/wire state;
screens render immutable inputs and report actions; components receive subsets.

- Expose read-only `StateFlow<FeatureUiState>` backed by private mutable state.
  Use an immutable rendering data class, grouped by responsibility/lifetime.
  Keep workflow progress/pending platform requests separate by default; derive
  busy/error presentation from their operation owner.
- Use nested sealed types/enums for exclusive sub-modes and phase-specific payloads;
  top-level rendering state is not a sealed workflow. Avoid contradictory flags.
- Keep independent content, refresh, selection, and mutation states independent.
  Distinguish not-loaded, empty success, partial/unavailable, permissions, and
  offline conditions when presentation differs.
- Select by stable identity and define disappearance handling. Derive counts/action
  availability; store projections only with a concrete need and consistent owner.
- Immutability includes nested data/collections. Publish new snapshots; read-only
  types and `@Immutable` do not make mutable backing objects safe.
- Define initial state/invariants and meaningful errors/recovery. Keep raw exceptions
  and sensitive text out of user-facing state; localize at a UI boundary.

## Production, actions, and presentation

StateFlow is a conflated latest snapshot, not a command queue. Derive state from
authoritative inputs without independently mutable duplicates. Follow ViewModel
sharing/concurrency and [Compose collection](compose.md#state-and-effects).
Actions express intent; define preconditions, transitions, work, outcomes, and
repeat policy. Preserve validity under overlapping/out-of-order operations.

Each feature documents loading/refresh/error policy:

| Situation | Decision to define |
| --- | --- |
| Initial load/empty success | Distinct loading, empty, failure, and recovery presentation |
| Refresh with content | Retain content/indicator or replace misleading stale content |
| Refresh failure/cache/offline | Content retention, freshness, error/retry, refresh availability |
| Partial loading | Section-level state and failure isolation |
| Mutation/optimistic update | Progress, disabled actions, confirmation, rollback/reconciliation/pending |

Specify retained data, concurrent-refresh policy, error clearing, and retry with
current arguments. Define whether new queries/identities retain prior content and
how that relationship is shown. Stale results must not overwrite newer state or
trigger obsolete effects; use ViewModel cancellation/correlation patterns.

## Naming and effects

| Contract | Convention |
| --- | --- |
| Input | `<Feature>Action`; occurrences past tense (`StopSelected`), commands imperative (`Refresh`) |
| Rendering | `<Feature>UiState` |
| Transient output | `<Feature>Effect`, exposed as `effects` backed by `_effects` |
| Navigation | `NavigateTo<Destination>`, `NavigateBack` |
| UI request | Meaningful `Show<Meaning>` / `Request<Interaction>` |
| Recoverable request | `<Feature>Request` |

Avoid the ambiguous `Event` contract suffix. Routes choose surfaces/localized text.
Use one typed read-only effects stream for transient UI/navigation whose loss is
acceptable. Keep rendering separate from execution and acknowledged pending requests.
This transient-stream exception is project policy; delivery-critical work needs state.

Define replay, buffering/overflow, inactive behavior, and one logical consumer.
UI effects must not block navigation or accidentally displace it. Collect at routes;
never replay completed effects. Channel/SharedFlow do not provide exactly-once
processing or process recovery.

Acknowledged requests need stable identity, phase, and acknowledgment meaning
(accepted/launched/completed/dismissed). Match results to identity/phase; old results
must not clear newer requests. Save minimal recovery inputs when required and
reconcile external state before retrying uncertain outcomes. Permissions/external
launches execute at UI boundaries with Activity Result recovery; recomposition
must not relaunch them.

## Restoration and verification

Define survival across configuration changes, process recreation, and fresh launch.
Use saveable Compose state for small presentation values, `SavedStateHandle` for
essential inputs/pending requests, and repositories for durable data/work. Save
reconstruction inputs, not datasets/exceptions/platform objects. Reload/revalidate
authoritative identities; reconstruct progress rather than restoring stuck loading
or completed effects. Saved state is not durable storage or exactly-once delivery.

Feature docs record owners, initial state, invariants, presentation, delivery, and
restoration semantics. Verify UI states/actions, effect loss/replay, and recreation
at their boundaries; follow the ViewModel checklist and [testing](testing.md).
Report material gaps and exceptions.
