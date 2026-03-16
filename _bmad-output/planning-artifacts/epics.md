---
stepsCompleted: ['step-01-validate-prerequisites', 'step-02-design-epics', 'step-03-create-stories', 'step-04-final-validation']
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

# jsf-autoreload - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for jsf-autoreload, decomposing the requirements from the PRD and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

**Plugin Installation & Configuration:**
- FR1: Developer can apply the plugin to an existing Gradle build with a single plugin declaration
- FR2: Developer can apply the plugin to an existing Maven build with a single plugin declaration
- FR3: Developer can configure the WebSocket port via build tool DSL/configuration
- FR4: Developer can configure the application server name via build tool DSL/configuration
- FR5: Developer can configure the exploded WAR output directory via build tool DSL/configuration
- FR6: Developer can configure which directories to watch for file changes via build tool DSL/configuration
- FR7: Plugin can infer default output directory based on server name when not explicitly configured

**Build & Server Lifecycle:**
- FR8: Plugin can wire build task/phase dependencies so that preparation runs before server start, and the dev loop runs after server start
- FR9: Plugin can inject the runtime JAR into the exploded WAR's WEB-INF/lib before the application server starts
- FR10: Plugin can detect whether the application server is running
- FR11: Plugin can retrieve the HTTP port and context root from a running server

**Dev Loop Lifecycle:**
- FR12: Plugin can start and run a long-lived dev server (WebSocket server + file watcher) that blocks until terminated
- FR13: Plugin can shut down the file watcher, WebSocket server, and dev loop gracefully on termination (e.g., Ctrl+C)

**File Watching & Synchronization:**
- FR14: Plugin can detect file creation events in watched directories
- FR15: Plugin can detect file modification events in watched directories
- FR16: Plugin can detect file deletion events in watched directories
- FR17: Plugin can copy a changed file to the exploded WAR output directory preserving relative path structure
- FR18: Plugin can skip file copy on deletion events while still triggering a browser refresh
- FR19: Plugin can watch all files in configured watch directories (.xhtml, CSS, JS, images, fonts, and any other files present)

**Browser Refresh:**
- FR20: Plugin can run a WebSocket server that browser clients connect to
- FR21: Plugin can broadcast a reload message to all connected browsers when a file change is detected
- FR22: Runtime filter can buffer HTML responses and append a reload script before writing to the client
- FR23: Runtime filter can adjust response headers (Content-Length) to account for injected content
- FR24: Runtime filter can leave non-HTML responses (JSON, XML, etc.) unmodified
- FR25: Runtime filter can auto-register in Servlet 3.0+ containers without user configuration

**JSF Template Reload:**
- FR26: Plugin can configure the application server to force JSF template re-evaluation on every request (Mojarra FACELETS_REFRESH_PERIOD=0)
- FR27: Plugin can configure the application server to force JSF template re-evaluation on every request (MyFaces REFRESH_PERIOD=0)
- FR28: Plugin can detect existing configuration entries and skip writing duplicates on repeated runs

**Error Handling & Developer Feedback:**
- FR29: Plugin can fail fast with an actionable error message when the configured WebSocket port is already in use
- FR30: Plugin can fail fast with an actionable error message when the inferred output directory does not exist
- FR31: Plugin can warn when Liberty classloader delegation is set to parentFirst
- FR32: Plugin can display a startup message confirming the WebSocket server address and watched directories

**Server Extensibility:**
- FR33: Contributor can implement a new server adapter by implementing a public interface with three methods
- FR34: Contributor can reference existing adapter implementations as documentation for building new ones

**Distribution & Documentation:**
- FR35: Maintainer can publish the plugin to Gradle Plugin Portal via automated CI pipeline
- FR36: Maintainer can publish the plugin to Maven Central via automated CI pipeline
- FR37: Developer can view a compatibility matrix showing supported servers, build tools, and JSF implementations
- FR38: Developer can clone and run example projects for supported server/build-tool combinations
- FR39: Contributor can follow a documented guide to add a new ServerAdapter implementation

### NonFunctional Requirements

**Performance:**
- NFR1: End-to-end feedback loop (file save → browser refresh) completes in under 2 seconds for a single file change event (create, modify, or delete) on a file under 1MB
- NFR2: File system event detection and file copy completes in under 500ms as measured by unit test wall-clock timing from event callback to copy completion
- NFR3: WebSocket broadcast delivery to connected browsers completes in under 100ms as measured by unit test round-trip timing from broadcast call to client message receipt
- NFR4: Plugin startup (WebSocket server + file watcher initialization) completes in under 3 seconds
- NFR5: Runtime filter response buffering adds no more than 50ms latency to HTML responses
- NFR6: Plugin coalesces rapid successive file change events within a debounce window (default 300ms) into a single browser reload to prevent reload storms

