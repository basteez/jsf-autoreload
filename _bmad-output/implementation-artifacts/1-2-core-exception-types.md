# Story 1.2: Core Exception Types

Status: done

## Story

As a developer,
I want clear, typed exceptions for configuration and runtime errors,
So that error messages are actionable and tell me exactly what to fix.

## Acceptance Criteria

1. `JsfAutoreloadException` extends `RuntimeException`, message follows format `"[JSF Autoreload] {what went wrong}. {how to fix it}."`
2. `JsfAutoreloadConfigException` extends `JsfAutoreloadException` for configuration/validation errors with actionable messages
3. Both exceptions support a `cause` parameter preserved via `getCause()`
4. No Gradle, Maven, or Servlet API imports in either exception class

## Tasks / Subtasks

- [x] Task 1: Create `JsfAutoreloadException` (AC: #1, #3, #4)
  - [x] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadException.java`
  - [x] 1.2 Extend `RuntimeException`
  - [x] 1.3 Provide constructors: `(String message)` and `(String message, Throwable cause)`
  - [x] 1.4 Message format: `"[JSF Autoreload] {what}. {fix}."`
- [x] Task 2: Create `JsfAutoreloadConfigException` (AC: #2, #3, #4)
  - [x] 2.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadConfigException.java`
  - [x] 2.2 Extend `JsfAutoreloadException`
  - [x] 2.3 Provide constructors: `(String message)` and `(String message, Throwable cause)`
- [x] Task 3: Write unit tests (AC: #1, #2, #3)
  - [x] 3.1 Create `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadExceptionTest.java`
  - [x] 3.2 Test message format includes `[JSF Autoreload]` prefix
  - [x] 3.3 Test cause preservation via `getCause()`
  - [x] 3.4 Test `JsfAutoreloadConfigException` is a subtype of `JsfAutoreloadException`
  - [x] 3.5 Test `JsfAutoreloadException` is a subtype of `RuntimeException`

## Dev Notes

### Architecture Patterns

- Error message format is a project-wide convention: `"[JSF Autoreload] {what went wrong}. {how to fix it}."`
- Example: `"[JSF Autoreload] Output directory not found: /path/to/dir. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your server name."`
- Example: `"[JSF Autoreload] Port 35729 is already in use. Configure a different port via jsfAutoreload { port = XXXX }."`
- These exceptions will be used by ALL core classes and translated by plugin modules:
  - Gradle: `JsfAutoreloadException` -> `GradleException`
  - Maven: `JsfAutoreloadException` -> `MojoExecutionException`

### Code Conventions

- No `var` keyword — explicit types everywhere
- No wildcard imports
- Package: `it.bstz.jsfautoreload` (root package of core module)
- Test method naming: camelCase describing behavior, e.g., `preservesCauseWhenProvided()`
- Assertions: JUnit 5 only (`assertEquals`, `assertTrue`, `assertThrows`)

### This is a Simple Story

Two small classes with standard constructors. The message format convention is the key deliverable — it establishes the pattern all subsequent stories will follow.

### Project Structure Notes

- Location: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/`
- Tests: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/`
- Depends on Story 1.1 (core module must exist)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Error Handling & Exceptions]
- [Source: _bmad-output/planning-artifacts/architecture.md#Java Code Conventions]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.2]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (1M context)

### Debug Log References

No issues encountered.

### Completion Notes List

- Created `JsfAutoreloadException` extending `RuntimeException` with two constructors (message-only and message+cause). Establishes the `[JSF Autoreload]` message prefix convention.
- Created `JsfAutoreloadConfigException` extending `JsfAutoreloadException` for configuration/validation errors, same constructor pattern.
- Both classes reside in `it.bstz.jsfautoreload` package with zero external imports (AC #4 satisfied).
- Wrote 9 unit tests covering: message prefix verification, cause preservation, null cause when not provided, subtype hierarchy (ConfigException -> JsfAutoreloadException -> RuntimeException), and catchability.
- All tests pass. Full project build successful with no regressions.

### File List

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadException.java`
- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadConfigException.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadExceptionTest.java`

## Code Review (AI)

- **Reviewer:** Claude Opus 4.6 (1M context)
- **Date:** 2026-03-21
- **Result:** Pass — no issues found
- Clean implementation. No issues found. All ACs verified.

## Change Log

- 2026-03-21: Code review complete — no issues found, status changed to done.
- 2026-03-16: Story implementation complete — created core exception types with message format convention and comprehensive test coverage.
