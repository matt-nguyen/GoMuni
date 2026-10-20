---
name: feature-implementation
description: Implement an approved Android feature plan and produce a structured handoff for code review and verification.
---
# Feature Implementation

## Purpose

Implement an approved feature plan while following existing project
architecture, conventions, and constraints.

Implementation executes the decisions made during discovery and planning.

Do not redesign the feature, repeat broad discovery, or expand the scope
unless the approved plan cannot be implemented correctly.

## Inputs

Use:

- The approved implementation plan.
- `AGENTS.md`.
- Reference documentation identified by the plan or needed for the
  components being modified.
- Existing source code relevant to the planned changes.

Treat the approved plan as the primary implementation handoff.

Do not repeat discovery or planning unless the plan is incomplete,
incorrect, or incompatible with the actual codebase.

## Process

1. Read the complete implementation plan.
2. Identify the planned implementation steps and their dependencies.
3. Read the existing code directly relevant to the first change.
4. Consult applicable project guidance before modifying that area.
5. Implement the plan in dependency order.
6. Follow existing project patterns and reuse existing abstractions where
   specified by the plan or clearly appropriate.
7. Add or update the tests specified by the plan.
8. Perform lightweight checks while implementing when useful, such as
   compiling an affected module or running a targeted test.
9. Review the completed changes against the approved plan.
10. Produce an implementation handoff for verification.

## Implementation Guidelines

### Follow the Plan

Implement the architecture, data flow, components, and behavior defined
by the approved plan.

Do not silently:

- Change the planned architecture.
- Introduce new project-wide conventions.
- Add unrelated functionality.
- Perform unrelated refactoring.
- Replace existing abstractions with new ones.
- Expand the scope of the feature.

Small implementation-level decisions may be made when they do not alter
the planned architecture or behavior.

### Follow Existing Project Conventions

Apply the relevant rules from `AGENTS.md` and applicable reference
documentation.

When modifying existing code, preserve established patterns unless the
plan explicitly requires changing them.

### Keep Changes Focused

Prefer the smallest coherent set of changes that implements the plan.

Do not clean up unrelated code simply because it was encountered during
implementation.

### Handle Plan Problems Explicitly

If implementation reveals that the plan is incorrect or cannot be
implemented as written, determine whether the issue is minor or
significant.

Minor implementation details may be resolved directly when they:

- Do not change architecture.
- Do not change requirements.
- Do not change public contracts unexpectedly.
- Do not materially expand scope.

Stop implementation and report the issue when resolving it would require:

- An architectural decision.
- A significant change to the planned data flow.
- A new module or dependency not anticipated by the plan.
- A significant contract change.
- A requirements decision.
- A substantial scope increase.

Do not silently redesign the plan.

## Tests

Implement the tests identified by the plan.

Tests should verify behavior introduced or changed by the feature.

Follow existing project testing conventions and reuse established test
infrastructure where appropriate.

Do not add tests solely to reproduce implementation details.

## Implementation Review

For Kotlin or Gradle Kotlin changes, run before independent code review:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck :app:detektDebug
```

Resolve failures before handoff. Repeat after code-review corrections.

Before handing off to verification, compare the completed changes against
the approved plan.

Confirm:

- Planned components were added or modified.
- Planned behavior was implemented.
- Planned data/state flow was preserved.
- Required tests were added or updated.
- No unnecessary changes were introduced.

Do not perform the full verification workflow here unless explicitly
requested. Verification is the responsibility of the next workflow stage.

## Output

Produce the following implementation handoff:

### Implemented

Summarize the completed behavior and major changes.

### Changed Areas

List the modules and major components that were added, modified, or
removed.

### Tests Added / Updated

List the tests created or changed and what behavior they cover.

### Plan Deviations

List any differences from the approved plan and explain why they were
necessary.

Write `None` when there were no deviations.

### Verification Notes

Provide information useful to the verification stage, including:

- Relevant modules.
- Relevant test suites.
- Build or test commands identified by the plan.
- Areas that deserve manual or targeted verification.

### Remaining Issues

List unresolved implementation issues or blockers.

Write `None` when implementation completed without known issues.

## Handoff Requirements

The verification stage should be able to determine what changed, what
tests exist, and what needs to be checked without repeating implementation
analysis.

Implementation is complete when the approved plan has been executed and
the implementation handoff has been produced.

Do not claim that the feature is fully verified until the verification
stage has completed.
