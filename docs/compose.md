# Compose implementation conventions

This document owns Compose APIs and mechanics. [UI architecture](ui-architecture.md)
owns responsibility boundaries and extraction decisions; read both for Compose work.
[UI state](ui-state.md) owns state/effect delivery and restoration contracts,
[Navigation](navigation.md) owns scenes and entry lifetimes, and
[DESIGN.md](../DESIGN.md) owns appearance. Apply these conventions to new work and
approved refactors; preserve intended behavior during fixes.

## Composable APIs and documentation

- Name composables by their purpose. Use named arguments for destination wiring and
  calls with multiple callbacks or booleans.
- Renderable screens/components accept `modifier: Modifier = Modifier`, normally
  the first optional parameter. Apply it once to the outer layout; use fresh
  modifiers for children. Effect-only composables do not need a Modifier.
- Keep inputs narrow and explicit. Use content slots for replaceable presentation
  rather than speculative flags or dependencies on entire ViewModels.
- Add concise KDoc to public/internal composable entry points: purpose, `@param`
  descriptions for every argument, and any non-obvious lifecycle behavior.
- Use CompositionLocal for scoped, cross-cutting context such as theme or scene
  coordination. Prefer explicit parameters for ordinary state and callbacks; do
  not introduce hidden dependency lookup or unrelated mutable shared state.

## Remembered state and restoration mechanics

- Create route-owned scroll state, snackbar hosts, focus requesters, and sheet
  controllers with their appropriate remember APIs. Pass what rendering needs
  into screens; follow [ownership boundaries](ui-architecture.md#route-wrappers-and-state-integration).
- Key `remember` by inputs that invalidate the object. Recomposition must not
  recreate controllers or restart work accidentally. Keep identity stable when
  switching between layout variants.
- Use `rememberSaveable` and a Saver where needed for small restorable presentation
  values. Ordinary `remember` does not survive Activity recreation. Choose what
  to restore using [restoration contracts](ui-state.md#restoration).
- Keep Compose framework state objects outside immutable ViewModel UI state.
  Use stable item keys in lazy layouts so item-local state follows item identity.

## Effects and lifecycle integration

- Collect rendering state with `collectAsStateWithLifecycle`. Collect transient
  effects in `LaunchedEffect` with `repeatOnLifecycle`; use the entry's lifecycle
  and RESUMED for interactive navigation, following [eligibility rules](navigation.md#viewmodel-navigation-events).
- Choose effect keys for the work's actual identity. Use `rememberUpdatedState`
  for callbacks/values that must stay current without restarting the operation.
- Use `DisposableEffect` to register observers/resources and release them in
  `onDispose`. Use `rememberCoroutineScope` for route-owned interaction work that
  should end with composition; it does not itself pause when the lifecycle stops.
- Do not execute operations in a composable body. Avoid duplicate collectors and
  permission launches on recomposition. Keep launchers registered at a stable
  integration boundary and request from handlers/effects after registration.
- Return platform results to their owner according to the
  [delivery contract](ui-state.md#state-and-one-time-effects). UI effect execution
  must not block the navigation collector; see the
  [route example](viewmodels.md#collecting-effects-in-the-route).

## Layout, modifiers, insets, and accessibility

- Use project tokens/components from DESIGN.md. Respect caller placement through
  Modifier and review modifier order: padding, clipping, backgrounds, and input
  modifiers affect layout and hit areas differently.
- Apply scaffold content padding at the intended boundary. Coordinate consumed
  insets when scaffolds nest to avoid doubled padding or obscured content. Handle
  keyboard insets where text input requires it. Follow the declared
  [inset ownership](ui-architecture.md#scaffolds-bars-and-insets).
- Choose adaptive layouts from current window size, posture, and available space.
  Extract variants with the same inputs/callbacks. Hoist remembered controllers
  above layout branches when their state must survive resizing or folding.
- Preserve semantics, meaningful content descriptions, interaction target sizes,
  and keyboard/focus behavior. Avoid redundant descriptions for decorative content.
  Verify text scaling and screen-reader behavior where interactions change.

## Previews and verification

- Preview screens with representative supplied state, no-op callbacks, the project
  theme, and preview-owned presentation objects. Do not require DI, ViewModels,
  network, or live platform effects. Cover meaningful states and supported sizes.
- Test screen interactions through supplied actions and components through named
  callbacks. Previews verify rendering, not effect delivery or Android recovery.
- Check relevant font scales, semantics, keyboard behavior, overlays, resizing,
  and host/screen insets. Use device/emulator checks for platform behavior and
  report material unverified cases. Avoid tests of private layout structure.

## Related guidance

- [UI architecture](ui-architecture.md): ownership and composable decomposition.
- [UI state](ui-state.md): rendering, delivery, and restoration contracts.
- [ViewModels](viewmodels.md): state production and asynchronous work.
- [Navigation](navigation.md): entries, scenes, Back, and eligibility.
