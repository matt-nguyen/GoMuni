# Architecture conventions

## Scope

These reusable conventions define responsibilities and contracts for Android
applications using MVVM and layered architecture. They prescribe project policy;
they do not describe the current implementation or require a particular package
tree. Keep implementation inventories and accepted exceptions in separate documents.

Use [UI architecture](ui-architecture.md), [UI state](ui-state.md), and
[Navigation](navigation.md) for their detailed conventions. Apply these boundaries
to new features and authorized refactors without expanding unrelated bug fixes
into architectural migrations.

## Dependency boundaries

Dependencies point toward domain contracts:

| Layer or boundary | Responsibilities and allowed dependencies |
| --- | --- |
| UI | Presentation, screen state, user actions, and platform UI integration. Depends on domain models, repository interfaces, and use cases; not data implementations. |
| Domain | Business models, rules, use cases, expected failure types, and repository contracts. Independent of Android, Compose, persistence, network libraries, and data implementations. |
| Data | Implements domain contracts, coordinates data sources, maps external models, and owns persistence and data policy. Depends on domain and required infrastructure libraries; not UI. |
| Composition root / DI | Connects interfaces to implementations and supplies platform dependencies. May reference all layers for wiring. |
| Platform/background adapters | Integrate Android services, workers, and other entry points with domain contracts. Do not depend on screen ViewModels or manipulate their state. |

Kotlin coroutine and Flow types may appear in domain contracts. Keep Android and
DI framework annotations out of domain; construct domain objects through bindings
or provider methods in the composition root.

Enforce boundaries through packages, visibility, review, or Gradle modules as
appropriate. Separate modules are optional. Organize by feature within layers
when useful, and avoid a generic shared package that becomes a route around the
dependency rules.

## Repository contracts and data sources

Domain owns repository interfaces; data owns their implementations. Design contracts
around application capabilities rather than exposing an API client's endpoints or
a database's tables. ViewModels and use cases consume those contracts.

Repositories hide local and remote sources and coordinate shared authoritative
data. Neither UI nor use cases access DAOs, HTTP clients, persistence entities,
or source implementations directly. Data sources handle their specific storage
or transport operations; repositories decide how those sources work together.

For each repository operation, define:

- Whether it is a one-shot suspend operation or an observable stream.
- Inputs, identity, output meaning, and expected failure outcomes.
- Freshness and cache behavior, including whether returned data is authoritative.
- Concurrency, cancellation, retry, and mutation/idempotency behavior.
- Whether observing data starts work and what happens when observation stops.

Avoid duplicate repository interfaces in different layers for the same capability.
Split contracts when responsibilities or consumers require it, rather than adding
an interface for every class.

## Use cases and business rules

Use cases belong in domain and depend on domain repository contracts or other
domain operations. Introduce them for business rules, meaningful reuse, or
orchestration across operations. Simple ViewModel reads or commands may call
domain repository interfaces directly; a pass-through use case is not mandatory.

Business orchestration belongs in a use case; source coordination and caching
belong in a repository. Keep rendering decisions and transient presentation state
in UI. Name use cases by their application intent and keep responsibilities focused.
Do not make them retain screen state or become navigation coordinators.

Domain owns business validation and invariants. UI owns presentation validation,
such as when to display a field error. UI may surface domain validation results,
but critical business checks must also run at the domain operation boundary.
Data validates external payloads and storage constraints and maps problems into
the operation's contract.

## Models and mapping

Keep API DTOs, Room entities, transport errors, and serialization/storage annotations
inside data. Map them to domain models at the data boundary. Domain models describe
application meaning without depending on API or schema details.

Map domain data to UI models in UI when presentation needs differ. Direct rendering
of a suitable domain model is allowed; separate UI models are not mandatory for
every feature. Keep localized text, display formatting, visual tokens, and Compose
state holders out of domain and data.

Place each mapper beside the boundary it translates. Define handling for missing
fields, unknown values, identifier normalization, and units where relevant. Do not
silently turn malformed data into a successful empty result. Avoid copying models
between layers without a concrete contract or ownership reason.

## Failures across boundaries

Represent expected operational failures with typed domain outcomes, such as a
sealed result and domain failure types. Examples include unavailable resources,
connectivity failures, rejected input, and conflicts when those are anticipated
parts of the operation. Define the vocabulary per capability; do not require one
universal result type for every repository.

Translate expected transport and storage failures at the data boundary. Use cases
may translate them into business outcomes. ViewModels map outcomes to presentation
state and recovery actions. Do not expose HTTP responses, SQL exceptions, or raw
exception text as domain or user-facing failure contracts.

