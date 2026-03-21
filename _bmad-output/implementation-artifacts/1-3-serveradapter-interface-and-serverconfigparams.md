# Story 1.3: ServerAdapter Interface & ServerConfigParams

Status: done

## Story

As a contributor,
I want a public ServerAdapter interface with clear method contracts,
So that I can implement support for a new application server.

## Acceptance Criteria

1. `ServerAdapter` in the core module declares 5 methods: `isRunning()`, `getHttpPort()`, `getContextRoot()`, `resolveOutputDir(String serverName, Path projectDir)`, `writeServerConfig(ServerConfigParams params)`
2. `ServerConfigParams` holds all configuration values needed by `writeServerConfig()` including JSF refresh period settings
3. Each `ServerAdapter` method has Javadoc documenting its contract, parameters, and exceptions
4. Core module compiles independently with no Gradle API, Maven API, or Servlet API imports

## Tasks / Subtasks

- [x] Task 1: Create expanded `ServerAdapter` interface in core (AC: #1, #3, #4)
  - [x] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`
  - [x] 1.2 Declare 5 methods with full Javadoc:
    - `boolean isRunning()` — checks if the application server is running
    - `int getHttpPort()` — returns the HTTP port of the running server
    - `String getContextRoot()` — returns the context root of the application
    - `Path resolveOutputDir(String serverName, Path projectDir)` — resolves the exploded WAR output directory
    - `void writeServerConfig(ServerConfigParams params)` — writes server-specific JSF configuration
  - [x] 1.3 Document exception behavior: methods may throw `JsfAutoreloadException`
- [x] Task 2: Create `ServerConfigParams` POJO (AC: #2)
  - [x] 2.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerConfigParams.java`
  - [x] 2.2 Include fields: `Path outputDir`, `int mojarraRefreshPeriod`, `int myfacesRefreshPeriod`, `int port`
  - [x] 2.3 Use builder pattern or constructor for immutability
  - [x] 2.4 All fields accessible via getters, no setters
- [x] Task 3: Write unit tests (AC: #1, #2)
  - [x] 3.1 Create `ServerConfigParamsTest.java` to verify construction and getter access
  - [x] 3.2 Verify `ServerConfigParams` is immutable (no setters, fields final)
- [x] Task 4: Update old `ServerAdapter` reference in gradle-plugin (AC: #4)
  - [x] 4.1 The old `ServerAdapter.java` in `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/` has only 3 methods — leave it in place for now; it will be replaced when LibertyServerAdapter migrates in Story 1.4
  - [x] 4.2 Ensure core compiles independently without any gradle-plugin code

## Dev Notes

### Existing ServerAdapter (Current State — DO NOT BREAK)

The existing `ServerAdapter.java` in `jsf-autoreload-gradle-plugin` has only 3 methods:
```java
public interface ServerAdapter {
    boolean isRunning();
    int getHttpPort();
    String getContextRoot();
}
```
This interface is used by `LibertyServerAdapter` in the gradle-plugin module. The new expanded interface in core will ADD `resolveOutputDir()` and `writeServerConfig()`. The old interface stays in gradle-plugin until Story 1.4 migrates everything.

### Architecture Decision: Interface Design

- Core never constructs `ServerAdapter` instances — plugin modules construct the adapter and pass it in
- Each implementation MUST document which files it creates/modifies (for testability without real server)
- `ServerConfigParams` holds values needed by `writeServerConfig()`:
  - `outputDir` — path to exploded WAR
  - `mojarraRefreshPeriod` — Mojarra FACELETS_REFRESH_PERIOD value (typically 0)
  - `myfacesRefreshPeriod` — MyFaces REFRESH_PERIOD value (typically 0)
  - `port` — WebSocket port for properties file writing

### Code Conventions

- Package: `it.bstz.jsfautoreload.server`
- No `var` keyword, no wildcard imports, explicit types
- Javadoc on all public interface methods
- `ServerConfigParams` should be immutable — final fields, no setters
- Consider builder pattern for `ServerConfigParams` for consistency with `DevServerConfig` (Story 1.5)

### Project Structure Notes

- New interface: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`
- New POJO: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerConfigParams.java`
- Old interface remains temporarily: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#ServerAdapter Interface]
- [Source: _bmad-output/planning-artifacts/architecture.md#Core Module API Design]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.3]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java — current 3-method interface]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (1M context)

### Debug Log References

No issues encountered.

### Completion Notes List

- Created expanded `ServerAdapter` interface in core with 5 methods, all with full Javadoc documenting contracts, parameters, and exceptions.
- Created immutable `ServerConfigParams` with builder pattern — holds outputDir, mojarraRefreshPeriod, myfacesRefreshPeriod, port. All fields final, no setters.
- Wrote 4 unit tests verifying builder construction, default values, custom refresh periods, and custom port.
- Old gradle-plugin `ServerAdapter` left in place (3 methods) — will be replaced in Story 1.4.
- Core module compiles independently with zero Gradle/Maven/Servlet imports.

### File List

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerAdapter.java`
- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/ServerConfigParams.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/server/ServerConfigParamsTest.java`

## Code Review (AI)

- **Reviewer:** Claude Opus 4.6 (1M context)
- **Date:** 2026-03-21
- **Result:** Pass — 1 issue found and fixed
- **Fixed H4:** `ServerConfigParams.builder().build()` now validates outputDir is non-null, throwing `JsfAutoreloadConfigException`. Added test for validation.

## Change Log

- 2026-03-21: Code review complete — fixed H4 (outputDir null validation in ServerConfigParams builder), status changed to done.
- 2026-03-16: Story implementation complete — created core ServerAdapter interface (5 methods with Javadoc) and immutable ServerConfigParams builder.
