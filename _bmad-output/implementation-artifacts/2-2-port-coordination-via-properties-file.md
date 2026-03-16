# Story 2.2: Port Coordination via Properties File

Status: ready-for-dev

## Story

As a developer,
I want the plugin and runtime filter to agree on the WebSocket port without server-specific configuration,
So that port coordination works on any Servlet 3.0+ container.

## Acceptance Criteria

1. `jsfPrepare` writes `jsf-autoreload.properties` to exploded WAR's `WEB-INF/classes/` containing `port={configured_port}`
2. Existing `jsf-autoreload.properties` is overwritten with current port on repeated runs (idempotent)
3. The runtime filter reads the port from the classpath properties file at init time

## Tasks / Subtasks

- [ ] Task 1: Write properties file in prepare step (AC: #1, #2)
  - [ ] 1.1 In `JsfPrepareTask.java`, add logic to write `jsf-autoreload.properties` to `{outputDir}/WEB-INF/classes/`
  - [ ] 1.2 File content: single line `port={configured_port}`
  - [ ] 1.3 Create parent directories if needed
  - [ ] 1.4 Overwrite existing file (idempotent)
  - [ ] 1.5 Use `java.nio.file.Files.writeString()` for file writing
- [ ] Task 2: Verify runtime filter reads the file (AC: #3)
  - [ ] 2.1 This is implemented in Story 2.3 — this story only ensures the file is written correctly
  - [ ] 2.2 Write integration test: after jsfPrepare, verify file exists at correct path with correct content
- [ ] Task 3: Update tests (AC: #1, #2)
  - [ ] 3.1 Update `JsfAutoreloadPluginIntegrationTest.java` or create new test
  - [ ] 3.2 Test: after jsfPrepare, `WEB-INF/classes/jsf-autoreload.properties` exists
  - [ ] 3.3 Test: file contains `port=35729` (default) or custom port
  - [ ] 3.4 Test: re-running jsfPrepare overwrites the file

## Dev Notes

### Architecture Decision: Properties File Port Coordination

This replaces the Liberty-specific `jvm.options` mechanism. The new approach:
- Prepare step writes `jsf-autoreload.properties` to `WEB-INF/classes/`
- Runtime filter reads from classpath at init time
- Server-agnostic — works on Liberty, Tomcat, any Servlet 3.0+ container

### Properties File Format

File: `{outputDir}/WEB-INF/classes/jsf-autoreload.properties`
Content:
```properties
port=35729
```

Simple single-key file. No comments needed (machine-generated).

### Integration with JsfPrepareTask

This is a small addition to `JsfPrepareTask.java`. The prepare task already:
- Copies runtime JAR to `WEB-INF/lib`
- Calls `ServerAdapter.writeServerConfig()` for JSF config

Now also:
- Writes `jsf-autoreload.properties` to `WEB-INF/classes/`

### Removing Old jvm.options Mechanism

The old `JsfPrepareTask` wrote port to Liberty's `jvm.options` file. This should be removed in favor of the properties file approach. The `LibertyServerAdapter.writeServerConfig()` handles Liberty-specific config (bootstrap.properties for JSF refresh), NOT port coordination.

### Project Structure Notes

- Modified: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java`
- Depends on: Story 2.1 (JsfPrepareTask refactored to use core)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Port Coordination]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 2.2]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java — current prepare logic]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
