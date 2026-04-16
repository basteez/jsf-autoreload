# Research: JSF Hot Reload Plugin

**Feature Branch**: `001-jsf-hot-reload`
**Date**: 2026-04-16

## Research Task 1: Java Version

**Context**: The feature spec states Java 8 minimum (`--release 8`), single artifact running on Java 8, 11, 17, 21+, with multi-target JDK-optimized builds added later via CI/CD. The constitution has been amended to align (v1.0.1).

**Decision**: Java 8+ (compile with `--release 8`)

**Rationale**:
- The plugin targets legacy JSF projects — many of which still run on Java 8. Requiring Java 11+ would exclude a significant portion of the target audience.
- Compiling with `--release 8` ensures bytecode and API compatibility with Java 8 while allowing development on modern JDKs.
- `java.nio.file.WatchService` (the most critical API for this project) is available since Java 7.
- Multi-target JDK-optimized builds (e.g., using `java.net.http.HttpClient` on Java 11+) can be introduced later via CI/CD profiles if performance profiling warrants it.
- Single artifact, no classifier variants — keeps the dependency story simple for consumers.

**Alternatives considered**:
- Java 11+: Rejected — excludes legacy JSF shops still on Java 8, which are the primary audience for this plugin
- Java 17+: Rejected — overly restrictive for a plugin targeting legacy projects
- Separate artifacts per Java version: Rejected — adds maintenance burden; multi-release JAR or CI/CD profiles are simpler

## Research Task 2: JSF Version Range — Resolve Spec vs Constitution Conflict

**Context**: The feature spec says "JSF 2.0–2.3" for javax.faces support. The constitution says "JSF 2.3 (javax.faces)".

**Decision**: JSF 2.3+ for javax.faces, Jakarta Faces 3.0+ for jakarta.faces

**Rationale**:
- JSF 2.3 is the final javax.faces release and the natural boundary before the Jakarta migration.
- Supporting JSF 2.0–2.2 would require accommodating APIs that changed significantly between versions.
- JSF 2.3 introduced `@PushContext` and modern CDI integration — the baseline the plugin should target.
- The constitution explicitly scopes to "JSF 2.3 (javax.faces) and Jakarta Faces 3.0+".

**Alternatives considered**:
- JSF 2.0+: Rejected — constitution conflict; JSF 2.0/2.1 API differences increase complexity for negligible user base
- Jakarta Faces only: Rejected — spec explicitly requires javax.faces support for legacy projects

## Research Task 3: javax/jakarta Dual Namespace in Single Artifact

**Context**: FR-015 requires both `javax.faces` (JSF 2.3) and `jakarta.faces` (Faces 3.0+) in a single artifact with runtime detection.

**Decision**: Namespace Abstraction Layer with Bridge Pattern

**Rationale**:
The plugin interacts with a small surface area of the JSF/Servlet APIs:
- `FacesContext` (detecting project stage, getting external context)
- `SystemEventListener` / `PhaseListener` (injecting script into views)
- `UIComponent` / `UIOutput` (adding script component)
- `ServletContainerInitializer` (registering SSE servlet)
- `HttpServletRequest` / `HttpServletResponse` (SSE endpoint)
- `AsyncContext` (keeping SSE connections open)

Approach:
1. Define internal interfaces (`JsfBridge`, `ServletBridge`) abstracting namespace-specific operations
2. Provide two implementations in separate packages:
   - `it.bstz.jsfautoreload.bridge.javax` — imports `javax.faces.*`, `javax.servlet.*`
   - `it.bstz.jsfautoreload.bridge.jakarta` — imports `jakarta.faces.*`, `jakarta.servlet.*`
3. At startup, use `Class.forName("jakarta.faces.context.FacesContext")` to detect namespace
4. Instantiate the appropriate bridge implementation
5. All core logic works through bridge interfaces — never imports javax/jakarta directly

Compilation strategy:
- Both `javax.faces-api` and `jakarta.faces-api` declared as `provided` dependencies
- Both bridge packages compile in the same module (different imports, no class conflict)
- Only one bridge instantiated at runtime

**Alternatives considered**:
- Separate artifacts per namespace: Rejected — FR-015 explicitly requires single artifact
- Reflection-only (no compile-time deps): Rejected — error-prone, hard to maintain
- Multi-release JAR (MRJAR): Rejected — MRJAR is for Java version differences, not namespace differences
- Bytecode transformation: Rejected — overly complex for this scope

## Research Task 4: File System Watching in Java

**Context**: FR-001 requires monitoring directories for file changes.