**Compatibility:**
- NFR7: Plugin runs on Java 11 and all subsequent LTS versions; CI matrix tests against Java 11, 17, and 21
- NFR8: Gradle plugin works with Gradle 7.x and 8.x
- NFR9: Maven plugin works with Maven 3.6+
- NFR10: Runtime filter works in any Servlet 3.0+ container
- NFR11: Runtime filter operates correctly with both Mojarra and MyFaces JSF implementations
- NFR12: Plugin does not conflict with common Gradle/Maven plugins (war, java, liberty-gradle-plugin, spring-boot-gradle-plugin, maven-war-plugin, maven-compiler-plugin); verified by integration tests
- NFR13: Runtime JAR has zero transitive dependencies to avoid classpath conflicts in user WARs
- NFR14: Plugin operates correctly on macOS, Linux, and Windows

**Reliability:**
- NFR15: Dev loop runs stably for extended development sessions (8+ hours) without memory leaks — heap usage remains stable under repeated file change events
- NFR16: Graceful shutdown releases all resources (file watchers, WebSocket connections, threads) within 2 seconds
- NFR17: File watcher recovers from transient file system errors (e.g., locked files) without crashing the dev loop
- NFR18: WebSocket server handles client disconnection/reconnection without intervention

**Documentation Quality:**
- NFR19: Developer can determine whether their stack is supported within 30 seconds of reading the README
- NFR20: Developer can go from zero to working live-reload by following the README in under 5 minutes
- NFR21: Example projects run successfully on a fresh clone with no modifications beyond prerequisite JDK and server installation

### Additional Requirements

**From Architecture — Scaffolding & Module Structure:**
- Evolve existing two-module build into four-module build using create-then-extract sequence
- Rename `jsf-autoreload-plugin` → `jsf-autoreload-gradle-plugin` for symmetric naming
- Module dependency direction rule: runtime (standalone), core (standalone), gradle-plugin → core, maven-plugin → core; no cross-dependencies
- Core module must never import Gradle API, Maven API, or Servlet API
- CI must compile-verify each module independently to catch leaked API imports

**From Architecture — Core API & Design Decisions:**
- Core module API: Hybrid — `DevServer` orchestrator entry point with individually exposed components (`FileChangeWatcher`, `DevWebSocketServer`, `ServerAdapter`)
- `DevServerConfig` immutable POJO with builder pattern; validates required fields at build time
- Core never constructs `ServerAdapter` instances — plugin modules construct and pass in
- Event pipeline: Direct callbacks via `Consumer<Path>` with 300ms debounce coalescing via `ScheduledExecutorService`
- `DevServer` accepts injectable `ScheduledExecutorService` for testability

**From Architecture — Port Coordination:**
- Properties file (`jsf-autoreload.properties`) written to `WEB-INF/classes/` during prepare step
- Port resolution fallback chain: properties file → JVM system property → default 35729
- Eliminates Liberty-specific `jvm.options` mechanism in favor of server-agnostic approach

**From Architecture — ServerAdapter Evolution:**
- Expanded interface: `isRunning()`, `getHttpPort()`, `getContextRoot()`, `resolveOutputDir()`, `writeServerConfig()`
- Each implementation must document which files it creates/modifies for testability
- `ServerConfigParams` POJO for passing config to `writeServerConfig()`

**From Architecture — Maven Plugin:**
- Single user-facing `jsf-autoreload:dev` goal with internal `PrepareStep` class
- Use `de.benediktritter.maven-plugin-development` Gradle plugin for Maven descriptor generation
- Testing via `maven-invoker-plugin` (not `maven-plugin-testing-harness`)

**From Architecture — Shadow Relocation:**
- Each plugin module (`gradle-plugin`, `maven-plugin`) shadows its own uber-JAR with `core` inlined
- Core publishes un-relocated JAR for direct use and testing

**From Architecture — Implementation Patterns:**
- Custom exceptions: `JsfAutoreloadException` and `JsfAutoreloadConfigException` (core); translated to `GradleException`/`MojoExecutionException` in plugins
- Error message format: `"[JSF Autoreload] {what went wrong}. {how to fix it}."`
- Logging: JUL in core/runtime, build-tool native in plugins; `[JSF Autoreload]` prefix
- All long-lived resources implement `Closeable`; daemon threads only; reverse shutdown order
- No `var` keyword, no wildcard imports, explicit types everywhere

**From Architecture — Implementation Sequence:**
1. Create empty `core` and `maven-plugin` shells; rename `plugin` → `gradle-plugin`
2. Extract shared classes from `gradle-plugin` to `core`
3. Expand `ServerAdapter` interface with config methods
4. `DevServerConfig` builder with injectable executor
5. `DevServer` orchestrator with callback pipeline
6. Port coordination via properties file
7. `TomcatServerAdapter` implementation
8. Maven mojo with internal prepare logic

### FR Coverage Map

