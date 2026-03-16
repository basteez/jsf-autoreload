# Story 1.4: LibertyServerAdapter Migration to Core

Status: ready-for-dev

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

- [ ] Task 1: Move `LibertyServerAdapter` to core module (AC: #1, #6)
  - [ ] 1.1 Copy `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java` to `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java`
  - [ ] 1.2 Update imports to use core's `ServerAdapter` interface (5 methods)
  - [ ] 1.3 Remove the old `ServerAdapter.java` interface from gradle-plugin (now in core)
  - [ ] 1.4 Remove the old `LibertyServerAdapter.java` from gradle-plugin
  - [ ] 1.5 Update gradle-plugin classes to import from core module
- [ ] Task 2: Implement `resolveOutputDir()` (AC: #2)
  - [ ] 2.1 Liberty exploded WAR path convention: `{projectDir}/build/wlp/usr/servers/{serverName}/apps/expanded/{appName}.war`
  - [ ] 2.2 The app name can be derived from the project directory name or passed in
  - [ ] 2.3 Return a `Path` object representing the resolved directory
- [ ] Task 3: Implement `writeServerConfig()` (AC: #3, #4, #5)
  - [ ] 3.1 Write to `bootstrap.properties` file in the Liberty server directory
  - [ ] 3.2 Add keys: `javax.faces.FACELETS_REFRESH_PERIOD=0`, `org.apache.myfaces.REFRESH_PERIOD=0`
  - [ ] 3.3 Check for existing entries before writing (idempotent)
  - [ ] 3.4 Check `server.xml` for `parentFirst` classloader delegation and log warning
  - [ ] 3.5 Use `java.nio.file.Files` for all I/O (not `java.io.File`)
- [ ] Task 4: Move and update tests (AC: #6)
  - [ ] 4.1 Move `LibertyServerAdapterTest.java` from gradle-plugin to core test directory
  - [ ] 4.2 Add tests for `resolveOutputDir()` with different server names
  - [ ] 4.3 Add tests for `writeServerConfig()` — writes correct properties
  - [ ] 4.4 Add tests for `writeServerConfig()` idempotency — no duplicates on second call
  - [ ] 4.5 Add tests for parentFirst classloader warning
- [ ] Task 5: Update gradle-plugin to depend on core's adapter (AC: #1)
  - [ ] 5.1 Update `JsfPrepareTask.java` to use core's `ServerAdapter` and `ServerConfigParams`
  - [ ] 5.2 Ensure gradle-plugin compiles and tests pass

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

### Debug Log References

### Completion Notes List

### File List
