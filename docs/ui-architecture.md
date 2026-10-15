# UI architecture

These reusable conventions prescribe UI boundaries for Android applications using
Compose, MVVM, and Navigation 3. Read [Compose](compose.md) alongside it for mechanics. Apply to
new work and authorized refactors; preserve behavior during fixes. Feature-specific
contracts/exceptions belong in separate documentation.

## Responsibilities

| Boundary | Owns |
| --- | --- |
| App/navigation host | Stacks, entries, shared navigation chrome, external entry, app UI integration |
| Feature route | Dependencies/ViewModel acquisition, lifecycle collection, action wiring, presentation controllers, UI/platform effects |
| ViewModel | Screen/business state, actions, async logic, typed navigation/effect requests |
| Screen | Supplied-state rendering, destination scaffold/app bar, adaptive arrangement, typed actions |
| Shared component | Supplied-value rendering and named callbacks |

Use `FeatureRoute`, `FeatureScreen`, `FeatureUiState`, `FeatureAction`, and
`FeatureViewModel`. Keep one clear role per composable. Extract coherent
responsibilities, not arbitrary blocks or line-count targets; avoid unnecessary
ViewModels/state holders. Preserve scope, effect keys, collection, saveable state,
scene metadata, and inset ownership during extraction.

## Routes and screen contracts

- Acquire entry-scoped ViewModels/dependencies at the route boundary; supply validated
  arguments through factories/saved state. Collect read-only UI state with lifecycle
  awareness.
- Send business actions to the ViewModel and presentation interactions to route-owned
  state holders. Routes own scroll, snackbar, focus, and sheet controllers, passing
  what rendering needs. Hoisted helpers may manage these on their behalf.
- Screens receive immutable relevant state, one `onAction: (FeatureAction) -> Unit`,
  Modifier, and explicit presentation/adaptive inputs. Actions express intent with
  minimal payloads, not navigation types, platform objects, or composable callbacks.
- Screens/components do not own feature state, fetch dependencies/data, collect
  flows, launch effects, or mutate history. Intrinsic framework component behavior
  is allowed. Recomposition is rendering, not an operation trigger.
- Components use narrow values and named callbacks; screens translate callbacks
  into actions. Components must not depend on whole feature actions/ViewModels
  merely to report an interaction. Keep navigation policy with the owner; extract
  shared eligibility rules as pure functions.

## Reuse, chrome, and effects

Put reusable rendering in feature `components` or app UI/design-system packages
according to ownership. Keep routes/screens/state/actions/ViewModels in feature
packages and private helpers beside their owner. Avoid speculative options or
shared packages that depend on feature orchestration. Styling belongs to
[DESIGN.md](../DESIGN.md); business rules belong to state owners.

The host owns shared bars/rails; screens own destination scaffolds, app bars, and
content arrangement. Give each inset/padding one owner. Routes own overlay
controllers; screens render surfaces. Define destination-versus-host overlay scope
and follow [Navigation](navigation.md) for Back and navigable overlays.

Routes execute feature effects, permissions, external launches, focus, and scrolling.
Keep app-wide external entry at the host. Correlate results to owner actions using
[UI state](ui-state.md) delivery contracts; ViewModels must not retain platform/UI
controllers. Presentation effects may originate in route state holders.

## Adaptive UI and verification

Use one state/action contract across layouts. Integration supplies adaptive inputs;
screens choose arrangement. Moving panes must not create duplicate business state
or ViewModels. Follow Compose mechanics and Navigation multi-entry policies.

Before refactoring, identify roles, state owners, and lifetimes; check helpers have
not acquired unrelated responsibilities. Verify screen contracts at screens,
integration at routes/hosts, rendering/interactions through Compose checks, and
operations through ViewModel tests. Record exceptions and report unverified cases;
see [testing](testing.md).