| FR | Epic | Description |
|---|---|---|
| FR1 | Epic 2 | Gradle plugin declaration |
| FR2 | Epic 4 | Maven plugin declaration |
| FR3 | Epic 1 | WebSocket port configuration (DevServerConfig) |
| FR4 | Epic 1 | Server name configuration (DevServerConfig) |
| FR5 | Epic 1 | Output directory configuration (DevServerConfig) |
| FR6 | Epic 1 | Watch directories configuration (DevServerConfig) |
| FR7 | Epic 1 | Output directory inference (ServerAdapter.resolveOutputDir) |
| FR8 | Epic 2 | Build task/phase dependency wiring (Gradle; Maven in Epic 4) |
| FR9 | Epic 2 | Runtime JAR injection (Gradle; Maven in Epic 4) |
| FR10 | Epic 1 | Server running detection (ServerAdapter.isRunning) |
| FR11 | Epic 1 | HTTP port/context root retrieval (ServerAdapter) |
| FR12 | Epic 1 | Long-lived dev server (DevServer.start) |
| FR13 | Epic 1 | Graceful shutdown (Closeable + reverse shutdown) |
| FR14 | Epic 1 | File creation event detection |
| FR15 | Epic 1 | File modification event detection |
| FR16 | Epic 1 | File deletion event detection |
| FR17 | Epic 1 | File copy to exploded WAR preserving relative path |
| FR18 | Epic 1 | Skip copy on delete, still trigger refresh |
| FR19 | Epic 1 | Watch all file types in configured dirs |
| FR20 | Epic 1 | WebSocket server for browser connections |
| FR21 | Epic 1 | Broadcast reload on file change |
| FR22 | Epic 2 | Runtime filter buffers HTML + injects script |
| FR23 | Epic 2 | Runtime filter adjusts Content-Length |
| FR24 | Epic 2 | Runtime filter leaves non-HTML unmodified |
| FR25 | Epic 2 | Runtime filter auto-registers via web-fragment.xml |
| FR26 | Epic 1 | Mojarra FACELETS_REFRESH_PERIOD=0 config |
| FR27 | Epic 1 | MyFaces REFRESH_PERIOD=0 config |
| FR28 | Epic 1 | Config deduplication on repeated runs |
| FR29 | Epic 1 | Fail-fast on port conflict |
| FR30 | Epic 1 | Fail-fast on missing output directory |
| FR31 | Epic 1 | Warn on parentFirst classloader |
| FR32 | Epic 1 | Startup confirmation message |
| FR33 | Epic 1 | ServerAdapter public interface (3→5 methods) |
| FR34 | Epic 3 | Reference adapter implementations (Tomcat) |
| FR35 | Epic 5 | Publish to Gradle Plugin Portal via CI |
| FR36 | Epic 5 | Publish to Maven Central via CI |
| FR37 | Epic 5 | Compatibility matrix in README |
| FR38 | Epic 5 | Clonable example projects |
| FR39 | Epic 5 | Documented ServerAdapter contribution guide |

## Epic List

### Epic 1: Core Module Extraction & Dev Loop Engine
Restructure the project from two modules into four, extract shared logic into `jsf-autoreload-core`, and implement the complete dev loop engine: DevServer orchestrator, DevServerConfig builder, expanded ServerAdapter interface, FileChangeWatcher, DevWebSocketServer, JSF template reload config, and error handling. After this epic, the core module provides a build-tool-agnostic engine ready for any server and plugin combination.
**FRs covered:** FR3, FR4, FR5, FR6, FR7, FR10, FR11, FR12, FR13, FR14, FR15, FR16, FR17, FR18, FR19, FR20, FR21, FR26, FR27, FR28, FR29, FR30, FR31, FR32, FR33

### Epic 2: Gradle + Liberty End-to-End Live-Reload
Wire the renamed `jsf-autoreload-gradle-plugin` to the core engine, update the runtime filter with server-agnostic port coordination (properties file), and deliver the complete Gradle+Liberty live-reload experience. After this epic, a Gradle developer using Liberty can install the plugin, run `gradle jsfDev`, and get automatic browser refresh on file save.
**FRs covered:** FR1, FR8, FR9, FR22, FR23, FR24, FR25

### Epic 3: Tomcat Server Support
Implement TomcatServerAdapter, validating the server-agnostic architecture. After this epic, developers using Tomcat (with Gradle) get the same live-reload experience — proving the platform story.
**FRs covered:** FR34
**Depends on:** Epic 1

### Epic 4: Maven Build Tool Support
Deliver the `jsf-autoreload-maven-plugin` with a single `jsf-autoreload:dev` goal wrapping core logic. After this epic, Maven developers get live-reload with any supported server (Liberty or Tomcat).
**FRs covered:** FR2
**Depends on:** Epic 1, Epic 3 (Tomcat validates ServerAdapter interface before Maven plugin work begins)

