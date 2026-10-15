# Compose

Read [UI architecture](ui-architecture.md) for boundaries alongside it.
[UI state](ui-state.md) owns delivery/restoration, [Navigation](navigation.md)
owns entries/scenes, and [DESIGN.md](../DESIGN.md) owns appearance. Apply to new
work and authorized refactors; preserve behavior during fixes.

## APIs

- Name composables by purpose. Use named arguments for destination wiring and
  calls with multiple callbacks or booleans.
- Renderable screens/components take `modifier: Modifier = Modifier`, normally
  first among optional parameters. Apply it once to the outer layout; use fresh
  child modifiers. Effect-only composables need no Modifier.
- Use narrow explicit inputs and content slots; avoid speculative flags or whole
  ViewModel dependencies. Reserve CompositionLocal for scoped cross-cutting context,
  not ordinary state/callbacks or hidden dependency lookup.
- Document public/internal entry points with purpose, every `@param`, and relevant
  lifecycle behavior.

## State and effects

- Create route-owned scroll, snackbar, focus, and sheet controllers with appropriate
  remember APIs. Key remembered objects by invalidating inputs; preserve identity
  across adaptive branches and use stable keys in lazy lists.
- Use `rememberSaveable`/Savers for small restorable presentation state. Ordinary
  `remember` does not survive Activity recreation. Keep Compose state holders out
  of immutable ViewModel state.
- Collect rendering via `collectAsStateWithLifecycle`. Collect transient effects in
  route-owned `LaunchedEffect`/`repeatOnLifecycle`; use entry RESUMED for interactive
  navigation and enforce host eligibility.
- Key effects by work identity. Use `rememberUpdatedState` for current callbacks
  without restarts; pair `DisposableEffect` registration with `onDispose` cleanup.
- `rememberCoroutineScope` ends with composition, not automatically on lifecycle
  stops. Register launchers stably and launch from handlers/effects after registration.
- Never run operations in a composable body or duplicate collectors/permission
  launches on recomposition. Correlate platform results with their owner; UI effects
  must not block navigation collection.

## Layout and accessibility

- Reuse design tokens/components. Respect caller placement and modifier order:
  padding, clipping, backgrounds, and input affect layout/hit areas.
- Apply scaffold padding and consume each inset at its declared owner. Avoid doubled
  padding, obscured content, and keyboard overlap.
- Select adaptive layouts from window size, posture, and available space. Keep the
  same inputs/callbacks and hoist controllers above branches when state must survive.
- Preserve semantics, meaningful descriptions, target sizes, keyboard/focus behavior,
  text scaling, and screen-reader usability. Decorative content needs no redundant
  descriptions.

## Verification

Preview meaningful states/sizes with supplied state, theme, no-op callbacks, and
preview-owned controllers; require no DI, ViewModels, network, or platform effects.
Test screens through typed actions and components through callbacks, avoiding
private layout assertions. Check relevant semantics, font scales, keyboard,
overlays, resizing, and insets. Device checks verify platform behavior; previews
cannot prove effect delivery or recovery. Report material gaps; follow
[testing](testing.md).
