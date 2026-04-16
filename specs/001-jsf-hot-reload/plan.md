# Implementation Plan: JSF Hot Reload

**Branch**: `001-jsf-hot-reload` | **Date**: 2026-04-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-jsf-hot-reload/spec.md`

## Summary

Build a JSF hot-reload plugin that monitors file changes (XHTML views, static resources, compiled classes) and automatically refreshes the browser via Server-Sent Events. The plugin supports both `javax.faces` (JSF 2.3) and `jakarta.faces` (Faces 3.0+) in a single artifact using a runtime namespace bridge. Architecture is Maven multi-module: core library with SPI, Tomcat adapter, Maven plugin for optional auto-compile, and integration tests.

## Technical Context

**Language/Version**: Java 11+ (LTS baseline per constitution; resolves spec conflict — see [research.md](./research.md) Task 1)
**Primary Dependencies**: JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+ — all `provided` scope
**Storage**: N/A (file system watching only, no persistent storage)
**Testing**: JUnit 5 (unit tests), Embedded Tomcat (integration tests), TDD mandatory per constitution
**Target Platform**: Java Servlet containers — Tomcat 9+ primary, pluggable for WildFly/Jetty/GlassFish via SPI
**Project Type**: Library (Maven multi-module: core + adapter + plugin + tests)
**Performance Goals**: View/static changes reflected in browser within 3s, class changes within 5s (SC-001/002/003)
**Constraints**: Zero production overhead (FR-008), spec-only APIs (constitution III), minimal dependencies (constitution I), single artifact for dual namespace (FR-015)
**Scale/Scope**: 10+ simultaneous browser connections (SC-006), 20 rapid saves debounced to ≤2 refreshes (SC-007)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Phase 0 Check

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Plugin Modularity | PASS | Core is a self-contained JAR depending only on JSF spec API + Java SE. Container adapters are separate modules via SPI — no tight coupling to specific servers. |
| II. Test-Driven Development | PASS | TDD (Red-Green-Refactor) will be enforced in all task definitions. Integration tests use embedded Tomcat. |
| III. JSF Specification Compliance | PASS | Script injection via `SystemEventListener` on `PostAddToViewEvent` + auto-discovered `faces-config.xml`. No implementation internals used. |
| IV. Zero Production Impact | PASS | `ServletContainerInitializer` checks dev mode at startup — returns immediately if not Development. `SystemEventListener` exits early. No servlets registered in production. |
| V. Observability & Diagnostics | PASS | `java.util.logging` with structured format: `[JSF-AUTORELOAD] {level} \| {event_type} \| {file_path} \| {details}`. All events logged per FR-011. |

**Spec-Constitution Conflicts Resolved:**

| Conflict | Spec Says | Constitution Says | Resolution |
|----------|-----------|-------------------|------------|
| Java version | Java 8 (`--release 8`) | Java 11+ (LTS baseline) | **Constitution prevails** — authoritative governance doc. See research Task 1. |
| JSF range | JSF 2.0–2.3 | JSF 2.3 | **Constitution prevails** — JSF 2.3 is the last javax.faces release; 2.0/2.1 API differences add complexity for negligible user base. |

### Post-Phase 1 Check

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Plugin Modularity | PASS | Maven multi-module structure confirmed. Core has zero container-specific imports. Adapter SPI defined. |
| II. TDD | PASS | Test structure defined in each module. Integration test module uses embedded Tomcat. |
| III. JSF Spec Compliance | PASS | Bridge pattern isolates all namespace-specific code. Core never imports javax/jakarta directly. |
| IV. Zero Production Impact | PASS | SCI pattern provides conditional registration. No production code paths. |
| V. Observability | PASS | Structured JUL logging throughout. ReloadNotification includes full diagnostics. |

**Gate: PASSED** — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-jsf-hot-reload/
├── plan.md                          # This file
├── research.md                      # Phase 0 output — 11 research decisions
├── data-model.md                    # Phase 1 output — 6 entities, 2 state machines
├── quickstart.md                    # Phase 1 output — developer getting-started guide
├── contracts/
│   ├── sse-endpoint.md              # SSE HTTP endpoint contract
│   ├── container-adapter-spi.md     # Container adapter Java SPI contract
│   └── jsf-bridge-spi.md           # Internal namespace bridge contract
└── tasks.md                         # Phase 2 output (created by /speckit-tasks)
```

### Source Code (repository root)