### Epic 5: Documentation, Distribution & Community Readiness
Publish to Gradle Plugin Portal and Maven Central via CI, create README with GIF demo and compatibility matrix, example projects (`liberty-gradle/`, `tomcat-maven/`), and CONTRIBUTING.md with "How to add a ServerAdapter" guide. After this epic, the tool is discoverable, well-documented, and ready for community adoption and extension.
**FRs covered:** FR35, FR36, FR37, FR38, FR39
**Depends on:** Epics 2, 3, 4

## Epic 1: Core Module Extraction & Dev Loop Engine

Restructure the project from two modules into four, extract shared logic into `jsf-autoreload-core`, and implement the complete dev loop engine. After this epic, the core module provides a build-tool-agnostic engine ready for any server and plugin combination.

### Story 1.1: Four-Module Project Scaffolding

As a maintainer,
I want the project structured into four modules (core, gradle-plugin, maven-plugin, runtime),
So that shared logic can be extracted into core without build-tool-specific dependencies.

**Acceptance Criteria:**

**Given** the existing two-module project (plugin + runtime)
**When** the scaffolding is applied
**Then** `settings.gradle.kts` includes four modules: `jsf-autoreload-core`, `jsf-autoreload-gradle-plugin`, `jsf-autoreload-maven-plugin`, `jsf-autoreload-runtime`
**And** the existing `jsf-autoreload-plugin` directory is renamed to `jsf-autoreload-gradle-plugin`

**Given** the new `jsf-autoreload-core` module
**When** I inspect its `build.gradle.kts`
**Then** it applies `java-library` and `maven-publish` plugins
**And** it has zero build-tool (Gradle API, Maven API) or Servlet API dependencies

**Given** the new `jsf-autoreload-maven-plugin` module
**When** I inspect its `build.gradle.kts`
**Then** it depends on `jsf-autoreload-core`
**And** it applies the `de.benediktritter.maven-plugin-development` plugin for descriptor generation

**Given** `jsf-autoreload-gradle-plugin`
**When** I inspect its `build.gradle.kts`
**Then** it depends on `jsf-autoreload-core`

**Given** the full project
**When** I run `./gradlew build`
**Then** all four modules compile successfully and all existing tests pass

### Story 1.2: Core Exception Types

As a developer,
I want clear, typed exceptions for configuration and runtime errors,
So that error messages are actionable and tell me exactly what to fix.

**Acceptance Criteria:**

**Given** the core module
**When** a general runtime error occurs
**Then** `JsfAutoreloadException` (extends `RuntimeException`) is thrown with a message following the format `"[JSF Autoreload] {what went wrong}. {how to fix it}."`

**Given** the core module
**When** a configuration or validation error occurs
**Then** `JsfAutoreloadConfigException` (extends `JsfAutoreloadException`) is thrown with an actionable message

**Given** an exception with a root cause
**When** constructed
**Then** the cause is preserved and accessible via `getCause()`

**Given** the core module
**When** I inspect exception class imports
**Then** no Gradle, Maven, or Servlet API imports are present

### Story 1.3: ServerAdapter Interface & ServerConfigParams

As a contributor,
I want a public ServerAdapter interface with clear method contracts,
So that I can implement support for a new application server.

**Acceptance Criteria:**

**Given** `ServerAdapter` in the core module
**When** I inspect the interface
**Then** it declares 5 methods: `isRunning()`, `getHttpPort()`, `getContextRoot()`, `resolveOutputDir(String serverName, Path projectDir)`, `writeServerConfig(ServerConfigParams params)`

**Given** `ServerConfigParams`
**When** I create an instance
**Then** it holds all configuration values needed by `writeServerConfig()` including JSF refresh period settings

**Given** the `ServerAdapter` interface
**When** I inspect the Javadoc
**Then** each method's contract is documented including expected behavior, parameters, and exceptions

**Given** the core module
**When** compiled independently
**Then** no Gradle API, Maven API, or Servlet API imports are present in any class

### Story 1.4: LibertyServerAdapter Migration to Core

As a developer using Liberty,
I want the Liberty server adapter working through the core module,
So that my existing live-reload setup continues to work on the new architecture.

**Acceptance Criteria:**

**Given** `LibertyServerAdapter` in `it.bstz.jsfautoreload.server.liberty`
**When** `resolveOutputDir("defaultServer", projectDir)` is called
**Then** it returns the correct Liberty exploded WAR path based on the server name and project directory

**Given** `LibertyServerAdapter`
**When** `writeServerConfig(params)` is called
**Then** it writes `bootstrap.properties` with `javax.faces.FACELETS_REFRESH_PERIOD=0` and `org.apache.myfaces.REFRESH_PERIOD=0`

**Given** existing configuration entries in `bootstrap.properties`
**When** `writeServerConfig(params)` is called again
**Then** duplicate entries are not written (idempotent)

