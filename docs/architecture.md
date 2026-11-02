# Architecture

These are conventions, not an implementation inventory. Apply them to new
work and authorized refactors; preserve intended behavior during fixes. Record
feature policies and accepted exceptions separately.

## Boundaries

Dependencies point toward domain contracts. Enforce through packages, visibility,
review, or optional Gradle modules; shared packages must respect the same boundaries.

| Layer | Responsibility |
| --- | --- |
| UI | Presentation, screen state/actions, platform UI integration; consumes domain contracts/models. |
| Domain | Business models, validation, use cases, failures, repository interfaces; independent of Android, DI annotations, storage, network, and data implementations. Coroutine/Flow types are allowed. |
| Data | Implements domain repositories; coordinates sources, mapping, persistence, and data policy. |
| Composition root / DI | Wires implementations and platform dependencies across layers. |
| Platform/background adapters | Connect Android entry points to domain; never manipulate screen ViewModels. |

## Repositories and use cases

- Domain owns repository interfaces; data owns implementations. Repositories access
  persistence through local data source interfaces, including Room and DataStore;
  local data sources own storage APIs, keys, and storage-model mapping. Prefer DAO
  `@Transaction` methods for operations within one DAO. For transactions spanning
  multiple DAOs, use a dedicated data-layer transaction coordinator that owns
  `TransitDatabase.withTransaction`; local data sources inject DAOs and the
  coordinator rather than `TransitDatabase`.
  Data hides HTTP clients, entities, and source coordination. Contracts describe
  capabilities, not endpoint/table structure. Avoid duplicate or unnecessary interfaces.
- Define each operation's inputs/identity, output/failures, suspend-versus-stream
  behavior, freshness/cache authority, concurrency, cancellation, retry/idempotency,
  and work triggered by subscription changes.
- Use cases encapsulate business rules or meaningful orchestration across domain
  operations; do not create use cases that only delegate repository calls. Simple
  ViewModel operations call repositories directly. Each use case exposes one public
  operation through `operator fun invoke`; helpers remain private.
- Repositories own source coordination/caching; UI owns presentation decisions.
  Use cases must not retain screen state or coordinate navigation.
- Domain enforces business invariants; UI controls validation presentation; data
  validates external payloads/storage constraints and translates failures.

## Models and failures

Keep DTOs, entities, storage/serialization annotations, and transport failures in
data. Map to domain at that boundary; map domain to UI only when presentation needs
differ. Keep localized text, formatting, visual tokens, and Compose state in UI.
Place mappers beside their boundary; define missing/unknown values, identifiers,
and units. Malformed data must not silently become successful empty content.

Represent expected failures as sealed domain error interfaces returned through
typed outcomes, using vocabulary appropriate to the capability. Reuse the existing
outcome convention; do not represent expected domain failures as thrown exceptions.
Data translates transport/storage failures into these outcomes; use cases may
translate business outcomes; ViewModels provide presentation/recovery. Keep raw
responses, SQL exceptions, and exception text out of those contracts.

Unexpected failures remain exceptions. Define logging and safe presentation without
broad swallowing or duplicate logging. Define stream failure/retry semantics.
Always preserve `CancellationException`; catch only for meaningful recovery,
translation, or cleanup.

## Work lifetimes and DI

- Callers own ordinary work: suspend for commands, Flow for observations, structured
  concurrency for children. Never detach commands into unowned scopes.
- Data/domain APIs must be main-safe. Blocking/expensive work owns dispatcher
  switching; inject dispatchers where needed. `suspend` alone does not switch threads.
- Screen work belongs to destination scope; visibility work may need a narrower
  lifetime. Document owners, subscribers, cancellation, and cleanup of shared scopes.
- Work surviving destination removal needs an explicit background owner. Application
  scopes do not guarantee process-death survival. Define stale/duplicate policies,
  transactions/reconciliation, and retries after uncertain external mutations.
- Use Hilt at Android entry points/composition root and constructor injection
  elsewhere. Provide framework-free domain objects through composition-root wiring.
  Avoid service locators and container lookup inside domain/screens.
- Choose DI scopes by lifetime; supply runtime arguments explicitly. Long-lived
  objects must not retain Activity/UI/navigation references. Abstract platform
  capabilities narrowly, with implementations outside domain.

## Persistence and verification

Repositories own source of truth, freshness, offline/refresh policy, conflicts, and
cache invalidation; no universal local-first/network-first rule applies. Persist
durable data through repositories; UI saved state contains reconstruction inputs.
Distinguish cached, pending, and confirmed data. Define unavailable storage,
partial-update, and retry behavior in feature/data documentation.

Before refactoring, inspect dependencies, contracts, behavior, and lifetimes. Verify
affected domain rules, repository coordination/mapping/failures, concurrency,
cancellation, retry, ViewModel behavior, DI, and platform integration at their
boundaries. Use controlled dependencies; report unverified persistence/recovery.
See [testing](testing.md), [UI architecture](ui-architecture.md),
[UI state](ui-state.md), and [Navigation](navigation.md) for detailed contracts.
