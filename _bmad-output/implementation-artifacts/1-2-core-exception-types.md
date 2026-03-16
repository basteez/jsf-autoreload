# Story 1.2: Core Exception Types

Status: ready-for-dev

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

- [ ] Task 1: Create `JsfAutoreloadException` (AC: #1, #3, #4)
  - [ ] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadException.java`
  - [ ] 1.2 Extend `RuntimeException`
  - [ ] 1.3 Provide constructors: `(String message)` and `(String message, Throwable cause)`
  - [ ] 1.4 Message format: `"[JSF Autoreload] {what}. {fix}."`
- [ ] Task 2: Create `JsfAutoreloadConfigException` (AC: #2, #3, #4)
  - [ ] 2.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/JsfAutoreloadConfigException.java`
  - [ ] 2.2 Extend `JsfAutoreloadException`
  - [ ] 2.3 Provide constructors: `(String message)` and `(String message, Throwable cause)`
- [ ] Task 3: Write unit tests (AC: #1, #2, #3)
  - [ ] 3.1 Create `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/JsfAutoreloadExceptionTest.java`
  - [ ] 3.2 Test message format includes `[JSF Autoreload]` prefix
  - [ ] 3.3 Test cause preservation via `getCause()`
  - [ ] 3.4 Test `JsfAutoreloadConfigException` is a subtype of `JsfAutoreloadException`
  - [ ] 3.5 Test `JsfAutoreloadException` is a subtype of `RuntimeException`

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

### Debug Log References

### Completion Notes List

### File List