**Given** Liberty with `parentFirst` classloader delegation
**When** the adapter detects this configuration
**Then** a warning is logged: `"[JSF Autoreload] Liberty classloader delegation is set to parentFirst. This may prevent the runtime filter from loading correctly."`

**Given** all existing LibertyServerAdapter tests
**When** run from the core module
**Then** all pass without modification

### Story 1.5: DevServerConfig Immutable Builder

As a developer,
I want to configure the dev loop with port, output directory, watch directories, and server settings,
So that the live-reload works with my specific project setup.

**Acceptance Criteria:**

**Given** `DevServerConfig.builder()`
**When** all required fields (outputDir, watchDirs, serverAdapter) are set and `build()` is called
**Then** an immutable `DevServerConfig` instance is returned

**Given** `DevServerConfig.builder()`
**When** a required field (outputDir, watchDirs, or serverAdapter) is missing and `build()` is called
**Then** `JsfAutoreloadConfigException` is thrown with an actionable message identifying the missing field

**Given** `DevServerConfig.builder()`
**When** port is not explicitly set
**Then** the default value `35729` is used

**Given** `DevServerConfig.builder()`
**When** debounceMs is not explicitly set
**Then** the default value `300` is used

**Given** `DevServerConfig.builder()`
**When** a `ScheduledExecutorService` is provided via `.executor()`
**Then** it is used instead of creating a default (enables test injection)

**Given** an immutable `DevServerConfig`
**When** any getter is called
**Then** the configured value is returned and the config object cannot be modified

### Story 1.6: FileChangeWatcher Extraction to Core

As a developer,
I want file changes in my source directories detected automatically,
So that modified .xhtml, CSS, JS, and static files are immediately noticed.

**Acceptance Criteria:**

**Given** configured watch directories
**When** a file is created in a watched directory
**Then** the `onChange` callback (`Consumer<Path>`) is invoked with the created file's path

**Given** configured watch directories
**When** a file is modified in a watched directory
**Then** the `onChange` callback is invoked with the modified file's path

**Given** configured watch directories
**When** a file is deleted in a watched directory
**Then** the `onDelete` callback (`Consumer<Path>`) is invoked with the deleted file's path

**Given** any file type (.xhtml, .css, .js, .png, .woff, etc.) in a watched directory
**When** the file changes
**Then** the appropriate callback is invoked regardless of file extension

**Given** `FileChangeWatcher` implementing `Closeable`
**When** `close()` is called
**Then** all watching threads are stopped and resources released

**Given** a transient file system error (e.g., locked file)
**When** the watcher encounters the error
**Then** watching continues without crashing the watcher

**Given** `FileChangeWatcher` in the core module
**When** I inspect its imports
**Then** no Gradle API imports are present

### Story 1.7: DevWebSocketServer Extraction to Core

As a developer,
I want a WebSocket server that browsers can connect to for reload notifications,
So that my browser refreshes automatically when files change.

**Acceptance Criteria:**

**Given** `DevWebSocketServer` started on a configured port
**When** a browser client connects via WebSocket
**Then** the connection is established successfully

**Given** one or more connected browser clients
**When** `broadcast("reload")` is called
**Then** all connected clients receive the "reload" message within 100ms

**Given** a browser client that disconnects
**When** it reconnects
**Then** the new connection is accepted without error or manual intervention

**Given** `DevWebSocketServer` attempting to start
**When** the configured port is already in use
**Then** `JsfAutoreloadException` is thrown with message: `"[JSF Autoreload] Port {port} is already in use. Configure a different port via jsfAutoreload { port = XXXX }."`

**Given** `DevWebSocketServer` implementing `Closeable`
**When** `close()` is called
**Then** all connections are closed and the port is released

**Given** `DevWebSocketServer` startup
**When** initialization completes
**Then** it completes in under 3 seconds

### Story 1.8: DevServer Orchestrator

As a developer,
I want a single dev server that watches files, copies changes to the exploded WAR, and triggers browser reloads,
So that I get a seamless sub-2-second feedback loop from file save to browser refresh.

**Acceptance Criteria:**

**Given** `DevServer` started with a valid `DevServerConfig`
**When** a file is modified in a watch directory
**Then** the file is copied to the output directory preserving its relative path structure
**And** a reload message is broadcast to all connected browsers

**Given** `DevServer` running
**When** a file is deleted in a watch directory
**Then** the file is NOT copied to the output directory
**And** a reload message IS broadcast to all connected browsers

**Given** rapid successive file changes (e.g., IDE "save all") within the 300ms debounce window
**When** the debounce window expires
**Then** a single browser reload is triggered (not one per change)

**Given** `DevServer` startup completing successfully
**When** the dev loop begins
**Then** a confirmation message is logged: `"[JSF Autoreload] WebSocket server listening on ws://localhost:{port}. Watching: {dirs}"`

