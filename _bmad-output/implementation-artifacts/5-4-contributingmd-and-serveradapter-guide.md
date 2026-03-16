# Story 5.4: CONTRIBUTING.md & ServerAdapter Guide

Status: ready-for-dev

## Story

As a contributor,
I want a documented guide for adding a new ServerAdapter,
So that I can extend jsf-autoreload to support my application server.

## Acceptance Criteria

1. `CONTRIBUTING.md` includes a dedicated "How to add a new ServerAdapter" section
2. The guide walks through the `ServerAdapter` interface (5 methods), explains each method's contract, and points to `LibertyServerAdapter` and `TomcatServerAdapter` as reference implementations
3. The guide documents where server-specific exploded WAR output directories are typically located, how to wire the adapter into the build plugin DSL, and how to test without a real server installation
4. `CONTRIBUTING.md` documents the PR review process, coding conventions, and test expectations

## Tasks / Subtasks

- [ ] Task 1: Create CONTRIBUTING.md structure (AC: #4)
  - [ ] 1.1 Create `CONTRIBUTING.md` in project root
  - [ ] 1.2 Section: Getting Started (clone, build, test)
  - [ ] 1.3 Section: Development Setup (JDK, Gradle, IDE)
  - [ ] 1.4 Section: Coding Conventions (reference architecture document patterns)
  - [ ] 1.5 Section: Testing Expectations (unit + integration, JUnit 5, no Hamcrest/AssertJ)
  - [ ] 1.6 Section: PR Review Process (what reviewers look for, response time expectations)
- [ ] Task 2: Write ServerAdapter contribution guide (AC: #1, #2, #3)
  - [ ] 2.1 Section: "How to Add a New ServerAdapter"
  - [ ] 2.2 Explain the `ServerAdapter` interface — all 5 methods with contracts:
    - `isRunning()` — health check pattern
    - `getHttpPort()` — port retrieval
    - `getContextRoot()` — context root retrieval
    - `resolveOutputDir(serverName, projectDir)` — exploded WAR path resolution
    - `writeServerConfig(params)` — JSF refresh config writing
  - [ ] 2.3 Walk through creating a new adapter (e.g., `WildFlyServerAdapter`) step by step
  - [ ] 2.4 Reference implementations: `LibertyServerAdapter` and `TomcatServerAdapter`
  - [ ] 2.5 Document common server deployment models (where exploded WARs live)
  - [ ] 2.6 Explain how to wire the adapter into the build plugin DSL
  - [ ] 2.7 Testing guide: use `@TempDir` + `MockWebServer`, no real server needed
  - [ ] 2.8 File side-effect documentation requirement

## Dev Notes

### Target Audience: Journey 3 (Kenji)

The contributor guide enables the Kenji persona — a senior Java developer who wants to add WildFly support. The guide should be practical: "here's the interface, here's a reference implementation, here's how to test, here's how to submit."

### ServerAdapter Interface (5 Methods)

```java
public interface ServerAdapter {
    boolean isRunning();
    int getHttpPort();
    String getContextRoot();
    Path resolveOutputDir(String serverName, Path projectDir);
    void writeServerConfig(ServerConfigParams params);
}
```

### Key Coding Conventions to Document

- No `var`, no wildcard imports, explicit types
- JUL logging in core module
- `[JSF Autoreload]` prefix for user-facing messages
- Exceptions: `JsfAutoreloadException` / `JsfAutoreloadConfigException`
- Tests: JUnit 5 only, `@TempDir`, `CountDownLatch`, no `Thread.sleep()`
- Package: `it.bstz.jsfautoreload.server.{servername}`

### Project Structure Notes

- New: `CONTRIBUTING.md` in project root
- Depends on: All previous epics (the guide documents what exists)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Implementation Patterns & Consistency Rules]
- [Source: _bmad-output/planning-artifacts/architecture.md#ServerAdapter Interface]
- [Source: _bmad-output/planning-artifacts/architecture.md#Test Organization & Conventions]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 5.4]
- [Source: _bmad-output/planning-artifacts/prd.md#FR39 — documented ServerAdapter guide]
- [Source: _bmad-output/planning-artifacts/prd.md#Journey 3: Kenji]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
