# Documentation index

Before planning, implementing, or debugging a change, read the documents whose
scope overlaps the task. Multiple documents may apply. Follow their conventions
and inspect the affected implementation and tests. These documents do not
necessarily describe current implementation.

| Document | Read when the task involves |
| --- | --- |
| [Architecture](architecture.md) | Layer dependencies, repository/use-case contracts, model mapping, failure translation, coroutine/work lifetimes, dependency injection, business validation, or data/persistence policy ownership. |
| [Navigation](navigation.md) | Destinations, back stacks, Back/Up, route/screen boundaries, ViewModel scopes, navigation events, restoration, adaptive navigation, or external entry. |
| [UI state](ui-state.md) | State models and ownership, screen actions, loading/refresh/error policies, StateFlow production and collection, asynchronous work, one-time effects, or state restoration. |
| [UI architecture](ui-architecture.md) | Any Compose change: responsibilities, route/screen/component boundaries, action wiring, dependency placement, or extraction decisions. |
| [Compose](compose.md) | Any Compose change: composable APIs/KDoc, modifiers, remembered state, effects, lifecycle collection, insets, adaptive layout mechanics, accessibility, or previews. |
| [ViewModels](viewmodels.md) | ViewModel responsibilities or splitting, typed action handling, initialization, internal patterns, operation concurrency, saved-state recovery, cleanup, or ViewModel tests. |
| [Testing](testing.md) | Formatting checks, test selection and required coverage, test file organization and naming, fakes versus mocks, coroutine or Compose tests, running tests, or diagnosing flaky tests. |

When adding a document, add a row with a relative link and clear reading triggers.

Define each convention in its owning document; other documents link to it rather
than restating it. `ui-state.md` owns state and delivery contracts; `viewmodels.md`
owns ViewModel implementation patterns. `ui-architecture.md` owns UI boundaries;
`compose.md` owns Compose implementation mechanics. Read both for Compose work.
When changing a convention, check linked
examples and references for consistency. Examples illustrate policy, not alternatives.