**Given** `DevServer` started with a `DevServerConfig` whose output directory does not exist
**When** `start()` is called
**Then** `JsfAutoreloadConfigException` is thrown with message: `"[JSF Autoreload] Output directory not found: {path}. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your server name."`

**Given** `DevServer` running
**When** `close()` is called (or Ctrl+C)
**Then** shutdown completes within 2 seconds in reverse order: dev loop → WebSocket server → file watcher

**Given** `DevServer` implementing `Closeable`
**When** used in a test with `try-with-resources`
**Then** all resources are released automatically

## Epic 2: Gradle + Liberty End-to-End Live-Reload

Wire the renamed `jsf-autoreload-gradle-plugin` to the core engine, update the runtime filter with server-agnostic port coordination, and deliver the complete Gradle+Liberty live-reload experience.

### Story 2.1: Gradle Plugin Wiring to Core

As a developer using Gradle,
I want to apply the jsf-autoreload plugin with a single declaration,
So that I get live-reload without complex build configuration.

**Acceptance Criteria:**

**Given** a Gradle build file with `plugins { id 'it.bstz.jsf-autoreload' }`
**When** the plugin is applied
**Then** it registers the `jsfAutoreload` extension block and `jsfDev`, `jsfPrepare` tasks

**Given** `JsfAutoreloadExtension` DSL values (port, serverName, outputDir, watchDirs)
**When** `jsfDev` task executes
**Then** the values are mapped to a `DevServerConfig` and passed to `DevServer.start()`

**Given** `jsfPrepare` task
**When** it executes before server start
**Then** it injects the runtime JAR into the exploded WAR's `WEB-INF/lib`
**And** it calls `ServerAdapter.writeServerConfig()` for JSF template reload configuration

**Given** `jsfDev` task
**When** task dependencies are resolved
**Then** `jsfPrepare` runs before server start, and the dev loop runs after server start

**Given** the Gradle plugin
**When** a `JsfAutoreloadException` is thrown by core
**Then** it is caught and translated to a `GradleException` with the same message

**Given** the Gradle plugin shadow JAR
**When** built
**Then** it includes `jsf-autoreload-core` with `org.java_websocket` relocated to avoid classpath conflicts

### Story 2.2: Port Coordination via Properties File

As a developer,
I want the plugin and runtime filter to agree on the WebSocket port without server-specific configuration,
So that port coordination works on any Servlet 3.0+ container.

**Acceptance Criteria:**

**Given** the `jsfPrepare` task executing
**When** preparation completes
**Then** a `jsf-autoreload.properties` file is written to the exploded WAR's `WEB-INF/classes/` containing `port={configured_port}`

**Given** an existing `jsf-autoreload.properties` file
**When** `jsfPrepare` runs again
**Then** the file is overwritten with the current port value (idempotent)

**Given** the properties file written by the prepare step
**When** the runtime filter initializes in the Servlet container
**Then** it reads the port from the classpath properties file

### Story 2.3: Runtime Filter Update for Properties File Port Resolution

As a developer,
I want the runtime filter to read the WebSocket port from a classpath properties file,
So that browser refresh works without JVM system properties or server-specific config.

**Acceptance Criteria:**

**Given** `jsf-autoreload.properties` on the classpath with `port=35729`
**When** `DevModeFilter` initializes
**Then** it reads port `35729` from the properties file

**Given** no properties file but JVM system property `jsf.autoreload.port=35730`
**When** `DevModeFilter` initializes
**Then** it falls back to the system property value `35730`

**Given** both properties file (`port=35729`) and system property (`jsf.autoreload.port=35730`)
**When** `DevModeFilter` initializes
**Then** the properties file value wins (`35729`)

**Given** neither properties file nor system property
**When** `DevModeFilter` initializes
**Then** it falls back to the default port `35729`

**Given** an HTML response passing through `DevModeFilter`
**When** the filter processes the response
**Then** it buffers the response body, appends the WebSocket reload script before `</body>`, and adjusts `Content-Length` to account for the injected content
**And** the latency added by buffering is under 50ms

**Given** a non-HTML response (JSON, XML, image, etc.)
**When** the filter processes the response
**Then** the response is passed through unmodified

**Given** a Servlet 3.0+ container
**When** the runtime JAR is present in `WEB-INF/lib`
**Then** `DevModeFilter` auto-registers via `web-fragment.xml` without any user configuration

### Story 2.4: Gradle + Liberty Integration Test

As a developer using Gradle + Liberty,
I want the full end-to-end live-reload pipeline verified,
So that I'm confident the plugin works correctly on my stack.

**Acceptance Criteria:**

**Given** a Gradle TestKit test project with the jsf-autoreload plugin applied
**When** `jsfPrepare` task runs
**Then** the runtime JAR is present in the exploded WAR's `WEB-INF/lib`
**And** `jsf-autoreload.properties` is present in `WEB-INF/classes`
**And** `bootstrap.properties` contains JSF refresh period entries

