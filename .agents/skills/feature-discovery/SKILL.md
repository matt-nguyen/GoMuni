---
name: feature-discovery
description: Research an Android feature before planning by inspecting the repository, existing patterns, architecture, and relevant project documentation.
---
# Feature Discovery

## Purpose

Understand the existing system sufficiently for planning a feature.

Do not implement or design the solution.

## Process

1. Read the feature requirements.
2. Identify likely affected modules.
3. Inspect relevant existing implementation.
4. Trace current data/state flow.
5. Find analogous implementations.
6. Consult applicable project references.
7. Identify reusable components and constraints.
8. Identify unresolved questions.
9. Stop once planning has sufficient context.

## Open Questions

Identify unresolved questions discovered during analysis.

- Mark questions as `Blocking` only when planning cannot safely
  determine the implementation without an answer.
- Mark other uncertainties as `Non-blocking`.
- For non-blocking questions, state the assumption planning may use.
- Do not ask the developer questions that can be answered from
  existing code, project documentation, or established conventions.

If blocking questions exist, stop and request clarification before
handing off to planning.

After clarification, update the discovery output with the resolved
decisions.

## Output

Provide:
- Requirements understood
- Relevant modules
- Current data flow
- Existing patterns
- Reusable components
- Constraints
- Unknowns
- Relevant references