# Implementation Plan: Fix Reload Bugs

**Branch**: `002-fix-reload-bugs` | **Date**: 2026-04-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-fix-reload-bugs/spec.md`

## Summary

Two critical bugs prevent the jsf-autoreload plugin from functioning: (1) the SSE client script is never injected into JSF pages because `ScriptInjector` is never instantiated or registered during bootstrap, leaving 0 browser connections; (2) the heartbeat scheduler is a no-op, allowing proxies/browsers to silently drop idle SSE connections. The fix involves wiring `ScriptInjector` into the initialization sequence with deferred JSF Application access, implementing a working heartbeat that sends SSE comments to all connections, and adding proactive stale-connection cleanup via `AsyncListener`.

## Technical Context

**Language/Version**: Java 8+ (compile with `--release 8`)
**Primary Dependencies**: JSF API (javax.faces 2.3 / jakarta.faces 3.0+), Servlet API 3.0+ (async support) — all `provided` scope
**Storage**: N/A
**Testing**: JUnit 5.11.4, Mockito 4.11.0, Embedded Tomcat 9.0.98 (integration tests)
**Target Platform**: Any Servlet 3.0+ container (Tomcat, WildFly, Payara, etc.)
**Project Type**: Library (JAR plugin)
**Performance Goals**: Browser refresh within 3s of file change (SC-001), SSE connections survive 5+ min idle (SC-003)
**Constraints**: Zero production impact (Constitution IV), single JAR artifact, no implementation-specific imports
**Scale/Scope**: Single-developer plugin; typically 1-10 concurrent browser connections

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Plugin Modularity | PASS | Fix touches only core module internals; no new dependencies or container coupling introduced |
| II. Test-Driven Development (NON-NEGOTIABLE) | PASS | All bug fixes require tests first: unit tests for ScriptInjector wiring, heartbeat behavior, connection cleanup; integration tests for end-to-end SSE flow |
| III. JSF Specification Compliance | PASS | ScriptInjector uses standard `PostAddToViewEvent` and `UIOutput`; no implementation internals |
| IV. Zero Production Impact | PASS | ScriptInjector checks `ProjectStage.Development`; heartbeat only runs in dev mode |
| V. Observability and Diagnostics | PASS | All fixes maintain existing logging patterns (ReloadLogger) with file path, timestamp, and actionable messages |

**Gate result: PASS** — no violations.

## Root Cause Analysis

### Bug 1: SSE broadcasting to 0 connections (CRITICAL)

**Location**: `AutoreloadInitializer.bootstrap()` — `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/init/AutoreloadInitializer.java:64-115`

**Root cause**: The `ScriptInjector` class is imported (line 12) but never instantiated or called in `bootstrap()`. The method creates `ConnectionManager`, `ReloadCoordinator`, `Debouncer`, `DefaultSseHandler`, and `DirectoryWatcher`s — but omits the step that would inject the EventSource client script into JSF pages. Without the script, no browser ever opens an SSE connection to `/_jsf-autoreload/events`, so `ConnectionManager.connections` is always empty.

**Complication**: `ScriptInjector.register()` requires a JSF `Application` object (`JsfBridge.registerScriptInjector(application, scriptContent)` → `Application.subscribeToEvent(PostAddToViewEvent.class, ...)`). During `ServletContainerInitializer.onStartup()`, the JSF `Application` is not yet available because JSF initializes after the servlet container. The registration must be deferred.

**Fix approach**: In each bridge's `registerDeferredScriptInjector()`, programmatically add a `ServletContextListener` via `servletContext.addListener()`. The Servlet spec guarantees these fire AFTER declared listeners (Mojarra/MyFaces), so the JSF `Application` is available. The listener retrieves it via `FactoryFinder` and calls `ScriptInjector.register()`. A `faces-config.xml` `PostConstructApplicationEvent` approach was attempted first but abandoned because Mojarra 2.3.9 throws a fatal `ConfigurationException` when it cannot instantiate the jakarta listener class in a javax environment.

**Additional bugs discovered during implementation**:
- `PostAddToViewEvent` infinite recursion: `addComponentResource()` triggers the event again. Fixed with a request-scoped re-entry guard.
- `UIOutput` Script renderer: `javax.faces.resource.Script` renderer does not render inline script content via `setValue()`. Fixed by overriding `encodeBegin()`/`encodeEnd()` to write `<script>` tags directly.
- Context reload destroys SSE connections before browsers process the reload event. Fixed with client-side `onerror` handler that reloads the page on connection loss.
- Tomcat `reloadable=true` races with our plugin's file watcher. Users must set `reloadable=false` when using jsf-autoreload.

### Bug 2: Heartbeat is a no-op (HIGH)

**Location**: `DefaultSseHandler.startHeartbeat()` — `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/DefaultSseHandler.java:69-72`

**Root cause**: The heartbeat scheduled task body is empty:
```java
heartbeatScheduler.scheduleAtFixedRate(() -> {
    // No-op if no connections; the comment keeps proxies from closing idle connections
}, 30, 30, TimeUnit.SECONDS);
```
The comment describes the intent but the implementation does nothing. SSE connections receive no keepalive data, allowing proxies, load balancers, and some browsers to drop the connection after their own idle timeout (typically 60-120s).

**Fix approach**: Iterate all connections in `ConnectionManager` and send an SSE comment (`:heartbeat\n\n`) every 30 seconds. Failed writes indicate dead connections and trigger removal.

### Bug 3: No proactive stale connection cleanup (MEDIUM)

**Location**: `ConnectionManager` — `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/sse/ConnectionManager.java`

**Root cause**: Dead connections (closed tabs, network drops) are only discovered when `broadcast()` catches an `IOException`. If no file changes occur, dead connections accumulate indefinitely. The heartbeat fix (Bug 2) partially mitigates this by sending periodic writes that detect failures. For immediate cleanup, an `AsyncListener` should be registered on each connection's `AsyncContext` to detect `onComplete`/`onTimeout`/`onError` events from the container.

**Fix approach**: In `DefaultSseHandler.handleRequest()`, after creating the `BrowserConnection`, register an `AsyncListener` (via the `ServletBridge`) on the `AsyncContext` that calls `connectionManager.remove(connection)` on disconnect events.

## Project Structure

### Documentation (this feature)

```text
specs/002-fix-reload-bugs/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
jsf-autoreload-core/
├── src/
│   ├── main/java/it/bstz/jsfautoreload/
│   │   ├── bridge/
│   │   │   ├── AsyncContextWrapper.java
│   │   │   ├── JsfBridge.java          # Interface — may need addAsyncListener()
│   │   │   ├── ServletBridge.java      # Interface — may need addAsyncListener()
│   │   │   ├── javax/
│   │   │   │   ├── JavaxJsfBridge.java  # registerScriptInjector() already correct
│   │   │   │   └── JavaxServletBridge.java
│   │   │   └── jakarta/
│   │   │       ├── JakartaJsfBridge.java
│   │   │       └── JakartaServletBridge.java
│   │   ├── init/
│   │   │   └── AutoreloadInitializer.java  # BUG 1: missing ScriptInjector wiring
│   │   ├── jsf/
│   │   │   └── ScriptInjector.java         # Correct but never called
│   │   └── sse/
│   │       ├── ConnectionManager.java      # BUG 3: no proactive cleanup
│   │       └── DefaultSseHandler.java      # BUG 2: no-op heartbeat
│   └── test/java/it/bstz/jsfautoreload/
│       └── sse/
│           └── ConnectionManagerTest.java  # Needs heartbeat + cleanup tests
│
jsf-autoreload-integration-tests/
└── src/test/java/it/bstz/jsfautoreload/it/
    ├── XhtmlReloadIT.java           # Should verify SSE connection established
    ├── MultiConnectionIT.java       # Should verify heartbeat keeps connections alive
    └── ...
```

**Structure Decision**: No new modules or directories. All fixes are within existing files in `jsf-autoreload-core`. May add new test classes for ScriptInjector wiring and heartbeat behavior.

## Complexity Tracking

> No constitution violations — table left empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
