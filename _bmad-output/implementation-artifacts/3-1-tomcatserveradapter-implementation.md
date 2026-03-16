# Story 3.1: TomcatServerAdapter Implementation

Status: ready-for-dev

## Story

As a developer using Tomcat,
I want the plugin to detect my Tomcat server, resolve the output directory, and configure JSF template reload,
So that I get the same live-reload experience as Liberty users.

## Acceptance Criteria

1. `TomcatServerAdapter` in `it.bstz.jsfautoreload.server.tomcat` implements the full `ServerAdapter` interface (5 methods)
2. `resolveOutputDir(serverName, projectDir)` returns the correct Tomcat exploded WAR path
3. `writeServerConfig(params)` writes JSF refresh configuration for Mojarra (`FACELETS_REFRESH_PERIOD=0`) and MyFaces (`REFRESH_PERIOD=0`) using Tomcat's configuration model
4. `writeServerConfig(params)` is idempotent — no duplicate entries on repeated calls
5. `isRunning()` returns `true` when Tomcat is running
6. `getHttpPort()` and `getContextRoot()` return correct values for the running Tomcat instance
7. Implementation documents which files it creates/modifies and what content it writes

## Tasks / Subtasks

- [ ] Task 1: Create `TomcatServerAdapter` class (AC: #1)
  - [ ] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/tomcat/TomcatServerAdapter.java`
  - [ ] 1.2 Implement all 5 `ServerAdapter` methods
  - [ ] 1.3 Constructor takes port and contextRoot parameters (same pattern as Liberty)
  - [ ] 1.4 Add class-level Javadoc documenting file side-effects (AC: #7)
- [ ] Task 2: Implement `resolveOutputDir()` (AC: #2)
  - [ ] 2.1 Tomcat exploded WAR convention: research Tomcat's standard deployment model
  - [ ] 2.2 Typical path: `{projectDir}/build/libs/exploded/{appName}.war` or Gradle war plugin output
  - [ ] 2.3 For Gradle war plugin: `{projectDir}/build/libs/{projectName}` or configured output
  - [ ] 2.4 Return appropriate `Path` based on Tomcat's deployment model
- [ ] Task 3: Implement `writeServerConfig()` (AC: #3, #4)
  - [ ] 3.1 For Tomcat, JSF refresh config goes in `web.xml` context-params or as system properties
  - [ ] 3.2 Write/update context params: `javax.faces.FACELETS_REFRESH_PERIOD=0`, `org.apache.myfaces.REFRESH_PERIOD=0`
  - [ ] 3.3 Check for existing entries before writing (idempotent)
  - [ ] 3.4 Use `java.nio.file` for all I/O operations
- [ ] Task 4: Implement `isRunning()`, `getHttpPort()`, `getContextRoot()` (AC: #5, #6)
  - [ ] 4.1 `isRunning()`: HTTP health check to localhost:{port} (same pattern as Liberty)
  - [ ] 4.2 `getHttpPort()`: return configured HTTP port
  - [ ] 4.3 `getContextRoot()`: return configured context root
- [ ] Task 5: Write comprehensive tests (AC: #1-#7)
  - [ ] 5.1 Create `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/server/tomcat/TomcatServerAdapterTest.java`
  - [ ] 5.2 Test `resolveOutputDir()` returns correct path
  - [ ] 5.3 Test `writeServerConfig()` writes correct configuration
  - [ ] 5.4 Test `writeServerConfig()` idempotency
  - [ ] 5.5 Test `isRunning()` with MockWebServer (true for 200, false for no connection)
  - [ ] 5.6 Test `getHttpPort()` and `getContextRoot()` return configured values
  - [ ] 5.7 Use `@TempDir` for file system tests, MockWebServer for HTTP tests

## Dev Notes

### Architecture Decision: Tomcat Validates the Platform

This is the "prove the platform" story. TomcatServerAdapter validates that the `ServerAdapter` abstraction works for a second server. The `DevServer` orchestrator must work identically with both Liberty and Tomcat — no server-specific code paths in core.

### Tomcat Deployment Model

Tomcat's exploded WAR deployment differs from Liberty:
- Tomcat typically uses `{CATALINA_HOME}/webapps/{appName}/` for exploded deployments
- With Gradle: the exploded WAR may be at the Gradle war plugin output location
- The `resolveOutputDir()` method needs to handle Tomcat's specific conventions
- Research Tomcat's `conf/server.xml` for context configuration if needed

### JSF Configuration for Tomcat

Tomcat uses `web.xml` context-params for JSF configuration:
```xml
<context-param>
    <param-name>javax.faces.FACELETS_REFRESH_PERIOD</param-name>
    <param-value>0</param-value>
</context-param>
<context-param>
    <param-name>org.apache.myfaces.REFRESH_PERIOD</param-name>
    <param-value>0</param-value>
</context-param>
```

OR system properties (simpler, no XML parsing needed):
- Consider which approach is more robust and less invasive

### File Side-Effect Documentation (Required)

Per architecture: each `ServerAdapter` implementation MUST document which files it creates/modifies. Example:
```
/**
 * TomcatServerAdapter - Tomcat application server support.
 *
 * Files created/modified by writeServerConfig():
 * - {outputDir}/WEB-INF/web.xml: Adds context-param entries for
 *   javax.faces.FACELETS_REFRESH_PERIOD and org.apache.myfaces.REFRESH_PERIOD
 */
```

### Code Conventions

- Package: `it.bstz.jsfautoreload.server.tomcat`
- Logging: JUL in core module
- Exceptions: throw `JsfAutoreloadException` or `JsfAutoreloadConfigException`
- No `var`, no wildcard imports

### Project Structure Notes

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/tomcat/TomcatServerAdapter.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/server/tomcat/TomcatServerAdapterTest.java`
- Depends on: Story 1.3 (ServerAdapter interface), Story 1.2 (exceptions)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#ServerAdapter Interface]
- [Source: _bmad-output/planning-artifacts/architecture.md#Implementation Sequence — step 7]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.1]
- [Source: jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/server/liberty/LibertyServerAdapter.java — reference implementation]
- [Source: _bmad-output/planning-artifacts/prd.md#FR34 — reference adapter implementations]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
