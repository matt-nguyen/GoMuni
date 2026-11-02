# Domain-driven design

These are conventions, not an implementation inventory. Apply them to new work
and authorized refactors; preserve intended behavior during fixes. Record domain
policies and accepted exceptions separately.

## Language and boundaries

- Use consistent business vocabulary in models, operations, and documentation.
  Distinguish concepts with different meanings, identities, or lifecycles.
- Define the context in which each model and identifier applies. Different contexts
  may use different models of the same concept; translate explicitly at boundaries
  rather than forcing a universal model.
- Model business meaning independently of external payloads and storage schemas.
  See [Architecture](architecture.md) for mapping and dependency boundaries.

## Identity and invariants

- Entities have stable identity across state changes; value objects are defined by
  their values. Define identifier scope and equality consistently across operations.
- Use value objects when they enforce invariants or prevent identifier/unit mixups.
  Define valid ranges, units, and missing/unknown states; do not invent valid-looking
  values for invalid input.
- Enforce business invariants when constructing or changing domain state. Keep
  rules with their owning model; use domain policies for rules spanning concepts
  that have no natural entity or value-object owner.
- Make time-dependent rules explicit about their time input. Keep business
  decisions separate from localized text, formatting, and interaction state.

## Consistency and behavior

- Define aggregates around invariants requiring atomic consistency. An aggregate
  root controls changes within that boundary; relationships alone do not require
  objects to belong to the same aggregate.
- Define failure and reconciliation behavior for operations crossing consistency
  boundaries. Use domain events when a meaningful business occurrence needs
  independent consumers, with explicit delivery and consistency requirements.
- Read projections may be immutable data classes. Use richer models where business
  behavior requires them; do not add aggregates or events solely to follow a pattern.

## Design evidence

For affected behavior, record vocabulary/context, identity, invariants, rule owners,
and consistency boundaries in feature handoffs or relevant feature documentation.
State unresolved business assumptions rather than inventing rules. Keep the record
proportional to scope and identify scenarios verifying changed rules; see
[Testing](testing.md). Architectural contract and review guidance belongs in
[Architecture](architecture.md).
