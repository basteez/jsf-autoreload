---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments: ['_bmad-output/planning-artifacts/prd.md', '_bmad-output/planning-artifacts/prd-validation-report.md', '_bmad-output/implementation-artifacts/tech-spec-jsf-autoreload-plugin.md']
workflowType: 'architecture'
lastStep: 8
status: 'complete'
completedAt: '2026-03-14'
project_name: 'jsf-autoreload'
user_name: 'Tiziano'
date: '2026-03-14'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**
39 FRs across 9 categories:
- Plugin Installation & Configuration (FR1-FR7) — DSL/config for both Gradle and Maven, with sensible defaults and auto-inference
- Build & Server Lifecycle (FR8-FR11) — Task/phase wiring, runtime JAR injection, server detection
- Dev Loop Lifecycle (FR12-FR13) — Long-lived dev server with graceful shutdown
- File Watching & Synchronization (FR14-FR19) — Create/modify/delete detection, file copy to exploded WAR, all file types
- Browser Refresh (FR20-FR25) — WebSocket server, broadcast, Servlet Filter response buffering, auto-registration
- JSF Template Reload (FR26-FR28) — Mojarra and MyFaces REFRESH_PERIOD configuration with deduplication
- Error Handling & Developer Feedback (FR29-FR32) — Fail-fast with actionable messages, startup confirmation
- Server Extensibility (FR33-FR34) — Public ServerAdapter API, reference implementations
- Distribution & Documentation (FR35-FR39) — CI publishing, compatibility matrix, examples, contributor guide

**Non-Functional Requirements:**
21 NFRs across 4 categories:
- Performance (NFR1-NFR6) — Sub-2s end-to-end, <500ms file ops, <100ms WebSocket, <3s startup, <50ms filter latency, 300ms debounce
- Compatibility (NFR7-NFR14) — Java 11/17/21, Gradle 7+/8+, Maven 3.6+, Servlet 3.0+, Mojarra/MyFaces, common plugin compatibility, zero deps, cross-platform
- Reliability (NFR15-NFR18) — 8+ hour stability, 2s graceful shutdown, transient error recovery, reconnection handling
- Documentation Quality (NFR19-NFR21) — 30s stack assessment, 5min setup, clone-and-run examples

### Primary Architectural Drivers

These two requirements are non-negotiable and every architectural decision must be stress-tested against them:

1. **Zero-config experience** (FR1, FR2, FR7) — Developer applies the plugin to an existing build and gets live-reload working without configuration friction. This drives default inference logic, convention-over-configuration patterns, and the "just apply and go" installation model.
2. **Sub-2-second feedback loop** (NFR1) — End-to-end from file save to browser refresh in under 2 seconds. This drives the event pipeline design: file watching latency, file copy speed, WebSocket broadcast timing, and debounce window tuning.

### Scale & Complexity

- Primary domain: Developer tooling (JVM build plugin + runtime library)
- Complexity level: **Low overall, Medium for cross-module coordination** — no database, no auth, no multi-tenancy, no regulatory requirements, but the build toolchain abstraction (extracting shared core, coordinating two independent artifacts across different build tool ecosystems) is genuinely tricky
- Estimated architectural components: 4 modules (`core`, `gradle-plugin`, `maven-plugin`, `runtime`) with ~15-18 primary classes

### Technical Constraints & Dependencies

- **Java 11+ minimum** — must compile and run on Java 11, 17, 21
- **javax.servlet namespace** — Java EE 8; Jakarta EE 9+ (`jakarta.servlet`) is out of scope for v1
- **Zero runtime dependencies** — runtime JAR must be dependency-free to avoid WAR classpath conflicts
- **WebSocket library relocation** — `org.java-websocket` must be shadow-relocated in the Gradle plugin JAR to avoid classpath pollution. This has testing implications: unit tests import pre-relocation names but integration tests run against relocated bytecode, requiring careful test classpath configuration to avoid IDE-passes-CI-fails scenarios
- **Build tool isolation** — Gradle and Maven plugins must share core logic without leaking build-tool-specific APIs into shared code
- **Cross-platform** — file watching must work on macOS (kqueue/FSEvents), Linux (inotify), Windows (ReadDirectoryChangesW)
- **Core extraction from existing codebase** — v0.1-beta exists as a two-module Gradle+Liberty build (tech spec); v1.0 requires four modules. Extracting `FileChangeWatcher`, `DevWebSocketServer`, and `ServerAdapter` into `jsf-autoreload-core` without breaking existing behavior is the highest-risk technical task. The PRD Risk Mitigation section explicitly flags tight coupling to Gradle APIs as a risk.
- **Module rename** — v0.1-beta's `jsf-autoreload-plugin` is renamed to `jsf-autoreload-gradle-plugin` in v1.0 for symmetry with `jsf-autoreload-maven-plugin`.