Unexpected failures remain exceptions rather than being disguised as success or
ordinary business rejection. Define where they are logged and safely presented,
without swallowing them in broad catch blocks or logging the same failure at every
layer. Observable operations must specify whether failures are emitted outcomes,
stream termination, or recoverable retry states.

Preserve `CancellationException`; cancellation is control flow, not a domain failure.
Catch only where a meaningful translation, recovery, or cleanup responsibility exists.

## Coroutines, dispatchers, and work lifetimes

Callers own the lifetimes of ordinary operations. Expose suspend functions for
one-shot work and Flow for ongoing observations. Use structured concurrency for
child work; repositories and use cases must not silently detach a caller's command
into an unowned scope.

Data and domain APIs must be safe to call from the main thread. The class performing
blocking I/O or expensive computation owns the required dispatcher switch. Inject
dispatchers where needed for control and testing; do not scatter hardcoded dispatcher
choices through callers. A suspend function alone does not move work off the main thread.

Screen work normally runs in its destination ViewModel's scope. Visibility-driven
work may need a narrower lifetime, as defined in the UI documents. Shared hot streams
or caches may use a deliberately supplied longer-lived scope; document its owner,
cleanup, subscribers, and cancellation policy.

Work that must continue after destination removal needs an explicit background
owner. Use the Android mechanism appropriate to the requirement, such as scheduled
persistent work or a user-visible service. An application coroutine scope can outlive
a screen but does not guarantee survival after process death.

Define duplicate-operation and stale-result policies at the relevant owner. Keep
cross-source mutations consistent using transactions or documented reconciliation
where required. Cancellation cannot undo an external mutation that already occurred;
recovery and retry must account for uncertain outcomes.

## Dependency injection

Use Hilt at Android entry points and in the composition root. Use constructor
injection for dependencies elsewhere; domain constructors remain framework-free.
Bind domain repository interfaces to data implementations through DI modules.
Provide domain use cases through composition-root factories or provider methods.

Keep DI wiring separate from business decisions. Do not use a service locator,
global singleton lookup, or a DI container from inside domain operations or screens.
Supply runtime destination inputs through the project's factory/saved-state mechanism.

Choose scopes by required lifetime, not convenience. Shared repositories may have
application scope when their state warrants it; screen ViewModels use destination
scope. Long-lived objects must not retain Activity, screen, launcher, or navigation
references. Inject narrow platform abstractions where domain needs a capability
such as time or an identifier source; implement Android details outside domain.

## Data policy and persistence

Repositories own data policy. Each feature documents its source of truth, freshness,
offline behavior, refresh triggers, conflict resolution, and cache invalidation.
There is no universal local-first or network-first requirement.

Persistence implementation belongs in data. Save durable user data and settings
through appropriate repository contracts; UI saved state holds reconstruction
inputs rather than serving as a database. Distinguish cached data, pending mutations,
and confirmed authoritative data when behavior depends on that distinction.

Define behavior for unavailable storage, partial updates, and retries where relevant.
Keep detailed policies in feature/data documentation rather than embedding them in
screens or duplicating them across ViewModels.

## Refactoring and verification

Before changing boundaries, inspect current dependencies, contracts, behavior, and
work lifetimes. Preserve intended user-visible behavior unless the task authorizes
a change. Record accepted exceptions with rationale, scope, and ownership.

Verify the affected contract at its boundary:

- Domain rules and use cases through repository fakes and meaningful outcomes.
- Repository mapping, source coordination, caching, and failure translation.
- Cancellation, concurrency, stale results, and retry/idempotency where relevant.
- ViewModel state/action behavior through domain contracts rather than infrastructure.
- DI wiring and platform/background integration where the change affects them.

Use deterministic coroutine tests and controllable dependencies where appropriate.
Test behavior rather than trivial delegation or private implementation structure.
Report verification performed and any untested persistence or recovery behavior.

## References

These are project conventions. In particular, domain-owned repository interfaces
and strict dependency direction are stronger choices than Android's general layer
recommendations; do not assume the guides mandate this exact structure.

- [Android data layer](https://developer.android.com/topic/architecture/data-layer)
  discusses repository responsibilities and data-source boundaries.
- [Android domain layer](https://developer.android.com/topic/architecture/domain-layer)
  discusses use cases and business orchestration.
- [Coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
  covers main-safe APIs, dispatcher injection, and cancellation.
