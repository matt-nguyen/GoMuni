# Compose UI architecture conventions

## Scope

These reusable conventions prescribe UI boundaries for Android applications using
Compose, MVVM, and Navigation 3. They do not describe a particular application's
current structure. Apply them to new features and authorized refactors; preserve
intended behavior during bug fixes and avoid unrelated architectural migrations.

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
Use saveable state where restoration is required. Compose framework state objects
are separate presentation inputs; do not embed them in immutable ViewModel UI state.

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

Reuse existing design tokens and components. Place styling decisions in the design
system; keep business rules in their state owners. Prefer stable item identity for
lists and preserve accessibility semantics, descriptions, and interaction targets.

## Scaffolds, bars, and insets

The app host owns shared navigation chrome, such as top-level navigation bars or
rails. Each screen owns its destination scaffold, app bar, and content arrangement.
The screen renders bars from supplied state and reports their actions through
`onAction`; owning their layout does not imply owning navigation logic.

Assign each system inset and padding responsibility to one level. When host and
destination scaffolds coexist, explicitly coordinate their padding and consumed
insets so content is neither obscured nor padded twice. Define keyboard inset
handling where text entry requires it.

The route owns snackbar, sheet, and other presentation controllers; the screen
renders their hosts and surfaces using supplied inputs. Define whether each overlay
belongs to a destination or to the app host. Follow navigation.md for navigable
overlays, dismissal, and Back coordination.

## Effects and platform integration

Route wrappers execute all feature-level effects, including transient UI effects,
permission launchers, external operations, focus changes, and programmatic scrolling.
Use lifecycle-aware effect collection and request correlation as specified in
ui-state.md and navigation.md. Keep screen rendering independent of effect delivery.

Use Compose effect APIs with deliberate keys and cleanup at this integration
boundary. Avoid duplicate collectors, repeated launches on recomposition, and
capturing outdated arguments or callbacks. App-wide external-entry handling stays
at the app/navigation boundary and delegates to the appropriate flow.

ViewModels request effects without retaining Activity, launcher, focus, or scaffold
objects. Platform results return as correlated actions to the correct owner.
Presentation-only effects may originate in the route's UI state holder rather
than the ViewModel.

## Adaptive layouts

Keep one UI state and action contract across compact and expanded layouts. Obtain
current window size and posture at an integration boundary and supply adaptive
inputs. Select layouts using the app window and available space rather than a
fixed device category or orientation alone.

Extract layout variants when that keeps rendering understandable; variants use
the same contract and shared components. Do not introduce separate ViewModels or
duplicate business state merely because a pane moves or becomes visible.

Preserve inputs, selection, and presentation state through resizing and folding.
Route-owned state holders should not be recreated solely because a different
layout branch renders. Follow navigation.md for multi-entry scenes, active-entry
eligibility, history, and Back behavior.

## Previews and verification

Preview screens with representative supplied state, a no-op action callback, and
preview-owned presentation objects. Preview setup may create state holders; the
production screen remains independent of ViewModel lookup, network access, and DI.
Use the project theme and cover meaningful states and adaptive layouts supported
by the feature. Previews show rendering; they do not verify effect delivery.

Test critical screen interactions by supplying state and checking the emitted
typed actions. Test reusable components through their named callback contracts.
Verify state transitions in ViewModel tests and integration behavior at the route
or navigation boundary, including effect correlation and restoration when relevant.

Check text scaling, accessibility, keyboard behavior, overlays, and host/screen
inset coordination where the change affects them. Select verification appropriate
to the change; avoid tests that only mirror private layout structure.

Before refactoring, identify existing responsibilities, behavior, and state owners.
Afterward, verify the affected contracts and document accepted exceptions. Report
what was checked and any material unverified behavior.

## References

These boundaries are project policy, including route ownership of presentation
state and effects. Android's general guidance allows state at the lowest suitable
owner; this document chooses a stricter screen boundary for consistent contracts.

- [State hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
  explains UI versus business state and ownership choices.
- [Adaptive display sizes](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes)
  explains preserving state while rendering different layouts.
