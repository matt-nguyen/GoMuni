---
name: feature-development-orchestration
description: Implement a non-trivial Android feature using the project's discovery, planning, implementation, independent code-review, and verification workflow.
---

# Feature Development

## Purpose

Coordinate the complete feature-development workflow.

Use the specialized workflow skills for each phase rather than performing
all phases as one undifferentiated task.

## Workflow

Execute phases in this order:

1. Discovery
2. Planning
3. Implementation
4. Independent code review
5. Verification

Each phase consumes the handoff produced by the previous phase.

---

## Phase 1: Discovery

Use the `feature-discovery` skill.

Provide it with:

- The user's feature request.
- `AGENTS.md`.
- Relevant repository access.
- Applicable project documentation.

Discovery must not implement the feature.

Produce the discovery handoff required by the discovery skill.

---

## Phase 2: Planning

Use the `feature-planning` skill.

Provide it with:

- The feature requirements.
- Discovery handoff.
- `AGENTS.md`.
- Relevant project documentation.

Do not repeat broad discovery unless the discovery handoff is insufficient.

Planning must not modify production code.

Produce the planning handoff required by the planning skill.

### Plan Approval Gate

After producing the planning handoff:

1. Present the implementation plan to the user.
2. Stop and ask for explicit approval before implementation.
3. Do not modify production code while waiting for approval.
4. If the user requests changes, revise the plan and present it again.
5. Proceed to Implementation only after the user explicitly approves the plan.

---

## Phase 3: Implementation

Use the `feature-implementation` skill.

Provide it with:

- The approved planning handoff.
- `AGENTS.md`.
- Relevant project documentation and source code.

Implement the approved plan.

Do not repeat broad discovery or redesign the feature unless the plan
cannot be implemented correctly.

Produce the implementation handoff required by the implementation skill.

---

## Phase 4: Independent Code Review

After implementation, require passing `spotlessCheck :app:detektDebug`
for Kotlin or Gradle Kotlin changes. Resolve failures before review.

Delegate review to a fresh `code-reviewer` subagent configured in
`.codex/agents/code-reviewer.toml`, using high reasoning for every review.

The reviewer must have an independent context and must not receive the
discovery, planning, or implementation conversation/reasoning history.

Provide the reviewer with:

- The repository `AGENTS.md`.
- The approved planning handoff.
- Access to the current repository.
- The current Git diff.
- Passing prerequisite check results for the current changes.
- Relevant project reference documentation as needed.

Do not provide the implementation reasoning.

The reviewer must:

1. Read `AGENTS.md`.
2. Read the approved plan.
3. Inspect the current Git diff.
4. Inspect relevant surrounding source code.
5. Consult applicable project reference documentation.
6. Review correctness, architecture, project conventions, code quality,
   and tests.
7. Produce a structured review handoff.
8. Not modify code.

If the result is `CHANGES REQUIRED`:

1. Present the findings to the user and wait for direction before fixes.
2. Return authorized corrections to the implementation phase.
3. Repeat formatting and prerequisite checks after corrections.
4. Spawn a new fresh-context `code-reviewer` to review the complete current diff.

If the result is `BLOCKED`, report the blocker and resolve it before
continuing to verification. Use the reviewer's structured handoff.

Repeat until the implementation is approved or a blocking issue requires
returning to planning.

---

## Phase 5: Verification

After code review is approved, use the `feature-verification` skill.

Provide it with:

- The approved planning handoff.
- Implementation handoff.
- Final code-review handoff.
- Applicable project verification instructions.

Verification must focus on proving that the reviewed implementation works.

Do not repeat the general code review.

Run applicable:

- Compilation.
- Targeted tests.
- Module tests.
- Lint/static analysis.
- Instrumentation/UI tests when required.
- Runtime/manual verification when required.

Produce the verification handoff required by the verification skill.

---

## Failure Routing

Route problems to the phase responsible for resolving them:

- Incorrect understanding of existing system → Discovery.
- Incorrect or incomplete design → Planning.
- Implementation defect → Implementation.
- Code quality/review finding → Implementation.
- Failed behavioral or automated verification → Implementation unless
  evidence indicates an earlier phase is responsible.

After corrections, repeat downstream phases when their previous results
may no longer be valid.

---

## Completion

The feature workflow is complete only when:

- Discovery completed.
- Planning completed.
- Implementation completed.
- Independent code review approved the current implementation.
- Verification passed or passed with acceptable documented notes.

Provide a concise final summary containing:

- What was implemented.
- Code-review result.
- Verification result.
- Any remaining notes.
