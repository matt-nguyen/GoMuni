---
name: feature-verification
description: Verify a reviewed Android feature implementation using builds, tests, static checks, and behavioral verification.
---
# Feature Verification

## Purpose

Verify that the reviewed feature implementation behaves as intended and
passes the applicable automated and manual checks.

Code quality and general diff review are handled by the preceding code
review stage.

Verification focuses on evidence that the completed implementation works.

Do not modify production code unless explicitly instructed to fix a
verification failure.

## Inputs

Use:

- The approved implementation plan.
- The implementation handoff.
- The code-review handoff.
- `AGENTS.md` when needed to determine required verification.
- Relevant tests and verification tooling.

Use the approved plan as the source of truth for intended behavior.

Use the implementation handoff to understand what was changed.

Use the code-review handoff to identify risks, concerns, or areas requiring
targeted verification.

Do not repeat broad discovery, planning, implementation analysis, or the
completed code review.

## Process

1. Read the approved plan.
2. Read the implementation handoff.
3. Read the code-review handoff.
4. Determine which behavior and components require verification.
5. Determine the smallest sufficient set of verification checks.
6. Run targeted checks first.
7. Expand verification when change scope, risk, or failures justify it.
8. Confirm that the behavior required by the plan is covered by the
   available evidence.
9. Record failures and anything that could not be verified.
10. Produce the verification handoff.

## Verification Strategy

Use verification proportional to the change.

Prefer:

1. Compile affected modules.
2. Run tests directly related to changed behavior.
3. Run affected module test suites when appropriate.
4. Run applicable lint/static-analysis checks.
5. Run instrumentation/UI tests when required.
6. Perform applicable manual/runtime verification when automated checks
   cannot sufficiently verify the behavior.
7. Run broader project checks when required by project guidance or
   justified by the change's scope or risk.

Do not run expensive project-wide checks automatically when targeted
verification provides sufficient evidence.

## Behavioral Verification

Verify that the behavior required by the approved plan works as intended.

Pay particular attention to:

- Acceptance criteria.
- Important state transitions.
- Error and edge cases identified by the plan.
- Integration boundaries affected by the change.
- Risks identified during implementation or code review.

Verification should answer:

"Do we have sufficient evidence that the implemented feature works?"

## Test Execution

Run the relevant existing and newly added tests.

Record:

- Tests executed.
- Tests passed.
- Tests failed.
- Relevant tests that could not be executed.

Do not perform a general review of test implementation quality. That
belongs to code review.

If verification exposes missing behavioral coverage that prevents
confidence in the feature, report it as a finding.

## Static Checks

Run applicable mechanical checks such as:

- Compilation.
- Lint.
- Static analysis.
- Formatting checks.
- Other project-defined quality gates.

Follow project-specific verification commands when defined.

For Kotlin or Gradle Kotlin changes, rerun checks on the reviewed implementation:

```powershell
.\gradlew.bat spotlessCheck :app:detektDebug
```

Do not run `spotlessApply` here. Route formatting failures to implementation
for correction and renewed code review.

## Code Review Boundary

Do not repeat the preceding code review.

In particular, do not perform another general review of:

- Git diff quality.
- Code style.
- Naming.
- Architecture.
- Abstraction quality.
- Unrelated changes.
- Test implementation quality.

Inspect source code only when necessary to:

- Understand a verification failure.
- Determine which verification command or behavior applies.
- Gather evidence for a verification finding.

If verification reveals a problem that the code review missed, report it.
Do not ignore a defect merely because code review has already passed.

## Failure Handling

When a check fails:

1. Record the failing check or command.
2. Capture the relevant failure.
3. Determine whether the failure is related to the feature when possible.
4. Continue independent checks when useful.
5. Report the failure.

Do not silently modify production code to make verification pass.

Route findings appropriately:

- Implementation — implementation defect.
- Planning — incorrect or incomplete implementation design.
- Discovery — incorrect understanding of the existing system.
- Environment — verification cannot be completed because of tooling,
  device, dependency, or environment problems.

## Output

Produce the following verification handoff:

### Result

One of:

- PASS
- PASS WITH NOTES
- FAIL
- BLOCKED

### Behavior Verified

List the important feature behavior that was successfully verified and
the evidence used.

### Checks Performed

For each check report:

- Check or command.
- Scope.
- Result.

### Test Results

Summarize the relevant test execution and results.

### Findings

For each finding include:

- Problem.
- Evidence.
- Impact on verification.
- Recommended workflow stage to return to.

Write `None` when there are no findings.

### Unverified Areas

List behavior or checks that could not be verified and why.

Write `None` when all required areas were verified.

### Final Assessment

State whether sufficient evidence exists to complete the feature workflow.

Do not claim successful verification when required behavior or checks
remain unverified.
