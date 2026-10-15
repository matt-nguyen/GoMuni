# UI architecture conventions

## Scope

These reusable conventions prescribe UI boundaries for Android applications using
Compose, MVVM, and Navigation 3. They do not describe a particular application's
current structure. Apply them to new features and authorized refactors; preserve
intended behavior during bug fixes and avoid unrelated architectural migrations.

This document owns UI responsibility boundaries, dependency placement, action wiring,
and extraction decisions. [Compose implementation](compose.md) owns API conventions,
remembered state, effects, layout mechanics, accessibility, and previews. Read both
for Compose work; link to the owning document instead of duplicating its rules.

Use [Navigation conventions](navigation.md) for destination identity, back stacks,
ViewModel scope, and navigation events. Use [UI state conventions](ui-state.md)
for state models, asynchronous work, restoration, and effect delivery. Use the
project's `DESIGN.md` for visual tokens and components. Keep feature-specific
contracts and accepted exceptions in separate documentation.

## Responsibilities

| Boundary | Owns |
| --- | --- |
| App/navigation host | Back stacks, entry registration, shared navigation chrome, external-entry coordination, and application-level UI integration. |
| Feature route | ViewModel acquisition, lifecycle-aware collection, screen callback wiring, presentation state holders, and UI/platform effect execution. |
| Feature ViewModel | Screen/business state, action handling, asynchronous screen logic, and typed navigation or effect requests. |
| Feature screen | Rendering supplied state, destination scaffold and app bar, adaptive content composition, and reporting typed actions. |
| Shared component | Rendering supplied values and reporting interactions through named callbacks. |

Use `FeatureRoute`, `FeatureScreen`, `FeatureUiState`, `FeatureAction`, and
`FeatureViewModel` names where appropriate. Keep each destination's contract
focused on its task. A route is an integration boundary, not a replacement for
the ViewModel or an additional repository layer.

## Composable responsibility checklist

- Give each composable one clear role from the table above. Integration composables
  may wire several concerns, but delegate destination display and platform execution
  when those responsibilities obscure the wiring.
- Extract a named helper for a coherent responsibility, not an arbitrary block or
  line-count target. Keep private helpers beside their owner until reuse warrants
  moving them. Do not add state holders or ViewModels just to shorten a function.
- Pass narrow values and named callbacks across boundaries. Display helpers receive
  history and callbacks; the navigation owner retains stack mutation and policy.
  Keep testable eligibility/relationship rules in pure functions shared by their users.
- Preserve state and lifecycle ownership during extraction: entry-scoped ViewModels,
  effect keys and collection, saveable state, scene metadata, and inset ownership.
