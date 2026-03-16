# Story 2.1: Gradle Plugin Wiring to Core

Status: ready-for-dev

## Story

As a developer using Gradle,
I want to apply the jsf-autoreload plugin with a single declaration,
So that I get live-reload without complex build configuration.

## Acceptance Criteria

1. `plugins { id 'it.bstz.jsf-autoreload' }` registers `jsfAutoreload` extension block and `jsfDev`, `jsfPrepare` tasks
2. `JsfAutoreloadExtension` DSL values (port, serverName, outputDir, watchDirs) are mapped to `DevServerConfig` and passed to `DevServer.start()`
3. `jsfPrepare` injects runtime JAR into exploded WAR's `WEB-INF/lib` and calls `ServerAdapter.writeServerConfig()`
4. Task dependencies: `jsfPrepare` runs before server start, dev loop runs after server start
5. Core `JsfAutoreloadException` is caught and translated to `GradleException` with same message
6. Shadow JAR includes `jsf-autoreload-core` with `org.java_websocket` relocated

## Tasks / Subtasks

- [ ] Task 1: Refactor `JsfDevTask` to delegate to core `DevServer` (AC: #2)
  - [ ] 1.1 Update `JsfDevTask.java` to build `DevServerConfig` from extension properties
  - [ ] 1.2 Construct `LibertyServerAdapter` (or appropriate adapter) from extension `serverName`
  - [ ] 1.3 Call `DevServer.start(config)` instead of managing watcher/websocket directly
  - [ ] 1.4 Remove all direct FileChangeWatcher/DevWebSocketServer usage from JsfDevTask
  - [ ] 1.5 Keep Java class compilation logic (JavaSourceCompiler) in gradle-plugin — this is Gradle-specific
- [ ] Task 2: Refactor `JsfPrepareTask` to use core APIs (AC: #3)
  - [ ] 2.1 Update `JsfPrepareTask.java` to use `ServerAdapter.writeServerConfig(ServerConfigParams)`
  - [ ] 2.2 Remove direct bootstrap.properties/web.xml writing — delegate to adapter
  - [ ] 2.3 Keep runtime JAR copy logic (Gradle-specific file resolution)
  - [ ] 2.4 Write `jsf-autoreload.properties` to `WEB-INF/classes/` with port value (new: port coordination)
- [ ] Task 3: Add exception translation (AC: #5)
  - [ ] 3.1 Wrap `DevServer.start()` call in try-catch
  - [ ] 3.2 Catch `JsfAutoreloadException` -> throw `GradleException(e.getMessage(), e)`
  - [ ] 3.3 Catch `JsfAutoreloadConfigException` -> throw `GradleException(e.getMessage(), e)`
- [ ] Task 4: Update shadow JAR configuration (AC: #6)
  - [ ] 4.1 Ensure shadow JAR includes core module classes
  - [ ] 4.2 Relocate `org.java_websocket` as before
  - [ ] 4.3 Relocate `io.methvin` (directory-watcher) as before
  - [ ] 4.4 Verify relocated JAR works with integration tests
- [ ] Task 5: Update plugin registration and tests (AC: #1, #4)
  - [ ] 5.1 Verify plugin ID `it.bstz.jsf-autoreload` still registers correctly
  - [ ] 5.2 Update integration tests to verify new behavior
  - [ ] 5.3 Test extension -> DevServerConfig mapping
  - [ ] 5.4 Test exception translation

## Dev Notes

### Current JsfDevTask.java (293 lines) — What Stays vs What Moves

**MOVES to core `DevServer` (already done in Story 1.8):**
- WebSocket server start/stop
- File watcher creation
- File copy to output directory
- Reload broadcast
- Debounce logic
- Graceful shutdown

**STAYS in `JsfDevTask` (Gradle-specific):**
- Gradle task properties and annotations (`@Input`, `@TaskAction`, etc.)
- Extension property resolution
- `DevServerConfig` construction from Gradle extension values
- `ServerAdapter` construction (choosing Liberty/Tomcat based on config)
- Exception translation to `GradleException`
- Java source compilation via `JavaSourceCompiler` (optional, Gradle-specific)
- Runtime shutdown hook for Liberty server stop

### Current JsfPrepareTask.java (169 lines) — What Changes

**Current behavior:**
- Copies runtime JAR to `WEB-INF/lib`
- Injects context-params into `web.xml` (3 JSF refresh params)
- Writes port to `jvm.options` (Liberty-specific)
- Checks server.xml for parentFirst classloader

**New behavior:**
- Copies runtime JAR to `WEB-INF/lib` (UNCHANGED — Gradle-specific file resolution)
- Calls `ServerAdapter.writeServerConfig(params)` instead of direct web.xml/jvm.options manipulation
- Writes `jsf-autoreload.properties` to `WEB-INF/classes/` with `port={port}` (NEW — port coordination)
- ParentFirst check moves to ServerAdapter

### Plugin Logging

Use Gradle's `project.getLogger()` in plugin classes, NOT JUL. The architecture mandates build-tool native logging in plugin modules.

### Shadow Relocation

Current relocations in gradle-plugin `build.gradle.kts`:
```kotlin
relocate("org.java_websocket", "it.bstz.jsfautoreload.shadow.org.java_websocket")
relocate("io.methvin", "it.bstz.jsfautoreload.shadow.io.methvin")
```
These must continue to work after core inlining.

### Project Structure Notes

- Modified: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/JsfDevTask.java`
- Modified: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java`
- Modified: `jsf-autoreload-gradle-plugin/build.gradle.kts` (shadow config)
- Depends on: All Epic 1 stories (core module complete)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Gradle Plugin Module Boundary]
- [Source: _bmad-output/planning-artifacts/architecture.md#Shadow Relocation Strategy]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 2.1]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/JsfDevTask.java — current 293-line implementation]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java — current 169-line implementation]
- [Source: jsf-autoreload-plugin/build.gradle.kts — shadow/relocation config]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
