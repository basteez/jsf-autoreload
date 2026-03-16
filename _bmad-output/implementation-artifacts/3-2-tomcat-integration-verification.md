# Story 3.2: Tomcat Integration Verification

Status: ready-for-dev

## Story

As a developer using Tomcat + Gradle,
I want the full dev loop verified against a Tomcat deployment model,
So that I'm confident the server-agnostic architecture works for my stack.

## Acceptance Criteria

1. Prepare step injects runtime JAR into Tomcat's exploded WAR `WEB-INF/lib`, writes `jsf-autoreload.properties` to `WEB-INF/classes`, and writes Tomcat-specific JSF config
2. `DevServer` with `TomcatServerAdapter`: file modified in watch directory -> copied to Tomcat output directory preserving relative path + browser reload triggered
3. `DevServer` orchestrator works identically with both `LibertyServerAdapter` and `TomcatServerAdapter` — no server-specific code paths in core

## Tasks / Subtasks

- [ ] Task 1: Create Tomcat integration test (AC: #1)
  - [ ] 1.1 Create integration test in gradle-plugin module
  - [ ] 1.2 Test: jsfPrepare with Tomcat adapter produces runtime JAR in `WEB-INF/lib`
  - [ ] 1.3 Test: jsfPrepare produces `jsf-autoreload.properties` in `WEB-INF/classes`
  - [ ] 1.4 Test: jsfPrepare writes Tomcat-specific JSF configuration
- [ ] Task 2: Verify DevServer with TomcatServerAdapter (AC: #2)
  - [ ] 2.1 Unit test in core: `DevServer` + mock `TomcatServerAdapter` + temp directories
  - [ ] 2.2 Modify file in watch dir -> verify copy to output dir + broadcast
  - [ ] 2.3 Verify relative path preservation
- [ ] Task 3: Verify server-agnostic orchestrator (AC: #3)
  - [ ] 3.1 Run same `DevServer` test with both `LibertyServerAdapter` and `TomcatServerAdapter`
  - [ ] 3.2 Verify identical behavior — same callbacks, same copy logic, same broadcast
  - [ ] 3.3 Confirm no `instanceof` checks or server-specific branching in `DevServer`

## Dev Notes

### This is a Verification Story

The primary goal is confidence that the architecture works for multiple servers. The code was built in previous stories — this story verifies integration.

### Server-Agnostic Validation

The key test: `DevServer` should work identically regardless of which `ServerAdapter` implementation is passed. No `instanceof TomcatServerAdapter` or `instanceof LibertyServerAdapter` in any core code. If there are, it's an architecture violation.

### Test Strategy

- Unit tests with mock/real adapters against temp directories
- No need for a real Tomcat installation — use `@TempDir` and verify file operations
- `MockWebServer` for `isRunning()` HTTP checks

### Project Structure Notes

- New/Modified: tests in both core and gradle-plugin modules
- Depends on: Story 3.1 (TomcatServerAdapter), Story 2.1 (Gradle plugin wiring)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Core Module API Design — "Core never constructs ServerAdapter instances"]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.2]
- [Source: _bmad-output/planning-artifacts/prd.md — "Server-agnostic architecture proven with two adapters"]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