**Decision**: `java.nio.file.WatchService` with configurable polling fallback

**Rationale**:
- `WatchService` is the standard Java API for file system notifications (since Java 7)
- On Linux: uses `inotify` (efficient, event-driven)
- On macOS: uses `kqueue` via polling (known ~2s latency). Mitigate with `SensitivityWatchEventModifier.HIGH`
- On Windows: uses `ReadDirectoryChangesW` (efficient)
- Recursive directory watching requires registering each subdirectory individually and handling `ENTRY_CREATE` events to register new subdirectories

Implementation:
- `DirectoryWatcher` component wrapping `WatchService`
- Recursive registration of all configured directories
- File pattern filtering (inclusion/exclusion globs)
- Emits `FileChangeEvent` objects to the debouncer
- Configurable polling fallback for unreliable environments

**Alternatives considered**:
- Apache Commons IO `FileAlterationMonitor`: Rejected — constitution mandates minimal dependencies
- JNA with native file watching: Rejected — adds native dependency, fragile across platforms

## Research Task 5: SSE Implementation with Servlet 3.0+ Async

**Context**: FR-006 requires Server-Sent Events for browser notification.

**Decision**: Servlet 3.0+ `AsyncContext`-based SSE implementation

**Rationale**:
- Servlet 3.0+ `AsyncContext` provides primitives for long-lived HTTP connections
- SSE protocol is simple: `Content-Type: text/event-stream`, messages formatted as `data: ...\n\n`
- No additional dependencies required
- Client-side `EventSource` auto-reconnects on disconnection (built into the SSE spec)

Implementation:
1. Register SSE servlet via `ServletContainerInitializer`
2. On GET: `request.startAsync()`, set content type, store `AsyncContext`
3. On file change: iterate active contexts, write SSE event
4. Handle disconnections via `AsyncListener.onTimeout()` and `onError()`
5. Servlet path: `/_jsf-autoreload/events` (underscore prefix to avoid app route collisions)

**Alternatives considered**:
- WebSocket: Rejected — FR-006 specifies SSE; bidirectional unnecessary
- Long polling: Rejected — SSE is more efficient for unidirectional push
- Third-party SSE library: Rejected — implementation is ~50 lines of servlet code

## Research Task 6: JSF Page Script Injection

**Context**: FR-007 requires transparent injection of reload script without template modification.

**Decision**: `SystemEventListener` on `PostAddToViewEvent` + auto-discovered `faces-config.xml`

**Rationale**:
- JSF auto-discovers `faces-config.xml` files in `META-INF/` of JARs on the classpath
- A `SystemEventListener` for `PostAddToViewEvent` fires whenever a view is built
- The listener programmatically adds a `UIOutput` component as a script resource to the view head
- This is the standard pattern used by JSF component libraries (PrimeFaces, OmniFaces)

Implementation:
1. Place `META-INF/faces-config.xml` in core JAR with system-event-listener registration
2. Listener checks Development stage (exits early if not dev mode)
3. Creates `UIOutput` component with appropriate script renderer type
4. Sets component value to inline SSE client JavaScript
5. Adds to view head via `UIViewRoot.addComponentResource()`

Injected script (minimal):
```javascript
(function() {
  var es = new EventSource('/_jsf-autoreload/events');
  es.onmessage = function() { location.reload(); };
})();
```
The `EventSource` API handles reconnection automatically per the SSE specification.

**Alternatives considered**:
- Servlet Filter wrapping response: Rejected — not a JSF spec mechanism; constitution requires spec compliance
- Custom UIComponent: Rejected — unnecessary complexity for inline script
- External script resource: Rejected — inline avoids extra HTTP request

## Research Task 7: Servlet Container Auto-Registration

**Context**: Plugin must register its SSE servlet without web.xml changes (SC-008).

**Decision**: `ServletContainerInitializer` (SCI) pattern

**Rationale**:
- Servlet 3.0+ defines SCI as a standard discovery mechanism
- Container discovers SCIs via `META-INF/services/javax.servlet.ServletContainerInitializer`
- SCI receives `ServletContext` at startup and dynamically registers servlets/listeners
- This is the standard zero-config pattern for servlet-based libraries

Implementation:
1. `JsfAutoreloadInitializer` implements the appropriate `ServletContainerInitializer`
2. Registered in both `META-INF/services/javax.servlet.ServletContainerInitializer` and `META-INF/services/jakarta.servlet.ServletContainerInitializer`
3. In `onStartup()`: detect dev mode → register SSE servlet → start file watcher → register shutdown listener

