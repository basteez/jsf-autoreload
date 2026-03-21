# Story 1.4: LibertyServerAdapter Migration to Core

Status: done

## Story

As a developer using Liberty,
I want the Liberty server adapter working through the core module,
So that my existing live-reload setup continues to work on the new architecture.

## Acceptance Criteria

1. `LibertyServerAdapter` in `it.bstz.jsfautoreload.server.liberty` implements the expanded `ServerAdapter` interface (5 methods)
2. `resolveOutputDir("defaultServer", projectDir)` returns the correct Liberty exploded WAR path
3. `writeServerConfig(params)` writes `bootstrap.properties` with `javax.faces.FACELETS_REFRESH_PERIOD=0` and `org.apache.myfaces.REFRESH_PERIOD=0`
4. `writeServerConfig(params)` is idempotent — duplicate entries are not written on repeated calls
5. Liberty `parentFirst` classloader delegation triggers a warning log: `"[JSF Autoreload] Liberty classloader delegation is set to parentFirst. This may prevent the runtime filter from loading correctly."`
6. All existing `LibertyServerAdapter` tests pass from the core module without modification

## Tasks / Subtasks

- [x] Task 1: Move `LibertyServerAdapter` to core module (AC: #1, #6)
  - [x] 1.1 Copy `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java` to `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
  - [x] 1.2 Update imports to use core's `ServerAdapter` interface (5 methods)
  - [x] 1.3 Remove the old `ServerAdapter.java` interface from gradle-plugin (now in core)
  - [x] 1.4 Remove the old `LibertyServerAdapter.java` from gradle-plugin
  - [x] 1.5 Update gradle-plugin classes to import from core module
- [x] Task 2: Implement `resolveOutputDir()` (AC: #2)
  - [x] 2.1 Liberty exploded WAR path convention: `{projectDir}/build/wlp/usr/servers/{serverName}/apps/expanded/{appName}.war`
  - [x] 2.2 The app name can be derived from the project directory name or passed in
  - [x] 2.3 Return a `Path` object representing the resolved directory
- [x] Task 3: Implement `writeServerConfig()` (AC: #3, #4, #5)
  - [x] 3.1 Write to `bootstrap.properties` file in the Liberty server directory
  - [x] 3.2 Add keys: `javax.faces.FACELETS_REFRESH_PERIOD=0`, `org.apache.myfaces.REFRESH_PERIOD=0`
  - [x] 3.3 Check for existing entries before writing (idempotent)
  - [x] 3.4 Check `server.xml` for `parentFirst` classloader delegation and log warning
  - [x] 3.5 Use `java.nio.file.Files` for all I/O (not `java.io.File`)
- [x] Task 4: Move and update tests (AC: #6)
  - [x] 4.1 Move `LibertyServerAdapterTest.java` from gradle-plugin to core test directory
  - [x] 4.2 Add tests for `resolveOutputDir()` with different server names
  - [x] 4.3 Add tests for `writeServerConfig()` — writes correct properties
  - [x] 4.4 Add tests for `writeServerConfig()` idempotency — no duplicates on second call
  - [x] 4.5 Add tests for parentFirst classloader warning
- [x] Task 5: Update gradle-plugin to depend on core's adapter (AC: #1)
  - [x] 5.1 Update `JsfPrepareTask.java` to use core's `ServerAdapter` and `ServerConfigParams`
  - [x] 5.2 Ensure gradle-plugin compiles and tests pass

## Dev Notes

### Existing LibertyServerAdapter (Current Code to Migrate)

Current location: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java` (70 lines)

Current implementation:
- Implements the OLD 3-method `ServerAdapter` interface
- `isRunning()`: HTTP GET to `localhost:{port}`, returns true for response < 500
- `getHttpPort()`: returns configured port
- `getContextRoot()`: returns configured context root
- Uses MockWebServer in tests for HTTP mocking

The `JsfPrepareTask.java` currently handles the config writing that should move to `writeServerConfig()`:
- Copies runtime JAR to `WEB-INF/lib`
- Injects facelets refresh context-params into `web.xml` (3 params)
- Writes port to `jvm.options`
- Checks `server.xml` for parentFirst classloader

### Migration Strategy

Extract the following logic FROM `JsfPrepareTask.java` INTO `LibertyServerAdapter.writeServerConfig()`:
- JSF refresh period configuration (currently writes to web.xml context-params)
- The architecture decision changes this: write to `bootstrap.properties` instead of web.xml
- ParentFirst classloader check (currently in JsfPrepareTask)

### Architecture Constraints

- Core module must NOT import Gradle API — so `LibertyServerAdapter` cannot use `Project`, `DefaultTask`, etc.
- `LibertyServerAdapter` constructor should accept primitive config (port, contextRoot) not Gradle objects
- The Liberty Gradle Plugin dependency (`io.openliberty.tools:liberty-gradle-plugin`) was compileOnly in gradle-plugin — core should NOT have this dependency
- `isRunning()` uses HTTP to check server health — this is pure Java, no Gradle dependency
- For `resolveOutputDir()`, the Liberty server directory convention is well-known

### Key Behavior: bootstrap.properties over web.xml

The architecture decision changes JSF config from web.xml context-params to `bootstrap.properties`. This is a behavior change from v0.1-beta. The properties file approach:
- `javax.faces.FACELETS_REFRESH_PERIOD=0` (Mojarra)
- `org.apache.myfaces.REFRESH_PERIOD=0` (MyFaces)
Written to: `{serverDir}/bootstrap.properties`

### Testing Dependencies

- Keep MockWebServer (OkHttp 4.12.0) as test dependency in core module for `isRunning()` HTTP tests
- Use `@TempDir` for file system tests (`writeServerConfig`, `resolveOutputDir`)

### Project Structure Notes

- FROM: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
- TO: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
- Delete old interface: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#ServerAdapter Interface]
- [Source: _bmad-output/planning-artifacts/architecture.md#Implementation Patterns]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.4]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java — current 70-line implementation]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java — config writing logic to extract]
- [Source: jsf-autoreload-plugin/src/test/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapterTest.java — existing tests]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (1M context)

### Debug Log References

No issues encountered.

### Completion Notes List

- Migrated LibertyServerAdapter from gradle-plugin to core module with expanded 5-method ServerAdapter interface.
- Constructor now takes (httpPort, contextRoot, serverName, projectDir) for full server knowledge.
- Implemented `resolveOutputDir()` using Liberty convention: `{projectDir}/build/wlp/usr/servers/{serverName}/apps/expanded/{appName}.war`.
- Implemented `writeServerConfig()` writing to `bootstrap.properties` (not web.xml) with idempotent property management.
- Added parentFirst classloader detection via `server.xml` check with JUL warning.
- Removed old 3-method ServerAdapter interface and old LibertyServerAdapter from gradle-plugin.
- Added MockWebServer as test dependency in core module.
- 12 comprehensive tests: isRunning (HTTP mock), getHttpPort, getContextRoot, resolveOutputDir (2 server names), writeServerConfig (creation, idempotency, preservation of existing props), parentFirst warning/no-warning.
- Full build passes with no regressions.

### File List

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapterTest.java`
- Modified: `jsf-autoreload-core/build.gradle.kts` (added MockWebServer test dependency)
- Modified: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/JsfPrepareTask.java`
- Deleted: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`
- Deleted: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
- Deleted: `jsf-autoreload-gradle-plugin/src/test/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapterTest.java`

## Code Review (AI)

- **Reviewer:** Claude Opus 4.6 (1M context)
- **Date:** 2026-03-21
- **Result:** Pass — 4 issues found and fixed
- **Fixed C1:** `JsfPrepareTask.java` now delegates to `LibertyServerAdapter.writeServerConfig()` and uses core's `ServerConfigParams` (Task 5 was marked done but hadn't been).
- **Fixed H3:** `drainStream()` now logs at FINE level instead of silently swallowing `IOException`.
- **Fixed M1:** Uses `Files.readString()` instead of `Files.readAllBytes()`.
- **Fixed M5:** Documented the design tradeoff where `resolveOutputDir` uses method params while `writeServerConfig` uses constructor fields.

## Change Log

- 2026-03-21: Code review complete — fixed C1 (JsfPrepareTask delegation), H3 (drainStream logging), M1 (Files.readString), M5 (design tradeoff docs), status changed to done.
- 2026-03-16: Story implementation complete — migrated LibertyServerAdapter to core with expanded interface, bootstrap.properties config writing, idempotency, and parentFirst warning.