```text
jsf-autoreload/                                    # Parent POM (packaging: pom)
├── pom.xml
│
├── jsf-autoreload-core/                           # Core library
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/it/bstz/jsfautoreload/
│       │   │   ├── core/
│       │   │   │   ├── DirectoryWatcher.java      # File system monitoring (WatchService)
│       │   │   │   ├── Debouncer.java             # Time-window debounce (ScheduledExecutorService)
│       │   │   │   └── ReloadCoordinator.java     # Orchestrates: debounce → adapter reload → SSE broadcast
│       │   │   ├── sse/
│       │   │   │   ├── SseHandler.java            # SSE request handling (bridge-abstracted)
│       │   │   │   └── ConnectionManager.java     # Manages active browser connections
│       │   │   ├── jsf/
│       │   │   │   ├── ScriptInjector.java        # SystemEventListener — injects reload script
│       │   │   │   └── DevModeGuard.java          # Checks PROJECT_STAGE via bridge
│       │   │   ├── bridge/
│       │   │   │   ├── JsfBridge.java             # Interface: JSF namespace abstraction
│       │   │   │   ├── ServletBridge.java         # Interface: Servlet namespace abstraction
│       │   │   │   ├── BridgeDetector.java        # Runtime namespace detection
│       │   │   │   ├── AsyncContextWrapper.java   # Abstracted async context
│       │   │   │   ├── javax/                     # javax.faces/javax.servlet implementations
│       │   │   │   │   ├── JavaxJsfBridge.java
│       │   │   │   │   └── JavaxServletBridge.java
│       │   │   │   └── jakarta/                   # jakarta.faces/jakarta.servlet implementations
│       │   │   │       ├── JakartaJsfBridge.java
│       │   │   │       └── JakartaServletBridge.java
│       │   │   ├── config/
│       │   │   │   ├── PluginConfiguration.java   # Configuration model
│       │   │   │   └── ConfigurationReader.java   # Reads from context-params / system props
│       │   │   ├── spi/
│       │   │   │   ├── ContainerAdapter.java      # SPI interface
│       │   │   │   └── ReloadException.java       # SPI exception
│       │   │   ├── model/
│       │   │   │   ├── FileChangeEvent.java       # Detected file change
│       │   │   │   ├── ReloadNotification.java    # Debounced reload message
│       │   │   │   ├── ChangeType.java            # CREATED / MODIFIED / DELETED
│       │   │   │   ├── FileCategory.java          # VIEW / STATIC / CLASS / SOURCE / OTHER
│       │   │   │   └── DebounceGroup.java         # VIEW_STATIC / CLASS
│       │   │   ├── init/
│       │   │   │   └── AutoreloadInitializer.java # Bootstraps the plugin (called by SCI bridges)
│       │   │   └── logging/
│       │   │       └── ReloadLogger.java          # Structured JUL logging
│       │   └── resources/META-INF/
│       │       ├── faces-config.xml               # Auto-discovered: registers ScriptInjector
│       │       ├── services/
│       │       │   ├── javax.servlet.ServletContainerInitializer
│       │       │   └── jakarta.servlet.ServletContainerInitializer
│       │       └── web-fragment.xml               # Marks async-supported=true
│       └── test/java/it/bstz/jsfautoreload/
│           ├── core/
│           │   ├── DirectoryWatcherTest.java
│           │   └── DebouncerTest.java
│           ├── sse/
│           │   └── ConnectionManagerTest.java
│           ├── config/
│           │   └── ConfigurationReaderTest.java
│           ├── bridge/
│           │   └── BridgeDetectorTest.java
│           └── model/
│               └── FileCategoryTest.java
│
├── jsf-autoreload-tomcat/                         # Tomcat container adapter
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/it/bstz/jsfautoreload/tomcat/
│       │   │   └── TomcatAdapter.java             # Implements ContainerAdapter
│       │   └── resources/META-INF/services/
│       │       └── it.bstz.jsfautoreload.spi.ContainerAdapter
│       └── test/java/it/bstz/jsfautoreload/tomcat/
│           └── TomcatAdapterTest.java
│
├── jsf-autoreload-maven-plugin/                   # Maven plugin (auto-compile)
│   ├── pom.xml
│   └── src/
│       ├── main/java/it/bstz/jsfautoreload/maven/
│       │   ├── AutoCompileMojo.java               # Watches .java, invokes compiler
│       │   └── WatchMojo.java                     # Runs the watcher as Maven goal
│       └── test/java/it/bstz/jsfautoreload/maven/
│           └── AutoCompileMojoTest.java
│
└── jsf-autoreload-integration-tests/              # Integration tests (not published)
    ├── pom.xml
    └── src/test/
        ├── java/it/bstz/jsfautoreload/it/
        │   ├── XhtmlReloadIT.java
        │   ├── StaticResourceReloadIT.java
        │   ├── ClassReloadIT.java
        │   ├── DebouncingIT.java
        │   ├── MultiConnectionIT.java
        │   └── ProductionModeIT.java
        └── resources/
            └── test-webapp/                       # Minimal JSF app for testing
```

**Structure Decision**: Maven multi-module with four modules. Core is the self-contained library (constitution I). Tomcat adapter is the reference container adapter (FR-012). Maven plugin enables auto-compile (FR-013/014). Integration tests verify end-to-end behavior with embedded Tomcat. Base package: `it.bstz.jsfautoreload`.

## Complexity Tracking

> No constitution violations requiring justification. All design decisions align with the five principles.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *(none)* | — | — |