**Given** the plugin applied alongside `war` and `liberty-gradle-plugin`
**When** the build runs
**Then** no task conflicts or classpath errors occur

**Given** Gradle 7.x and 8.x
**When** the integration tests run
**Then** they pass on both major versions

## Epic 3: Tomcat Server Support

Implement TomcatServerAdapter, validating the server-agnostic architecture. After this epic, developers using Tomcat (with Gradle) get the same live-reload experience — proving the platform story.

### Story 3.1: TomcatServerAdapter Implementation

As a developer using Tomcat,
I want the plugin to detect my Tomcat server, resolve the output directory, and configure JSF template reload,
So that I get the same live-reload experience as Liberty users.

**Acceptance Criteria:**

**Given** `TomcatServerAdapter` in `it.bstz.jsfautoreload.server.tomcat`
**When** `resolveOutputDir(serverName, projectDir)` is called
**Then** it returns the correct Tomcat exploded WAR path based on Tomcat's deployment model

**Given** `TomcatServerAdapter`
**When** `writeServerConfig(params)` is called
**Then** it writes the appropriate configuration to enable Mojarra `FACELETS_REFRESH_PERIOD=0` and MyFaces `REFRESH_PERIOD=0` for Tomcat's configuration model

**Given** existing configuration entries
**When** `writeServerConfig(params)` is called again
**Then** duplicate entries are not written (idempotent)

**Given** `TomcatServerAdapter`
**When** `isRunning()` is called and the Tomcat server is running
**Then** it returns `true`

**Given** `TomcatServerAdapter`
**When** `getHttpPort()` and `getContextRoot()` are called
**Then** they return the correct HTTP port and context root for the running Tomcat instance

**Given** `TomcatServerAdapter` implementation
**When** I inspect its documentation
**Then** it documents which files it creates or modifies and what content it writes (per architecture requirement)

### Story 3.2: Tomcat Integration Verification

As a developer using Tomcat + Gradle,
I want the full dev loop verified against a Tomcat deployment model,
So that I'm confident the server-agnostic architecture works for my stack.

**Acceptance Criteria:**

**Given** a Gradle project configured with `jsfAutoreload { }` targeting Tomcat
**When** the prepare step runs
**Then** the runtime JAR is injected into Tomcat's exploded WAR `WEB-INF/lib`
**And** `jsf-autoreload.properties` is written to `WEB-INF/classes`
**And** Tomcat-specific JSF config is written

**Given** `DevServer` started with `TomcatServerAdapter`
**When** a file is modified in a watch directory
**Then** it is copied to the Tomcat output directory preserving relative path structure
**And** a browser reload is triggered

**Given** the `ServerAdapter` interface
**When** both `LibertyServerAdapter` and `TomcatServerAdapter` are used through `DevServer`
**Then** the `DevServer` orchestrator works identically with both — no server-specific code paths in core

## Epic 4: Maven Build Tool Support

Deliver the `jsf-autoreload-maven-plugin` with a single `jsf-autoreload:dev` goal wrapping core logic. After this epic, Maven developers get live-reload with any supported server (Liberty or Tomcat).

### Story 4.1: Maven Mojo & PrepareStep Implementation

As a developer using Maven,
I want to run `mvn jsf-autoreload:dev` to get live-reload,
So that I don't need to switch build tools to use jsf-autoreload.

**Acceptance Criteria:**

**Given** a Maven `pom.xml` with the `jsf-autoreload-maven-plugin` declared
**When** the developer runs `mvn jsf-autoreload:dev`
**Then** the `JsfDevMojo` executes, internally running the prepare step before entering the dev loop

**Given** `JsfDevMojo`
**When** configuration is provided via `<configuration>` block (port, serverName, outputDir, watchDirs)
**Then** the values are mapped to a `DevServerConfig` and passed to `DevServer.start()`

**Given** `PrepareStep` as a separate testable class
**When** executed internally by the mojo
**Then** it resolves the runtime JAR via Maven dependency resolution and copies it to the exploded WAR's `WEB-INF/lib`
**And** it writes `jsf-autoreload.properties` to `WEB-INF/classes`
**And** it calls `ServerAdapter.writeServerConfig()` for JSF template reload configuration

**Given** a `JsfAutoreloadException` thrown by core
**When** caught by the mojo
**Then** it is translated to a `MojoExecutionException` with the same message

**Given** `PrepareStep` in isolation
**When** unit tested with plain JUnit (no Maven infrastructure)
**Then** all tests pass, verifying prepare logic independently of Maven runtime

### Story 4.2: Maven Plugin Integration Test

As a developer using Maven,
I want the Maven plugin verified against real Maven builds,
So that I'm confident it works with standard Maven project layouts.

**Acceptance Criteria:**