**Alternatives considered**:
- `@WebServlet` annotation: Rejected — no conditional registration based on dev mode
- `web-fragment.xml`: Rejected — no conditional logic; would register in production

## Research Task 8: Tomcat Programmatic Context Reload

**Context**: FR-012 requires servlet context reload when `.class` files change.

**Decision**: Tomcat `StandardContext.reload()` via `ServletContext` traversal

**Rationale**:
- Tomcat exposes its internal `Context` via `ServletContext` implementation hierarchy
- `StandardContext.reload()` triggers class reloading and servlet re-initialization
- The Tomcat adapter module has a `provided` dependency on `tomcat-catalina`

Implementation (Tomcat adapter):
1. Implement `ContainerAdapter` SPI interface
2. In `reload()`: obtain `StandardContext` via Tomcat's `ApplicationContextFacade` → `ApplicationContext` → `StandardContext`
3. Call `standardContext.reload()`
4. After reload, SSE connections drop — `EventSource` auto-reconnects
5. `SystemEventListener` re-registers on the new context

Notes:
- Session state is lost after reload (documented expected behavior, spec US3-AC3)
- The adapter auto-discovers via `ServiceLoader`

**Alternatives considered**:
- JMX MBean invocation: Rejected — more indirect, less reliable
- Instrumentation agent hot-swap: Rejected — different scope (method-level, not full reload)
- Manual classloader replacement: Rejected — `StandardContext.reload()` handles this correctly

## Research Task 9: Debouncing Strategy

**Context**: FR-005 requires debouncing rapid file changes into a single refresh.

**Decision**: Time-window debouncer using `ScheduledExecutorService`

**Rationale**:
- Classic debounce: on each event, reset a timer; fire when timer expires without interruption
- `ScheduledExecutorService.schedule()` supports cancellation and rescheduling
- Default debounce interval: 500ms (configurable via `PluginConfiguration`)
- Two debounce groups: VIEW_STATIC (fast, browser-only refresh) and CLASS (triggers container reload first)

Implementation:
1. Maintain a `ScheduledFuture<?>` per debounce group
2. On `FileChangeEvent`: determine group → cancel pending future → schedule new future
3. When future fires: emit `ReloadNotification` to SSE broadcaster
4. For CLASS group: invoke `ContainerAdapter.reload()` before SSE notification

**Alternatives considered**:
- RxJava debounce: Rejected — heavyweight dependency
- `Thread.sleep`: Rejected — blocks, no cancellation
- Rate limiter: Rejected — debounce provides better UX (coalesces bursts)

## Research Task 10: Logging Framework

**Context**: Constitution mandates `java.util.logging` or SLF4J.

**Decision**: `java.util.logging` (JUL)

**Rationale**:
- Zero additional dependencies (JUL is part of Java SE)
- Constitution Principle I demands minimal dependencies
- Most app servers configure JUL or have JUL bridges
- Consumers can add `jul-to-slf4j` bridge if they prefer SLF4J

Log format: `[JSF-AUTORELOAD] {level} | {event_type} | {file_path} | {details}`
Log levels: FINE = file events, INFO = reload triggers, WARNING = recoverable errors, SEVERE = fatal

**Alternatives considered**:
- SLF4J: Rejected as mandatory dependency — adds a JAR. JUL is zero-cost
- Log4j2: Rejected — adds dependency, security perception issues post-Log4Shell

## Research Task 11: Maven Multi-Module Structure

**Context**: FR-012 requires modular architecture for container adapters.

**Decision**: Four Maven modules under a parent POM

**Module structure**:
```
jsf-autoreload/                          (parent POM, packaging: pom)
├── jsf-autoreload-core/                 (core: watcher, SSE, JSF integration, SPI)
├── jsf-autoreload-tomcat/               (Tomcat container adapter)
├── jsf-autoreload-maven-plugin/         (Maven plugin for auto-compile)
└── jsf-autoreload-integration-tests/    (integration tests, not published)
```

Dependency graph:
- `core` → JSF API (provided), Servlet API (provided)
- `tomcat` → `core`, Tomcat Catalina (provided)
- `maven-plugin` → Maven Plugin API, Maven Project API
- `integration-tests` → `core`, `tomcat`, Embedded Tomcat (test)

Base package: `it.bstz.jsfautoreload`

**Alternatives considered**:
- Single module: Rejected — FR-012 explicitly requires modular architecture
- Gradle: Rejected — constitution mandates Maven as primary build tool
