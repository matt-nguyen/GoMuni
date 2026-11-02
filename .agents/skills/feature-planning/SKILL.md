---
name: feature-planning
description: Create an actionable Android feature implementation plan from a completed feature-discovery handoff.
---
# Feature Planning

## Purpose

Create an actionable implementation plan for a feature using the discovery findings and applicable project guidance.

Planning determines how the feature should be implemented.

Do not modify production code during this stage.

## Inputs

Use:

- Feature requirements.
- Discovery handoff.
- `AGENTS.md`.
- Reference documentation identified during discovery.
- Relevant source code when needed to validate or clarify the discovery findings.

Do not repeat broad repository discovery unless the discovery handoff is incomplete or appears incorrect.

## Process

1. Review the feature requirements and discovery handoff.
2. Resolve any blocking unknowns before finalizing the plan.
3. Determine the implementation approach using existing project patterns and constraints.
4. Identify the components that must be added, modified, or removed.
5. Define the resulting data and state flow.
6. Identify changes to contracts, models, UI state, actions, effects, persistence, networking, navigation, or module dependencies as applicable.
7. Define an implementation order that respects dependencies between changes.
8. Identify required tests.
9. Define verification steps.
10. Identify significant risks or assumptions.

Prefer existing project patterns and abstractions over introducing new ones.

Do not include speculative changes unrelated to the requested feature.

## Planning Guidelines

Plans should be specific enough that implementation does not need to redesign the solution.

Prefer identifying concrete modules, components, classes, or files when they are known.

Explain important architectural decisions, but do not turn the plan into detailed implementation code.

If the feature requires a project-level architectural decision that cannot be resolved from existing conventions, surface it instead of silently establishing a new convention.

## Output

Produce the following handoff:

### Approach

Briefly describe the implementation strategy and important design decisions.

### Affected Areas

List the modules and major components that will be added or modified.

### Data / State Flow

Describe the resulting flow of data, state, and user actions through the system.

### Implementation Steps

Provide an ordered sequence of implementation steps.

For each step, identify:

- The component or area being changed.
- What should change.
- Important existing patterns or constraints to follow.

### Tests

Identify tests that should be added or updated and the behavior each test should verify.

### Verification

List the build, test, lint, or other checks that should be run after implementation.

### Risks / Assumptions

Include only meaningful implementation risks, assumptions, or unresolved non-blocking questions.

## Handoff Requirements

The implementation agent should be able to execute the plan without repeating discovery or making major architectural decisions.

If critical information is still missing, do not produce a speculative final plan. Clearly identify what must be resolved first.