- Follow [composable API conventions](compose.md#composable-apis-and-documentation)
  for parameters, named arguments, and KDoc.
- Before finishing, identify each composable's role and verify that helpers have not
  acquired unrelated DI, business logic, platform work, or navigation policy.

## Route wrappers and state integration

Acquire dependencies at the route/entry boundary using the project's dependency
injection convention. Obtain the destination's entry-scoped ViewModel there, not
inside its screen or shared components. Supply validated destination arguments
through the configured factory or saved-state setup.

Collect the ViewModel's read-only UI state with lifecycle awareness. Wire the
screen's typed actions to the appropriate handler: business actions go to the
ViewModel; presentation-only actions go to route-owned UI state holders. Do not
send every scroll or animation change through the ViewModel merely because the
screen is stateless.

Route wrappers own explicit presentation state, such as scroll state, snackbar
hosts, focus requesters, and sheet controllers, and provide what rendering needs
to the screen. Hoisted helpers may manage these objects on the route's behalf.
Use [remembered-state mechanics](compose.md#remembered-state-and-restoration-mechanics)
to construct and retain presentation objects according to this ownership.

Screens and reusable components do not own feature state, collect flows, or launch
coroutines/effects. Framework composables may manage their own intrinsic behavior;
this rule does not require replacing the internal mechanics of Material components.

## Screen contracts and actions

Use one typed `onAction: (FeatureAction) -> Unit` callback per screen. Define actions
as user intent with the minimum necessary payload, such as an item identity or
changed input. Avoid passing navigation-library types, platform objects, or
composable callbacks inside actions.

A screen receives:

- The relevant immutable `FeatureUiState`.
- Its typed action callback.
- A `Modifier` for caller-controlled placement.
- Explicit presentation and adaptive inputs needed to render the destination.

Screens may derive display values and choose layouts from supplied inputs. They
must not fetch data, obtain dependencies, mutate navigation history, or execute
operations during composition. Recomposition is rendering, not an action trigger.

Shared components use named callbacks such as `onClick`, `onRetry`, or
`onItemSelected`. The screen translates them into feature actions. Components
must not depend on a feature's entire action hierarchy or ViewModel merely to
report one interaction.

## Shared reusable components

Put reusable components in their owning shared package from the start. Use a
feature's `components` package for feature-specific rendering, and the app-wide
UI/design-system package for primitives and components independent of feature contracts.
Feature routes, screens, state, actions, and ViewModels remain in feature packages.
Private screen-specific layout helpers may remain beside the screen.

Keep shared APIs narrow: pass values, small presentation models, modifiers, and
named callbacks. Do not make the shared package depend on feature orchestration.
Extract a meaningful reusable responsibility rather than every small layout block.
Avoid adding speculative options that have no concrete use.

Place styling decisions in the design system and business rules in their state
owners. Rendering mechanics follow [Compose conventions](compose.md).

## Scaffolds, bars, and insets

The app host owns shared navigation chrome, such as top-level navigation bars or
rails. Each screen owns its destination scaffold, app bar, and content arrangement.
The screen renders bars from supplied state and reports their actions through
`onAction`; owning their layout does not imply owning navigation logic.

Assign each system inset and padding responsibility to one level. Implementation
follows [layout and inset mechanics](compose.md#layout-modifiers-insets-and-accessibility).

The route owns snackbar, sheet, and other presentation controllers; the screen
renders their hosts and surfaces using supplied inputs. Define whether each overlay
belongs to a destination or to the app host. Follow navigation.md for navigable
overlays, dismissal, and Back coordination.

## Effects and platform integration

Route wrappers execute all feature-level effects, including transient UI effects,
permission launchers, external operations, focus changes, and programmatic scrolling.
Use lifecycle-aware effect collection and request correlation as specified in
ui-state.md and navigation.md. Keep screen rendering independent of effect delivery.

Use [Compose effect mechanics](compose.md#effects-and-lifecycle-integration) at this
boundary. App-wide external-entry handling stays at the app/navigation boundary
and delegates to the appropriate flow.

ViewModels request effects without retaining Activity, launcher, focus, or scaffold
objects. Platform results return as correlated actions to the correct owner.
Presentation-only effects may originate in the route's UI state holder rather
than the ViewModel.

## Adaptive layouts

Use one UI state and action contract across layout variants. The integration boundary
supplies adaptive inputs; screens select their content arrangement. Do not introduce
separate ViewModels or duplicate business state because panes move or become visible.

Sizing and presentation-state mechanics follow
[Compose layout conventions](compose.md#layout-modifiers-insets-and-accessibility).
Multi-entry scenes, eligibility, history, and Back belong to [Navigation](navigation.md).

## Verification

Before refactoring, identify responsibilities, state owners, and work lifetimes.
Verify screen contracts at the screen boundary and integration at the route/navigation
boundary. Record accepted exceptions with rationale and scope.

Use [Compose previews and checks](compose.md#previews-and-verification) for rendering
and interactions, and [ViewModel tests](viewmodels.md#verification-checklist) for state
and operation behavior. Report what ran and any material unverified behavior.

## References

These boundaries are project policy, including route ownership of presentation
state and effects. Android's general guidance allows state at the lowest suitable
owner; this document chooses a stricter screen boundary for consistent contracts.

- [State hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
  explains UI versus business state and ownership choices.
- [Adaptive display sizes](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes)
  explains preserving state while rendering different layouts.