**Given** a `maven-invoker-plugin` test project with `jsf-autoreload-maven-plugin` configured
**When** `mvn jsf-autoreload:dev` is invoked
**Then** the prepare step completes successfully: runtime JAR in `WEB-INF/lib`, properties file in `WEB-INF/classes`, server config written

**Given** the Maven plugin applied alongside `maven-war-plugin` and `maven-compiler-plugin`
**When** the build runs
**Then** no plugin conflicts or classpath errors occur

**Given** Maven 3.6+
**When** the integration tests run
**Then** they pass on supported Maven versions

**Given** the built `jsf-autoreload-maven-plugin` artifact
**When** inspected
**Then** it contains a valid `META-INF/maven/plugin.xml` descriptor generated by the `de.benediktritter.maven-plugin-development` Gradle plugin

## Epic 5: Documentation, Distribution & Community Readiness

Publish to Gradle Plugin Portal and Maven Central via CI, create README with GIF demo and compatibility matrix, example projects, and CONTRIBUTING.md with "How to add a ServerAdapter" guide. After this epic, the tool is discoverable, well-documented, and ready for community adoption and extension.

### Story 5.1: GitHub Actions CI/CD Pipeline

As a maintainer,
I want automated build, test, and publish pipelines,
So that releases to Gradle Plugin Portal and Maven Central are reliable and repeatable.

**Acceptance Criteria:**

**Given** a push to the main branch
**When** GitHub Actions CI runs
**Then** all four modules are built and tested across Java 11, 17, and 21

**Given** CI running
**When** each module is compiled
**Then** per-module compile verification catches any leaked API imports (e.g., Gradle API in core)

**Given** a tagged release (e.g., `v1.0.0`)
**When** the publish workflow runs
**Then** `jsf-autoreload-gradle-plugin` is published to Gradle Plugin Portal
**And** `jsf-autoreload-core`, `jsf-autoreload-maven-plugin`, and `jsf-autoreload-runtime` are published to Maven Central

**Given** a publish failure
**When** the workflow fails
**Then** the error is reported clearly and no partial release is left in an inconsistent state

### Story 5.2: README with Quick Start & Compatibility Matrix

As a developer evaluating jsf-autoreload,
I want a README with a GIF demo, 3-line quick start, and compatibility matrix,
So that I can determine if my stack is supported and get started in under 5 minutes.

**Acceptance Criteria:**

**Given** the README.md
**When** a developer reads it
**Then** they can determine whether their stack is supported (server, build tool, JSF implementation) within 30 seconds via a compatibility matrix table

**Given** the README.md
**When** a developer follows the Quick Start section
**Then** they can go from zero to working live-reload in under 5 minutes with a 3-line setup (Gradle) or equivalent Maven snippet

**Given** the README.md
**When** a developer views it on GitHub
**Then** it includes a GIF demo showing the live-reload experience (file save → browser refresh)

**Given** the README.md
**When** a developer looks for configuration options
**Then** a configuration reference section documents all DSL/configuration options (port, serverName, outputDir, watchDirs) with defaults

### Story 5.3: Example Projects

As a developer,
I want working example projects I can clone and run,
So that I can see live-reload in action on my stack before integrating into my own project.

**Acceptance Criteria:**

**Given** the `examples/liberty-gradle/` directory
**When** a developer clones the repo and follows the example README
**Then** they can run the Liberty + Gradle example with live-reload working out of the box
**And** the example README lists exact prerequisites (JDK version, Liberty installation)

**Given** the `examples/tomcat-maven/` directory
**When** a developer clones the repo and follows the example README
**Then** they can run the Tomcat + Maven example with live-reload working out of the box
**And** the example README lists exact prerequisites (JDK version, Tomcat installation)

**Given** each example project
**When** run on a fresh clone with prerequisites installed
**Then** it works with no modifications — a minimal JSF 2.x WAR with a single `.xhtml` page

### Story 5.4: CONTRIBUTING.md & ServerAdapter Guide

As a contributor,
I want a documented guide for adding a new ServerAdapter,
So that I can extend jsf-autoreload to support my application server.

**Acceptance Criteria:**

**Given** `CONTRIBUTING.md`
**When** a contributor reads it
**Then** it includes a dedicated "How to add a new ServerAdapter" section

**Given** the ServerAdapter guide
**When** a contributor follows it
**Then** it walks through the `ServerAdapter` interface (5 methods), explains each method's contract, and points to `LibertyServerAdapter` and `TomcatServerAdapter` as reference implementations

**Given** the ServerAdapter guide
**When** a contributor looks for practical guidance
**Then** it documents where server-specific exploded WAR output directories are typically located, how to wire the adapter into the build plugin DSL, and how to test without a real server installation

**Given** `CONTRIBUTING.md`
**When** a contributor wants to submit a PR
**Then** it documents the PR review process, coding conventions, and test expectations