### Cross-Cutting Concerns Identified

- **Core module extraction (two-module → four-module)** — The single riskiest architectural concern. Shared logic currently lives in the Gradle plugin module and must be extracted into a build-tool-agnostic `core` module. Every class that moves must shed Gradle API dependencies. This affects module boundaries, dependency scoping, and the public API surface.
- **Two-artifact coordination** — The build-time plugin and the runtime JAR are independent artifacts that must agree on a WebSocket port. The runtime JAR works in any Servlet 3.0+ container without knowing which build tool injected it. This is a non-trivial coordination contract that must be solved at the architecture level.
- **Port coordination (open question)** — The current mechanism (JVM system property via Liberty's `jvm.options`) is Liberty-specific. Tomcat doesn't have `jvm.options`. The port communication mechanism must be server-agnostic — this is an open architectural question that needs a decision before the second server adapter can be implemented.
- **Graceful shutdown & resource cleanup** — Watcher, WebSocket server, and threads must all release within 2 seconds on Ctrl+C. Affects every component that holds resources.
- **Actionable error messages** — Every failure mode (port conflict, missing output dir, parentFirst classloader) must produce a message that tells the developer exactly what to fix. This is a UX pattern, not just error handling.
- **Config deduplication** — `bootstrap.properties`, `jvm.options`, and web.xml entries must be idempotent across repeated runs. Affects all config-writing logic.
- **Shadow/relocation** — WebSocket library relocation affects import paths, testing, and debugging. Must be consistently handled across build configurations, with explicit attention to test classpath divergence between IDE and CI environments.

## Starter Template Evaluation

### Primary Technology Domain

JVM developer tooling (Gradle/Maven build plugin + runtime library) — based on project requirements analysis. No conventional starter template applies; the project scaffolding is a custom multi-module Gradle build.

### Starter Options Considered

Conventional starter template generators (create-app CLIs, boilerplate generators) do not exist for this project type. The relevant "starter" decision is: what does the v1.0 four-module Gradle project structure look like, and what foundational build decisions carry forward from v0.1-beta?

Three approaches were evaluated:

1. **Evolve existing two-module build** — Add `core` and `maven-plugin` modules to the existing `settings.gradle.kts`. Extract shared code. Least disruption.
2. **Clean-room four-module scaffold** — Start fresh with a new project structure, copy code in. Clean boundaries but high risk of regressions.
3. **Gradle init with custom type** — Use `gradle init --type java-library` for new modules, integrate into existing build. Moderate approach.

**Selected approach: Evolve existing two-module build** — The v0.1-beta is working, tested, and reviewed. The risk of regressions from a clean-room rewrite is not justified. Adding two new modules to the existing build and extracting code is the lowest-risk path.

### Scaffolding Sequence

The v1.0 module expansion follows a **create-then-extract** sequence:

1. **First commit:** Add `jsf-autoreload-core` and `jsf-autoreload-maven-plugin` as empty module shells — `build.gradle.kts` files, package directories, and `settings.gradle.kts` updated to include all four modules. No code moves yet.
2. **Subsequent commits:** Extract shared classes from `plugin` into `core`, one class or group at a time, verifying all existing tests pass after each move.
3. **Final commits:** Wire Maven plugin mojos in `maven-plugin` module against the now-stable `core` API.

This ensures the four-module structure exists before any code extraction begins, reducing the risk of mid-extraction structural changes.

### Module Dependency Direction Rule

Hard architectural constraint — violating this causes build-tool API leakage and classpath conflicts:

```
runtime        → (standalone, zero dependencies)
core           → (standalone, zero build-tool dependencies)
gradle-plugin  → core
maven-plugin   → core
```

- `gradle-plugin` and `maven-plugin` MUST NOT depend on each other
- `core` MUST NOT depend on Gradle API, Maven API, or Servlet API
- `runtime` MUST NOT depend on any other module
- `core` MUST NOT depend on `runtime`

### Foundational Technical Decisions

**Language & Runtime:**
- Java 11 source/target (supports Java 11, 17, 21 at runtime)
- No Kotlin in production code — pure Java for maximum compatibility and contributor accessibility
- Gradle Kotlin DSL for build scripts only

**Build Tooling:**
- Gradle 7+/8+ as the build system for the project itself
- `maven-publish` plugin on all four modules for Maven Central publishing
- `java-gradle-plugin` on `gradle-plugin` module only
- `java-library` on `core` and `runtime` modules

**Shadow Relocation Strategy (open question to resolve in architectural decisions):**
In v0.1-beta, `DevWebSocketServer` lives in the `gradle-plugin` module and the Shadow plugin relocates `org.java_websocket` there. In v1.0, `DevWebSocketServer` moves to `core`. The recommended approach: **each plugin module (`gradle-plugin`, `maven-plugin`) shadows its own uber-JAR with `core` inlined**. This keeps relocation self-contained — the user never sees `core` as a separate classpath artifact. The `core` module itself publishes an un-relocated JAR for direct use and testing.

**Runtime JAR Delivery:**
The mechanism for passing the runtime JAR to the server preparation step is **build-tool-specific** and lives in each plugin module, not in core:
- **Gradle:** Custom `runtimeJar` configuration with `@InputFiles FileCollection` (existing v0.1-beta mechanism)
- **Maven:** Maven dependency resolution in the mojo (to be designed in architectural decisions)

**Testing Framework:**
- JUnit 5 across all modules
- Mockito for runtime module (Servlet API mocking)
- Gradle TestKit for Gradle plugin integration tests
- MockWebServer (OkHttp) for ServerAdapter HTTP mocking

**Code Organization:**
- Four modules: `core`, `gradle-plugin`, `maven-plugin`, `runtime`
- Package root: `it.bstz.jsfautoreload`
- Module-specific sub-packages: `.watcher`, `.websocket`, `.server`, `.filter`

**CI/CD:**
- GitHub Actions for build, test, publish
- CI matrix: Java 11, 17, 21
- Publish targets: Gradle Plugin Portal + Maven Central
- **CI must compile-verify each module independently** — root-level `./gradlew build` won't catch leaked Gradle API imports in `core`. CI should include per-module compile checks or a dedicated verification step.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
1. Core module public API design — Hybrid orchestrator + exposed components
2. Port coordination mechanism — Properties file in WEB-INF/classes
3. ServerAdapter interface evolution — Expanded single interface per server

**Important Decisions (Shape Architecture):**
4. Event pipeline architecture — Direct callbacks
5. Configuration model — Immutable config POJO with builder
6. Maven lifecycle integration — Single goal with internal prepare logic

**Deferred Decisions (Post-v1):**
- Jakarta EE 9+ support strategy (Phase 2)
- Attach mode architecture (Phase 2)

### Core Module API Design

**Decision:** Hybrid — Core orchestrator (`DevServer`) with individually exposed components.

**Rationale:** The `DevServer` class provides a high-level entry point that keeps plugin modules thin — they build a config object and call `start()`. Individual components (`FileChangeWatcher`, `DevWebSocketServer`, `ServerAdapter`) remain public for independent testing and advanced use cases. Community contributors writing new `ServerAdapter` implementations need access to the component APIs.

**Key constraint:** Core never constructs `ServerAdapter` instances. Plugin modules construct the adapter (they know the server type from build config) and pass the instance into `DevServerConfig`. Core only calls interface methods — it has no knowledge of which adapter implementation it's using.

**Affects:** `core` module public API surface, plugin module complexity, contributor experience (Journey 3)

### Port Coordination

**Decision:** Properties file in `WEB-INF/classes`.

**Rationale:** The prepare step writes a `jsf-autoreload.properties` file into the exploded WAR's `WEB-INF/classes/`. The `DevModeFilter` reads it from the classpath at filter init time. This is fully server-agnostic — no JVM system properties, no web.xml modification, works in any Servlet 3.0+ container.

**Port resolution fallback chain:**
1. Classpath properties file (`jsf-autoreload.properties` → key `port`)
2. JVM system property (`jsf.autoreload.port`) — backwards compatibility with v0.1-beta
3. Default: `35729`

**Required test cases:**
- Properties file present → uses file value
- Only system property set → uses system property
- Both present → properties file wins
- Neither present → falls back to 35729

**Affects:** `DevModeFilter` init logic (runtime), prepare logic in both plugin modules, eliminates the Liberty-specific `jvm.options` mechanism

### ServerAdapter Interface

**Decision:** Expanded single interface with config methods.

**Rationale:** `ServerAdapter` grows from 3 to ~6 methods, adding `resolveOutputDir()` and `writeServerConfig()`. One class per server is simpler for contributors than two separate interfaces. The interface remains small enough for the "implement in a few hours" contributor story (Journey 3, Kenji).

**Interface contract:**
```java
public interface ServerAdapter {
    // Introspection (existing)
    boolean isRunning();
    int getHttpPort();
    String getContextRoot();
    // Configuration (new)
    Path resolveOutputDir(String serverName, Path projectDir);
    void writeServerConfig(ServerConfigParams params);
}
```

**File-system side-effect documentation requirement:** Each `ServerAdapter` implementation must document which files it creates or modifies and what content it writes. This enables test assertions without requiring a real server installation. Example for `LibertyServerAdapter`: "writes `bootstrap.properties` at `{serverDir}/bootstrap.properties`, adds keys `javax.faces.FACELETS_REFRESH_PERIOD=0` and `org.apache.myfaces.REFRESH_PERIOD=0` if not already present."

**Affects:** `core` module, `LibertyServerAdapter`, new `TomcatServerAdapter`, contributor documentation, CONTRIBUTING.md guide

### Event Pipeline

**Decision:** Direct callbacks via `Consumer<Path>`.

**Rationale:** The pipeline is linear with 3 stages (detect → copy → broadcast) and 2 event types (changed/deleted). Direct callbacks are simple, debuggable, and sufficient. Debounce is implemented as a time-window coalescing wrapper — accumulate events for 300ms, then fire a single callback with the batch.

**Testability:** The `DevServer` orchestrator accepts a `ScheduledExecutorService` via `DevServerConfig`. Production code passes `Executors.newScheduledThreadPool(1)`. Tests pass a controllable executor to advance time synthetically for debounce testing and to verify graceful shutdown (executor termination).

**Affects:** `FileChangeWatcher` API, `DevServer` orchestrator wiring

### Configuration Model

**Decision:** Immutable config POJO with builder pattern.

**Rationale:** `DevServerConfig.builder()` validates required fields at build time, is trivially testable, and scales cleanly for v2 options (`classReloadEnabled`, `attachMode`). Both Gradle and Maven plugin modules construct the same config object.

**Builder contract:**
```java
DevServerConfig config = DevServerConfig.builder()
    .port(35729)
    .outputDir(path)
    .watchDirs(dirs)
    .serverAdapter(adapter)       // instance, not class reference
    .debounceMs(300)
    .executor(scheduledExecutor)  // injectable for testability
    .build();
```

**Affects:** `core` module, both plugin modules (config construction)

### Maven Lifecycle Integration

**Decision:** Single user-facing `jsf-autoreload:dev` goal with prepare logic as a separate testable class called internally.

**Rationale:** Users type one command: `mvn jsf-autoreload:dev`. The mojo internally calls a `PrepareStep` class (shared logic from core + server-specific config from adapter) before delegating server start and entering the dev loop. The `PrepareStep` is a separate class for testability but not a separate mojo — users never need to know about it.

**Testing strategy:**
- **Unit tests:** Plain JUnit on `PrepareStep` class (no Maven infrastructure needed)
- **Integration tests:** `maven-invoker-plugin` — runs real Maven builds against test projects, analogous to Gradle TestKit. Avoid `maven-plugin-testing-harness` (brittle, poorly maintained).

**Affects:** `maven-plugin` module structure, user documentation, zero-config experience driver

### Decision Impact Analysis

**Implementation Sequence:**
1. Create empty `core` and `maven-plugin` module shells; rename `plugin` → `gradle-plugin` (scaffolding)
2. Extract existing classes from `gradle-plugin` to `core` (pure move, no API changes — verify all tests pass)
3. Expand `ServerAdapter` interface with config methods (unblocks adapter implementations)
4. `DevServerConfig` builder with injectable executor (unblocks orchestrator)
5. `DevServer` orchestrator with callback pipeline (unblocks plugin wiring)
6. Port coordination via properties file (unblocks `DevModeFilter` update)
7. `TomcatServerAdapter` implementation (validates adapter abstraction — the "prove the platform" milestone)
8. Maven mojo with internal prepare logic (depends on all above)

**Cross-Component Dependencies:**
- `DevServerConfig` references `ServerAdapter` — config builder accepts an adapter *instance* constructed by the plugin module
- Port coordination change affects both `DevModeFilter` (runtime) and prepare logic (plugins) — must be updated in sync
- Maven mojo design depends on `DevServer` orchestrator API being stable first
- `TomcatServerAdapter` validates the expanded interface before Maven work begins

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

**Critical Conflict Points Identified:** 5 areas where AI agents could make different choices, all resolved with explicit patterns below.

### Java Code Conventions

- **Class naming:** PascalCase, noun-based. Adapters: `{Server}ServerAdapter`. Tasks: `Jsf{Action}Task`. Mojos: `{Action}Mojo`.
- **Method naming:** camelCase, verb-first. Getters: `getPort()`. Booleans: `isRunning()`, never `running()`.
- **Package structure:** `it.bstz.jsfautoreload.{module-concern}`. Server adapters: `it.bstz.jsfautoreload.server.{servername}` (e.g., `.server.liberty`, `.server.tomcat`).
- **Constants:** `UPPER_SNAKE_CASE`. In the class that owns them, not in a shared `Constants` class.
- **No wildcard imports.** Explicit imports only.
- **No `var` keyword** — explicit types improve readability for contributors at varying skill levels.

### Error Handling & Exceptions

- **Core module:** Unchecked exceptions only. Two custom types:
  - `JsfAutoreloadException` — general errors (extends `RuntimeException`)
  - `JsfAutoreloadConfigException` — configuration/validation errors (extends `JsfAutoreloadException`)
- **Plugin modules:** Catch core exceptions and translate to build-tool-specific exceptions (`GradleException`, `MojoExecutionException`). Core exceptions must never leak to the user.
- **ServerAdapter implementations:** Throw `JsfAutoreloadException` or `JsfAutoreloadConfigException`. Never throw `IOException` directly — wrap it.
- **Error message format:** Always actionable. `"[JSF Autoreload] {what went wrong}. {how to fix it}."` Example: `"[JSF Autoreload] Output directory not found: /path/to/dir. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name."`
- **Never swallow exceptions silently.** At minimum, log at WARN level before suppressing.

### Logging

- **Core module:** `java.util.logging` (JUL) only — zero dependency.
- **Plugin modules:** Build tool's native logging (`project.getLogger()` for Gradle, `getLog()` for Maven). Never JUL in plugin modules.
- **Runtime module:** `java.util.logging` (same as core).
- **Logger naming:** One logger per class: `private static final Logger LOG = Logger.getLogger(ClassName.class.getName());`
- **Log levels:**
  - `SEVERE` — Fatal errors that stop the dev loop
  - `WARNING` — Non-fatal issues needing attention
  - `INFO` — Lifecycle events only (started, stopped)
  - `FINE` — Per-file-change events (detected, copied, broadcast)
- **Console prefix:** All user-facing messages prefixed with `[JSF Autoreload]`.
- **Never log stack traces at INFO or above** unless SEVERE. Use `FINE` for diagnostic traces.

### Resource Management & Threading

- **All long-lived resources implement `Closeable`** — `DevWebSocketServer`, `FileChangeWatcher`, `DevServer`. Enables try-with-resources in tests.
- **Thread creation:** Never raw `new Thread()`. Use `ScheduledExecutorService` (injectable via config) or `ExecutorService`. All threads must be daemon threads.
- **Shutdown order:** Reverse of startup. Startup: watcher → WebSocket server → dev loop. Shutdown: dev loop → WebSocket server → watcher.
- **Shutdown timeout:** Each component gets 1 second. Total shutdown within NFR16's 2-second budget. Use `executor.awaitTermination(1, TimeUnit.SECONDS)`.
- **File I/O:** Always `java.nio.file` (`Files.copy`, `Files.readString`, `Files.writeString`). `Path` everywhere, `File` only when a library API requires it.

### Test Organization & Conventions

- **Test location:** `src/test/java` mirroring main package structure.
- **Test class naming:** `{ClassUnderTest}Test.java` for unit tests. `{Feature}IntegrationTest.java` for integration tests.
- **Test method naming:** camelCase describing behavior: `copiesFileToOutputDirOnModifyEvent()`. No `test_` or `should_` prefix.
- **Test structure:** Arrange-Act-Assert with blank lines separating sections. No section comments.
- **Assertions:** JUnit 5 only (`assertEquals`, `assertTrue`, `assertThrows`). No Hamcrest, no AssertJ.
- **Timeouts:** Async tests use `CountDownLatch` with explicit timeout. Never `Thread.sleep()` unless documented with a comment explaining why.
- **Temp files:** JUnit 5 `@TempDir` for all file system tests. Never write to real project directory.
- **Test independence:** Each test runnable in isolation. No shared mutable state, no ordering dependencies.

### Enforcement Guidelines

**All AI Agents MUST:**
- Follow the naming conventions above — inconsistency between modules breaks contributor trust
- Use the error handling chain (core throws custom exceptions → plugin translates to build-tool exceptions)
- Use JUL in core/runtime, build-tool logging in plugins — mixing frameworks causes classpath issues
- Implement `Closeable` on any class that holds threads, sockets, or file handles
- Write tests following the conventions above — inconsistent test styles slow down code review

**Anti-Patterns:**
- `catch (Exception e) { /* ignore */ }` — never swallow exceptions
- `System.out.println("debug: ...")` — use JUL at `FINE` level
- `new Thread(() -> { ... }).start()` — use executor services
- `import java.io.*;` — no wildcard imports
- `Thread.sleep(500)` in tests — use `CountDownLatch` with timeout

## Project Structure & Boundaries

### Current State (v0.1-beta) → Target State (v1.0)

Existing structure is a two-module Gradle build (`jsf-autoreload-plugin`, `jsf-autoreload-runtime`). v1.0 adds `core` and `maven-plugin` modules, and renames `plugin` → `gradle-plugin` for symmetric naming.

### Complete Project Directory Structure

```
jsf-autoreload/
├── settings.gradle.kts                         # includes all 4 modules
├── build.gradle.kts                            # root build (shared config)
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── ci.yml                              # NEW: GitHub Actions CI pipeline
├── README.md
├── CONTRIBUTING.md                             # NEW: contributor guide with ServerAdapter walkthrough
├── docs/                                       # NEW: detailed documentation
│   ├── architecture.md                         # link to this document
│   └── configuration.md                        # configuration reference
├── examples/                                   # NEW: working example projects
│   ├── liberty-gradle/                         # Liberty + Gradle example
│   └── tomcat-maven/                           # Tomcat + Maven example
│
├── jsf-autoreload-core/                        # NEW MODULE: shared logic
│   ├── build.gradle.kts                        # java-library + maven-publish
│   └── src/
│       ├── main/java/it/bstz/jsfautoreload/
│       │   ├── DevServer.java                  # orchestrator entry point
│       │   ├── DevServerConfig.java            # immutable config with builder
│       │   ├── JsfAutoreloadException.java     # base exception
│       │   ├── JsfAutoreloadConfigException.java
│       │   ├── server/
│       │   │   ├── ServerAdapter.java          # MOVED from gradle-plugin
│       │   │   ├── ServerConfigParams.java     # NEW: config params POJO
│       │   │   ├── liberty/
│       │   │   │   └── LibertyServerAdapter.java  # MOVED from gradle-plugin
│       │   │   └── tomcat/
│       │   │       └── TomcatServerAdapter.java   # NEW
│       │   ├── watcher/
│       │   │   └── FileChangeWatcher.java      # MOVED from gradle-plugin
│       │   └── websocket/
│       │       └── DevWebSocketServer.java     # MOVED from gradle-plugin
│       └── test/java/it/bstz/jsfautoreload/
│           ├── DevServerTest.java              # NEW
│           ├── DevServerConfigTest.java        # NEW
│           ├── server/
│           │   ├── liberty/
│           │   │   └── LibertyServerAdapterTest.java  # MOVED from gradle-plugin
│           │   └── tomcat/
│           │       └── TomcatServerAdapterTest.java   # NEW
│           ├── watcher/
│           │   └── FileChangeWatcherTest.java  # MOVED from gradle-plugin
│           └── websocket/
│               └── DevWebSocketServerTest.java # MOVED from gradle-plugin
│
├── jsf-autoreload-gradle-plugin/               # EXISTING (renamed from plugin)
│   ├── build.gradle.kts                        # java-gradle-plugin + shadow + depends on core
│   └── src/
│       ├── main/java/it/bstz/jsfautoreload/
│       │   ├── JsfAutoreloadPlugin.java        # EXISTING (updated to use core)
│       │   ├── JsfAutoreloadExtension.java     # EXISTING
│       │   ├── JsfPrepareTask.java             # EXISTING (updated to delegate to core)
│       │   ├── JsfDevTask.java                 # EXISTING (updated to use DevServer)
│       │   └── compiler/
│       │       └── JavaSourceCompiler.java     # EXISTING (stays — Gradle-specific)
│       ├── main/resources/META-INF/gradle-plugins/
│       │   └── it.bstz.jsf-autoreload.properties
│       └── test/java/it/bstz/jsfautoreload/
│           ├── JsfAutoreloadPluginIntegrationTest.java  # EXISTING
│           └── compiler/
│               └── JavaSourceCompilerTest.java          # EXISTING
│
├── jsf-autoreload-maven-plugin/                # NEW MODULE: Maven plugin
│   ├── build.gradle.kts                        # maven-publish, depends on core
│   ├── pom.xml                                 # Maven plugin descriptor
│   └── src/
│       ├── main/java/it/bstz/jsfautoreload/
│       │   ├── JsfDevMojo.java                 # user-facing goal
│       │   └── PrepareStep.java                # testable prepare logic
│       └── test/java/it/bstz/jsfautoreload/
│           └── PrepareStepTest.java
│
└── jsf-autoreload-runtime/                     # EXISTING: runtime JAR (unchanged)
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── java/it/bstz/jsfautoreload/
        │   │   └── filter/
        │   │       └── DevModeFilter.java      # EXISTING (updated for properties file port)
        │   └── resources/META-INF/
        │       └── web-fragment.xml
        └── test/java/it/bstz/jsfautoreload/
            └── filter/
                └── DevModeFilterTest.java      # EXISTING (add properties file test cases)
```

### Architectural Boundaries

**Module Boundaries (hard rules):**

```
runtime        → zero dependencies, standalone
core           → zero build-tool dependencies, standalone
gradle-plugin  → depends on core only
maven-plugin   → depends on core only
```

**Core Module Boundary:**
- Exposes: `DevServer`, `DevServerConfig`, `ServerAdapter`, `FileChangeWatcher`, `DevWebSocketServer`, custom exceptions
- Receives: Configuration via `DevServerConfig` builder, `ServerAdapter` instance passed in
- Never imports: `org.gradle.*`, `org.apache.maven.*`, `javax.servlet.*`

**Gradle Plugin Module Boundary:**
- Exposes: Gradle plugin, extension DSL, tasks
- Receives: User configuration via Gradle DSL, passes to core via `DevServerConfig`
- Owns: Gradle task wiring, `runtimeJar` configuration, shadow/relocation, `JavaSourceCompiler`
- Translates: Core `JsfAutoreloadException` → `GradleException`

**Maven Plugin Module Boundary:**
- Exposes: Maven goal `jsf-autoreload:dev`
- Receives: User configuration via Maven `<configuration>`, passes to core via `DevServerConfig`
- Owns: Mojo lifecycle, runtime JAR resolution via Maven dependency mechanism, `PrepareStep`
- Translates: Core `JsfAutoreloadException` → `MojoExecutionException`

**Runtime Module Boundary:**
- Exposes: Nothing — auto-registers via `web-fragment.xml`
- Receives: Port via classpath properties file (fallback: system property, then default)
- Never imports: Anything outside `javax.servlet.*` and `java.*`

### Requirements to Structure Mapping

**FR Category → Module Mapping:**

| FR Category | Module(s) |
|---|---|
| Plugin Installation & Config (FR1-FR7) | `gradle-plugin` (Gradle), `maven-plugin` (Maven) |
| Build & Server Lifecycle (FR8-FR11) | `gradle-plugin`, `maven-plugin`, `core` (ServerAdapter) |
| Dev Loop Lifecycle (FR12-FR13) | `core` (DevServer orchestrator) |
| File Watching & Sync (FR14-FR19) | `core` (FileChangeWatcher) |
| Browser Refresh (FR20-FR25) | `core` (DevWebSocketServer), `runtime` (DevModeFilter) |
| JSF Template Reload (FR26-FR28) | `core` (ServerAdapter.writeServerConfig) |
| Error Handling (FR29-FR32) | `core` (exceptions), `gradle-plugin`/`maven-plugin` (translation) |
| Server Extensibility (FR33-FR34) | `core` (ServerAdapter interface + implementations) |
| Distribution & Docs (FR35-FR39) | Root (CI, README, CONTRIBUTING, examples) |

### Data Flow

```
User saves file
    → FileChangeWatcher detects event (core)
    → Debounce coalesces events (core, ScheduledExecutorService)
    → File copied to outputDir (core)
    → DevWebSocketServer broadcasts "reload" (core)
    → Browser receives WebSocket message (runtime JS snippet)
    → Browser reloads page
    → DevModeFilter intercepts response, injects script (runtime)
```

### Build Process Structure

**Development:** `./gradlew build` at root builds all four modules. Shadow JAR for `gradle-plugin` includes `core` with relocated WebSocket library.

**Publishing:**
- `core` → Maven Central (un-relocated JAR, for direct consumption and testing)
- `gradle-plugin` → Gradle Plugin Portal (shadow JAR with core + relocated deps)
- `maven-plugin` → Maven Central (JAR with core dependency declared)
- `runtime` → Maven Central (dependency-free JAR)

## Architecture Validation Results

### Coherence Validation

**Decision Compatibility:** All technology choices and architectural decisions are mutually compatible. No contradictions found between module boundaries, dependency rules, error handling chain, logging strategy, or build tooling decisions. Shadow relocation strategy (per-plugin uber-JAR) is compatible with the un-relocated core JAR.

**Pattern Consistency:** Implementation patterns (naming, error handling, logging, resource management, testing) align with all architectural decisions. Module boundaries naturally enforce the logging split (JUL in core/runtime, build-tool native in plugins) and the exception translation chain.

**Structure Alignment:** Four-module project structure supports all decisions. Dependency direction rule is enforceable by CI per-module compile checks. Module boundaries prevent API leakage.

### Requirements Coverage Validation

**Functional Requirements: 39/39 covered**

| FR Category | FRs | Architectural Support |
|---|---|---|
| Installation & Config | FR1-FR7 | `gradle-plugin`, `maven-plugin`, `DevServerConfig`, `ServerAdapter.resolveOutputDir()` |
| Build & Server Lifecycle | FR8-FR11 | Task/mojo wiring, `ServerAdapter` introspection methods |
| Dev Loop Lifecycle | FR12-FR13 | `DevServer.start()` blocking, `Closeable` shutdown |
| File Watching & Sync | FR14-FR19 | `FileChangeWatcher` with direct callbacks |
| Browser Refresh | FR20-FR25 | `DevWebSocketServer`, `DevModeFilter`, `web-fragment.xml` |
| JSF Template Reload | FR26-FR28 | `ServerAdapter.writeServerConfig()` with deduplication |
| Error Handling | FR29-FR32 | Custom exceptions, actionable message format |
| Server Extensibility | FR33-FR34 | `ServerAdapter` interface + Liberty/Tomcat implementations |
| Distribution & Docs | FR35-FR39 | CI pipeline, README, CONTRIBUTING.md, examples/ |

**Non-Functional Requirements: 21/21 covered**

| NFR Category | NFRs | Architectural Support |
|---|---|---|
| Performance | NFR1-NFR6 | Sub-2s driver, direct callbacks, `ScheduledExecutorService` debounce |
| Compatibility | NFR7-NFR14 | CI matrix (Java 11/17/21), Gradle 7+/8+, Maven 3.6+, zero runtime deps, cross-platform via `directory-watcher` |
| Reliability | NFR15-NFR18 | `Closeable`, daemon threads, reverse shutdown order, transient error recovery |
| Documentation Quality | NFR19-NFR21 | README with compatibility matrix, examples/, CONTRIBUTING.md |

### Implementation Readiness Validation

**Decision Completeness:** All 6 core decisions documented with rationale, interface contracts, and code examples. Open questions from earlier steps (shadow relocation, port coordination) are resolved.

**Structure Completeness:** Complete directory tree with every file annotated as EXISTING, MOVED, or NEW. Module boundaries documented with explicit import restrictions.

**Pattern Completeness:** 5 conflict areas resolved with concrete patterns, examples, and anti-patterns. Enforcement guidelines documented.

### Gap Analysis Results

**Critical Gaps:** 0

**Important Gaps:** 1 (resolved)

- **Maven plugin build mechanism:** The `maven-plugin` module is built by Gradle but must produce a valid Maven plugin artifact with `META-INF/maven/plugin.xml`. **Resolution:** Use `de.benediktritter.maven-plugin-development` Gradle plugin to generate the Maven descriptor from `@Mojo`/`@Parameter` annotations during the Gradle build. This keeps a single build system — contributors don't need Maven installed. End users consume the published artifact normally via `pom.xml`.

**Nice-to-Have Gaps:** 1

- **`JavaSourceCompiler` architectural placement:** The existing `JavaSourceCompiler` class stays in `gradle-plugin` (Gradle-specific). The tech spec marks Java class reloading as out of scope for v1. If v2 promotes this to core, it would need to be refactored to remove Gradle API dependencies. No action needed for v1.

### Architecture Completeness Checklist

**Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed (Low overall, Medium for cross-module coordination)
- [x] Technical constraints identified (7 constraints documented)
- [x] Cross-cutting concerns mapped (7 concerns documented)
- [x] Primary architectural drivers identified (zero-config + sub-2s loop)

**Architectural Decisions**
- [x] Critical decisions documented (3: core API, port coordination, ServerAdapter)
- [x] Important decisions documented (3: event pipeline, config model, Maven lifecycle)
- [x] Deferred decisions documented (2: Jakarta EE 9+, attach mode)
- [x] Implementation sequence defined (8 ordered steps)
- [x] Cross-component dependencies mapped

**Implementation Patterns**
- [x] Java code conventions established
- [x] Error handling & exception strategy defined
- [x] Logging strategy defined
- [x] Resource management & threading patterns defined
- [x] Test organization & conventions defined
- [x] Anti-patterns documented

**Project Structure**
- [x] Complete directory structure defined with annotations
- [x] Module boundaries established with import restrictions
- [x] FR-to-module mapping complete (39 FRs → 4 modules)
- [x] Data flow documented
- [x] Build process and publishing strategy defined

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

**Confidence Level:** High — all 60 requirements traced to architectural support, all decisions are coherent, and patterns are comprehensive enough to prevent agent implementation conflicts.

**Key Strengths:**
- Clear module boundaries with enforceable dependency rules
- Explicit implementation sequence (8 steps) with dependencies mapped
- Two architectural drivers (zero-config, sub-2s loop) provide a north star for all decisions
- Create-then-extract scaffolding sequence minimizes extraction risk
- Comprehensive implementation patterns prevent the most common AI agent conflict points

**Areas for Future Enhancement:**
- Jakarta EE 9+ (`jakarta.servlet`) support architecture (Phase 2)
- Attach mode architecture — connecting to already-running servers (Phase 2)
- `JavaSourceCompiler` potential promotion to core for Java class reloading (Phase 3)

### Implementation Handoff

**AI Agent Guidelines:**
- Follow all architectural decisions exactly as documented
- Use implementation patterns consistently across all modules
- Respect module boundaries — `core` must never import Gradle, Maven, or Servlet APIs
- Refer to this document for all architectural questions
- When in doubt, test against the two architectural drivers: does it preserve zero-config? Does it preserve the sub-2s loop?

**First Implementation Priority:**
1. Rename `jsf-autoreload-plugin` → `jsf-autoreload-gradle-plugin` in `settings.gradle.kts`
2. Create empty `jsf-autoreload-core` and `jsf-autoreload-maven-plugin` module shells
3. Begin class extraction from `gradle-plugin` to `core`
