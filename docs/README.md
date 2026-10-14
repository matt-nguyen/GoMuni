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
| [UI architecture](ui-architecture.md) | Compose feature structure, route/screen/component boundaries, action contracts, dependency injection placement, presentation state/effect integration, scaffolds/insets, adaptive layouts, or previews and UI tests. |
| [ViewModels](viewmodels.md) | ViewModel responsibilities or splitting, typed action handling, initialization, internal patterns, operation concurrency, saved-state recovery, cleanup, or ViewModel tests. |
| [Transit presentation](features/transit-presentation.md) | This app's transit destinations, current state ownership, arrivals scene integration, tracking startup, or notification navigation. |

When adding a document, add a row with a relative link and clear reading triggers.
