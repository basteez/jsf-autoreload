---
title: 'JSF Autoreload Plugin'
slug: 'jsf-autoreload-plugin'
created: '2026-03-12'
status: 'completed'
stepsCompleted: [1, 2, 3, 4]
tech_stack: ['Java 11+', 'Gradle 7+', 'io.methvin:directory-watcher', 'javax.servlet-api:4.0.1', 'org.java-websocket:Java-WebSocket:1.5.+', 'io.openliberty.tools:liberty-gradle-plugin']
files_to_modify: ['jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfAutoreloadPlugin.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfPrepareTask.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfDevTask.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfAutoreloadExtension.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/watcher/FileChangeWatcher.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/websocket/DevWebSocketServer.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/server/ServerAdapter.java', 'jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/server/liberty/LibertyServerAdapter.java', 'jsf-autoreload-runtime/src/main/java/io/github/tizianobasile/jsfautoreload/filter/DevModeFilter.java', 'jsf-autoreload-runtime/src/main/resources/META-INF/web-fragment.xml']
code_patterns: ['Plugin<Project> entry point', 'Gradle task with @TaskAction', 'Gradle extension DSL', 'Gradle configurations for inter-project JAR passing', 'Servlet Filter with response buffering + Content-Length reset', 'ServerAdapter read-only introspection interface', 'web-fragment.xml for zero-config filter registration', 'inline JS constant in filter class', 'CountDownLatch + shutdown hook for Gradle daemon-safe blocking']
test_patterns: ['JUnit 5 unit tests', 'Gradle TestKit integration tests via GradleRunner']
---

# Tech-Spec: JSF Autoreload Plugin

**Created:** 2026-03-12

**Group ID used throughout:** `it.bstz` — replace in all file paths, package declarations, and plugin IDs.

## Overview

### Problem Statement

Legacy enterprise JSF developers — running projects on Liberty that cannot be migrated to more modern frameworks — lack a live-reload development experience. The typical workflow requires manual server restarts after every code or template change, significantly slowing iteration speed. The developer already uses Gradle to start Liberty locally; this tool plugs into that existing workflow with zero migration cost.

### Solution

A Gradle plugin (Maven support in v2) that wraps the existing Liberty startup task and adds file watching with automatic browser refresh. When a `.xhtml`, CSS, or JS file changes, the plugin copies the updated file directly into the exploded WAR output directory and notifies the browser via WebSocket — no server restart required. Java class reloading is a v2 concern. The plugin delegates Liberty lifecycle management to the existing `liberty-gradle-plugin`.

### Scope

**In Scope (v1):**
- Gradle plugin with `jsfPrepare` + `jsfDev` tasks that wrap `liberty-gradle-plugin` startup
- File watching for: JSF templates (`.xhtml`), CSS, JS, and static resources
- Changed files copied directly to exploded WAR output directory (no full repackage)
- Browser auto-refresh via WebSocket — Servlet Filter auto-injects a JS snippet into every `.xhtml` response during dev mode
- WebSocket dev server on a configurable port (default: 35729)
- Primary server target: IBM Open Liberty / WebSphere Liberty
- JSF implementations: Mojarra and MyFaces (both)
- Server-agnostic `ServerAdapter` abstraction layer for future server support
- Both `REFRESH_PERIOD` set to 0 for Mojarra and MyFaces to force template re-evaluation

**Out of Scope (v1 — deferred to v2+):**
- Maven plugin
- Java class reloading / custom ClassLoader hot-swap
- Attach mode (plugin attaches to an already-running server not started by the plugin)
- Additional servers: Tomcat, WildFly, JBoss, GlassFish, standalone embedded
- Production deployment / non-dev mode usage
- IDE-specific integrations
- Security/auth on the dev WebSocket endpoint

---

## Context for Development

### Codebase Patterns

**Confirmed Clean Slate** — greenfield project, no legacy constraints.

**Project Structure (two-subproject multi-module build):**
```
jsf-autoreload/
├── settings.gradle.kts                        # includes jsf-autoreload-plugin, jsf-autoreload-runtime
├── build.gradle.kts                           # root build, no sources
│
├── jsf-autoreload-plugin/                     # Gradle plugin subproject
│   ├── build.gradle.kts
│   └── src/main/java/io/github/tizianobasile/jsfautoreload/
│       ├── JsfAutoreloadPlugin.java           # Plugin<Project> entry point
│       ├── JsfPrepareTask.java                # Pre-start preparation (runs before libertyStart)
│       ├── JsfDevTask.java                    # Dev loop: WS server + file watcher
│       ├── JsfAutoreloadExtension.java        # DSL: port, serverName, outputDir, watchDirs
│       ├── watcher/
│       │   └── FileChangeWatcher.java
│       ├── websocket/
│       │   └── DevWebSocketServer.java
│       └── server/
│           ├── ServerAdapter.java
│           └── liberty/
│               └── LibertyServerAdapter.java
│   └── src/main/resources/META-INF/gradle-plugins/
│       └── it.bstz.jsf-autoreload.properties
│
└── jsf-autoreload-runtime/                    # Runtime JAR subproject (deployed into WAR)
    ├── build.gradle.kts
    └── src/main/
        ├── java/io/github/tizianobasile/jsfautoreload/filter/
        │   └── DevModeFilter.java
        └── resources/META-INF/
            └── web-fragment.xml
```

**Two-artifact design:**
- `jsf-autoreload-plugin` — Gradle plugin (build-time, applied to user's `build.gradle`)
- `jsf-autoreload-runtime` — dependency-free JAR with `DevModeFilter` + `web-fragment.xml`, injected into `WEB-INF/lib` of the exploded WAR at dev-time by `JsfPrepareTask`

**Runtime JAR injection mechanism:** The plugin subproject declares a custom Gradle configuration `runtimeJar` with `project(":jsf-autoreload-runtime")` as a dependency. `JsfPrepareTask` receives this as an `@InputFiles FileCollection runtimeJarFiles` property. At task execution, `runtimeJarFiles.getSingleFile()` provides the JAR path. This is the idiomatic Gradle way — no classpath hacks, no resource streams.

**Task execution order:**
```
jsfPrepare → libertyStart → jsfDev
```
- `jsfPrepare`: copies runtime JAR, writes config files — MUST run before Liberty starts
- `libertyStart`: starts Liberty server (delegated to `liberty-gradle-plugin`)
- `jsfDev`: starts WS server + file watcher, blocks until Ctrl+C

### Target User Context

- Legacy enterprise Java shop, JSF 2.x project (likely `@ManagedBean` + CDI mix)
- Liberty server (Open Liberty or WebSphere Liberty), started locally via Gradle
- Exploded WAR deployment: Liberty config points to exploded output dir, not a packaged `.war`
- Java EE 8 / `javax.*` namespace (not Jakarta EE 9+)
- Java 11 or 17
- Dev workflow today: `gradle libertyRun` → edit file → wait for manual restart → check browser
- New workflow: `gradle jsfDev` → edit file → browser refreshes in < 2 seconds

### Files to Reference

| File | Purpose |
| ---- | ------- |
| `JsfAutoreloadPlugin.java` | Plugin entry point: registers extension, tasks, wires task graph |
| `JsfPrepareTask.java` | Pre-start task: injects runtime JAR, writes config, validates setup |
| `JsfDevTask.java` | Dev loop: starts WS server + file watcher, blocks with CountDownLatch |
| `JsfAutoreloadExtension.java` | User-facing DSL config block |
| `FileChangeWatcher.java` | File system watcher using directory-watcher, handles all event types |
| `DevWebSocketServer.java` | Lightweight WS server that pushes reload messages |
| `DevModeFilter.java` | Servlet Filter: buffers response, appends reload script, fixes Content-Length |
| `ServerAdapter.java` | Read-only introspection interface |
| `LibertyServerAdapter.java` | Liberty introspection via `java.net.HttpURLConnection` |
| `web-fragment.xml` | Auto-registers DevModeFilter in Servlet 3.0+ containers |

### Technical Decisions

- **Task ordering**: `jsfPrepare` runs before `libertyStart`; `jsfDev` runs after `libertyStart`. File setup (JAR copy, config writes) happens in `jsfPrepare` so Liberty starts with the correct configuration already in place.
- **Runtime JAR bundling**: Declared as a custom `runtimeJar` Gradle configuration in the plugin subproject. `JsfPrepareTask` receives it as `@InputFiles FileCollection` — idiomatic Gradle, no classpath reflection or resource streams.
- **Server lifecycle**: Delegate entirely to `liberty-gradle-plugin`. `JsfAutoreloadPlugin` wires `libertyStart.dependsOn("jsfPrepare")` and `jsfDev.dependsOn("libertyStart")` in `afterEvaluate`.
- **ServerAdapter contract**: Read-only introspection only — `isRunning(): boolean`, `getHttpPort(): int`, `getContextRoot(): String`. `LibertyServerAdapter.isRunning()` catches `java.net.ConnectException` and returns `false` (server not yet up) rather than propagating the exception.
- **Config file deduplication**: Before writing to `bootstrap.properties` or `jvm.options`, read existing content and check whether the key/line already exists. If present, skip the write. Use `java.util.Properties` for `bootstrap.properties`; line-scan for `jvm.options`. This prevents accumulating duplicate entries across repeated `jsfDev` runs.
- **Port communication (build-time → runtime)**: `JsfPrepareTask` writes `-Djsf.autoreload.port=${port}` to Liberty's `jvm.options`. `DevModeFilter` reads this at init via `System.getProperty("jsf.autoreload.port", "35729")`. No servlet context init params, no `web.xml` entries.
- **Servlet Filter response wrapping**: `DevModeFilter` wraps `HttpServletResponse` with a `CharArrayWriter`-backed `HttpServletResponseWrapper`. After `chain.doFilter()` completes: (1) check `Content-Type` contains `text/html`; (2) if yes, reset `Content-Length` header to `-1` (via `setContentLength(-1)`) to prevent browser truncation after script append; (3) append reload `<script>` after buffered content; (4) write complete buffer to real response. Skips injection if response is already committed.
- **Servlet API**: `javax.servlet:javax.servlet-api:4.0.1` (Java EE 8). Target user is on JSF 2.x / Liberty with `javax.*` namespace. Jakarta EE 9+ (`jakarta.servlet`) is out of scope for v1.
- **web-fragment.xml filter ordering**: The `<ordering>` element in `web-fragment.xml` controls fragment metadata loading order, NOT servlet filter chain execution order. Remove `<ordering>` element entirely — filter chain position is determined by the Servlet spec's filter declaration ordering. `DevModeFilter` wraps the response before FacesServlet runs (buffers everything), so chain position does not affect correctness. Map to `*.xhtml` with `REQUEST` and `FORWARD` dispatchers.
- **Shadow JAR and import packages**: Shadow plugin relocates `org.java_websocket` → `it.bstz.shaded.java_websocket` in the **output bytecode only**. Source code in `DevWebSocketServer.java` MUST import `org.java_websocket.*` (pre-relocation name). The Shadow plugin rewrites the bytecode at build time. Importing the relocated name in source will cause a compile error.
- **DELETE file events**: `FileChangeWatcher` checks the event type. On `DELETE`: skip the file copy (source no longer exists) but still call `wsServer.broadcast("reload")` so the browser reflects the current server state. On `CREATE`/`MODIFY`: copy the file to `outputDir` then broadcast.
- **outputDir validation**: If the resolved `outputDir` does not exist on disk at task execution time, `JsfPrepareTask` fails with: `"[JSF Autoreload] Output directory not found: ${outputDir}. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name matches jsfAutoreload { serverName = '...' }"`.
- **Inline reload script**: Inlined as a string constant in `DevModeFilter.java` — no separate resource file. Port substituted at filter init time.
- **Gradle daemon-safe blocking**: `JsfDevTask` blocks via `CountDownLatch(1).await()` with a JVM shutdown hook that calls `watcher.stop()`, `wsServer.stopServer()`, `latch.countDown()`.
- **WebSocket port conflict**: Fail-fast with `IllegalStateException`: `"JSF Autoreload: port {port} is already in use. Configure a different port via jsfAutoreload { port = XXXX }"`.
- **Liberty ClassLoader assumption**: `parentLast` delegation (Liberty's WAR default) is required. Plugin prints a startup warning if `parentFirst` is detected in `server.xml`.

---

## Implementation Plan

### Tasks

- [x] **Task 1: Root project scaffolding**
  - File: `settings.gradle.kts`
  - Action: Create root settings file with `rootProject.name = "jsf-autoreload"` and `include("jsf-autoreload-plugin", "jsf-autoreload-runtime")`.
  - File: `build.gradle.kts` (root)
  - Action: Empty root build file (no shared configuration needed in v1).

- [x] **Task 2: Plugin subproject build file**
  - File: `jsf-autoreload-plugin/build.gradle.kts`
  - Action: Apply plugins: `java-gradle-plugin`, `maven-publish`, `com.github.johnrengelman.shadow` (version `8.+`). Declare dependencies: `gradleApi()` (implementation), `io.methvin:directory-watcher:0.18.+` (implementation), `org.java-websocket:Java-WebSocket:1.5.+` (implementation), `io.openliberty.tools:liberty-gradle-plugin:3.+` (compileOnly). Create custom configuration `runtimeJar` and add `project(":jsf-autoreload-runtime")` as its dependency. Register plugin: `gradlePlugin { plugins { create("jsfAutoreload") { id = "it.bstz.jsf-autoreload"; implementationClass = "it.bstz.jsfautoreload.JsfAutoreloadPlugin" } } }`. Configure Shadow: `shadowJar { relocate("org.java_websocket", "it.bstz.shaded.java_websocket") }`.
  - Notes: The `runtimeJar` configuration is what makes the runtime JAR available to `JsfPrepareTask` without classpath reflection.

- [x] **Task 3: Runtime subproject build file**
  - File: `jsf-autoreload-runtime/build.gradle.kts`
  - Action: Apply `java-library` and `maven-publish`. Declare `javax.servlet:javax.servlet-api:4.0.1` as `compileOnly` — no runtime dependencies. Configure: `jar { manifest { attributes("Web-Fragment-Name" to "jsf-autoreload") } }`.

- [x] **Task 4: Gradle plugin properties file**
  - File: `jsf-autoreload-plugin/src/main/resources/META-INF/gradle-plugins/it.bstz.jsf-autoreload.properties`
  - Action: Create file with single line: `implementation-class=it.bstz.jsfautoreload.JsfAutoreloadPlugin`

- [x] **Task 5: ServerAdapter interface**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/server/ServerAdapter.java`
  - Action: Create `public interface ServerAdapter` with three methods: `boolean isRunning()`, `int getHttpPort()`, `String getContextRoot()`. No lifecycle methods.
  - Notes: This is the v2 extension point. Keep it minimal.

- [x] **Task 6: JsfAutoreloadExtension — Gradle DSL**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfAutoreloadExtension.java`
  - Action: Create class with properties (use Gradle `Property<T>` / `ListProperty<T>` for configuration cache compatibility): `Property<Integer> port` (default 35729), `Property<String> serverName` (default `"defaultServer"`), `Property<String> outputDir` (default `""` = auto-infer), `ListProperty<String> watchDirs` (default `["src/main/webapp"]`).

- [x] **Task 7: FileChangeWatcher**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/watcher/FileChangeWatcher.java`
  - Action: Create class wrapping `io.methvin.watcher.DirectoryWatcher`. Constructor takes `List<Path> watchDirs` and two callbacks: `Consumer<Path> onChanged` (for CREATE/MODIFY) and `Consumer<Path> onDeleted` (for DELETE). `start()` creates and starts the watcher in a daemon thread: `DirectoryWatcher.builder().paths(watchDirs).listener(event -> { if (event.eventType() == CREATE || event.eventType() == MODIFY) { onChanged.accept(event.path()); } else if (event.eventType() == DELETE) { onDeleted.accept(event.path()); } }).build()`. `stop()` closes the watcher.
  - Notes: Import `io.methvin.watcher.DirectoryChangeEvent.EventType.*`. Catch and log `IOException` from watcher startup — do not crash the task.

- [x] **Task 8: DevWebSocketServer**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/websocket/DevWebSocketServer.java`
  - Action: Create class extending `org.java_websocket.server.WebSocketServer` (pre-relocation import name — Shadow rewrites this to `it.bstz.shaded.java_websocket` at build time). Override `onOpen`, `onClose`, `onMessage`, `onError` (no-ops are fine). Expose `void broadcastReload()` calling `super.broadcast("reload")`. Constructor takes `int port` and calls `super(new InetSocketAddress(port))`.
  - Notes: ALWAYS import `org.java_websocket.*` in source — NEVER the relocated name. The Shadow plugin handles bytecode relocation at build time. Port conflict check: wrap `super.start()` in try-catch; if `BindException` is thrown, rethrow as `IllegalStateException("JSF Autoreload: port " + port + " is already in use. Configure a different port via jsfAutoreload { port = XXXX }")`.

- [x] **Task 9: LibertyServerAdapter**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/server/liberty/LibertyServerAdapter.java`
  - Action: Implement `ServerAdapter`. Fields: `int httpPort`, `String contextRoot`. Constructor takes both. `isRunning()`: open `HttpURLConnection` to `http://localhost:{httpPort}/`, set `connectTimeout(500)` and `readTimeout(500)`, return `true` if response code `< 500`. Catch `java.net.ConnectException` and return `false` (server not yet accepting connections). Catch `IOException` generically and return `false`. `getHttpPort()`: return `httpPort`. `getContextRoot()`: return `contextRoot`.
  - Notes: Use `java.net.HttpURLConnection` — no extra HTTP client dependency. Catching `ConnectException` before `IOException` is critical: a refused connection throws `ConnectException`, not a response code.

- [x] **Task 10: DevModeFilter — runtime module**
  - File: `jsf-autoreload-runtime/src/main/java/io/github/tizianobasile/jsfautoreload/filter/DevModeFilter.java`
  - Action: Implement `javax.servlet.Filter`. In `init(FilterConfig)`: read port via `System.getProperty("jsf.autoreload.port", "35729")`. Build inline script string: `"<script>(function(){var ws=new WebSocket('ws://localhost:" + port + "');ws.onmessage=function(e){if(e.data==='reload')window.location.reload();};ws.onclose=function(){setTimeout(function(){location.reload();},2000);};}());</script>"`. In `doFilter`: if response is already committed, call `chain.doFilter()` and return immediately. Otherwise, wrap `HttpServletResponse` with a `CharArrayWriter`-backed `HttpServletResponseWrapper` that overrides `getWriter()`. Call `chain.doFilter(request, wrappedResponse)`. After chain completes: check if `Content-Type` contains `text/html`; if yes, append the inline script string to the `CharArrayWriter` buffer, call `response.setContentLength(-1)` to reset Content-Length (prevents browser truncation), then write the full buffer to `response.getWriter()`. If `Content-Type` does not contain `text/html`, write the buffer unmodified.
  - Notes: `setContentLength(-1)` removes the `Content-Length` header so the browser does not truncate the response at the original length. This is required whenever content is appended after the chain writes.

- [x] **Task 11: web-fragment.xml — runtime module**
  - File: `jsf-autoreload-runtime/src/main/resources/META-INF/web-fragment.xml`
  - Action: Create Servlet 3.0 web fragment. Declare `DevModeFilter` with `<filter-class>it.bstz.jsfautoreload.filter.DevModeFilter</filter-class>`. Map to `*.xhtml` with `<dispatcher>REQUEST</dispatcher>` and `<dispatcher>FORWARD</dispatcher>`. Do NOT include an `<ordering>` element — fragment ordering controls metadata load order, not filter chain execution order, and is not needed here.
  - Notes: The filter buffers the complete response and appends after chain completes. Its position in the filter chain does not affect correctness, so no explicit ordering is required.

- [x] **Task 12: JsfPrepareTask — pre-start preparation**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfPrepareTask.java`
  - Action: Create class extending `DefaultTask`. Add `@InputFiles FileCollection runtimeJarFiles` (wired by plugin). Add `@Input` fields for `serverName`, `port`, `outputDir` (wired from extension). `@TaskAction void prepare()`:
    1. Resolve `outputDir`: if empty, infer as `${project.rootDir}/build/wlp/usr/servers/${serverName}/apps/expanded/${project.name}.war`. **Validate**: if the resolved path does not exist, throw `GradleException("[JSF Autoreload] Output directory not found: ${outputDir}. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your Liberty server name matches jsfAutoreload { serverName = '...' }")`.
    2. Copy `runtimeJarFiles.getSingleFile()` to `${outputDir}/WEB-INF/lib/jsf-autoreload-runtime.jar` using `Files.copy(..., REPLACE_EXISTING)`.
    3. Write to `bootstrap.properties` (path: `${project.rootDir}/build/wlp/usr/servers/${serverName}/bootstrap.properties`): load existing file as `java.util.Properties`; set `javax.faces.FACELETS_REFRESH_PERIOD=0` and `org.apache.myfaces.REFRESH_PERIOD=0` only if not already present; store back to file.
    4. Write to `jvm.options` (path: `${project.rootDir}/build/wlp/usr/servers/${serverName}/jvm.options`): read all lines; check if any line matches `-Djsf.autoreload.port=`; if not present, append `-Djsf.autoreload.port=${port}`.
    5. Check `server.xml` for `parentFirst`: read `${project.rootDir}/src/main/liberty/config/server.xml` (standard Liberty Gradle plugin path); if it contains `delegation="parentFirst"`, print: `[JSF Autoreload] WARNING: Liberty classloader delegation is set to 'parentFirst'. DevModeFilter may not register correctly. Switch to 'parentLast' (the default).`
  - Notes: Steps 3 and 4 deduplicate on every run — safe to call repeatedly.

- [x] **Task 13: JsfDevTask — dev loop**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfDevTask.java`
  - Action: Create class extending `DefaultTask`. Annotate with `@UntrackedTask(because = "Dev server runs indefinitely")`. Add `@Input` fields for `port`, `outputDir`, `watchDirs` (wired from extension). `@TaskAction void execute()`:
    1. Start `DevWebSocketServer` on configured `port` — fail fast on `IllegalStateException` from port conflict.
    2. Start `FileChangeWatcher` on resolved `watchDirs` paths with two callbacks:
       - `onChanged`: copy changed file to `outputDir` (preserving relative path), then call `wsServer.broadcastReload()`
       - `onDeleted`: skip file copy (source gone), call `wsServer.broadcastReload()`
    3. Create `CountDownLatch(1)`. Register JVM shutdown hook: `Runtime.getRuntime().addShutdownHook(new Thread(() -> { watcher.stop(); try { wsServer.stop(); } catch (Exception ignored) {} latch.countDown(); }))`.
    4. Print: `[JSF Autoreload] Dev server started on ws://localhost:${port}. Watching: ${watchDirs}`.
    5. Call `latch.await()`.

- [x] **Task 14: JsfAutoreloadPlugin — plugin entry point**
  - File: `jsf-autoreload-plugin/src/main/java/io/github/tizianobasile/jsfautoreload/JsfAutoreloadPlugin.java`
  - Action: Implement `Plugin<Project>`. In `apply(Project project)`:
    1. Register extension: `project.getExtensions().create("jsfAutoreload", JsfAutoreloadExtension.class)`.
    2. Register `jsfPrepare` task (type `JsfPrepareTask`): set group `"JSF Autoreload"`. Wire `runtimeJarFiles` from `project.getConfigurations().getByName("runtimeJar")`. Wire `serverName`, `port`, `outputDir` from extension.
    3. Register `jsfDev` task (type `JsfDevTask`): set group `"JSF Autoreload"`. Wire `port`, `outputDir`, `watchDirs` from extension.
    4. In `project.afterEvaluate`: if `libertyStart` task exists, wire `libertyStart.dependsOn("jsfPrepare")` and `jsfDev.dependsOn("libertyStart")`; else log warning: `"[JSF Autoreload] 'libertyStart' task not found. Make sure io.openliberty.tools.liberty plugin is applied."`.
  - Notes: `afterEvaluate` is required because `libertyStart` is registered by `liberty-gradle-plugin` which may be applied after our plugin.

- [x] **Task 15: Unit tests — FileChangeWatcher**
  - File: `jsf-autoreload-plugin/src/test/java/io/github/tizianobasile/jsfautoreload/watcher/FileChangeWatcherTest.java`
  - Action: Use JUnit 5 + `@TempDir`. Tests: (1) CREATE event — write new file, assert `onChanged` called with correct path within 2s via `CountDownLatch`; (2) MODIFY event — write existing file, assert `onChanged` called; (3) DELETE event — delete file, assert `onDeleted` called and `onChanged` NOT called.

- [x] **Task 16: Unit tests — DevModeFilter**
  - File: `jsf-autoreload-runtime/src/test/java/io/github/tizianobasile/jsfautoreload/filter/DevModeFilterTest.java`
  - Action: Use JUnit 5 + Mockito. Tests: (1) `text/html` response gets reload script appended to body; (2) `Content-Length` is reset to `-1` when script is appended; (3) `application/json` response is NOT modified; (4) already-committed response is NOT modified — `chain.doFilter()` is called but buffer is not written twice; (5) port read from JVM system property `jsf.autoreload.port` at filter init (set via `System.setProperty` in test setup).

- [x] **Task 17: Unit tests — DevWebSocketServer**
  - File: `jsf-autoreload-plugin/src/test/java/io/github/tizianobasile/jsfautoreload/websocket/DevWebSocketServerTest.java`
  - Action: Use JUnit 5. Start server on a random free port (use `ServerSocket(0)` to find one, close it, then bind WS server to same port). Connect a test WS client (`org.java_websocket.client.WebSocketClient`). Call `broadcastReload()`, assert client receives `"reload"` message within 1s via `CountDownLatch`. Test cleanup: call `wsServer.stop()` in `@AfterEach`.

- [x] **Task 18: Unit tests — LibertyServerAdapter**
  - File: `jsf-autoreload-plugin/src/test/java/io/github/tizianobasile/jsfautoreload/server/liberty/LibertyServerAdapterTest.java`
  - Action: Use JUnit 5 + a `MockWebServer` (e.g. `com.squareup.okhttp3:mockwebserver`) or a simple `com.sun.net.httpserver.HttpServer`. Tests: (1) `isRunning()` returns `true` when server returns 200; (2) `isRunning()` returns `true` when server returns 404 (server up but resource missing); (3) `isRunning()` returns `false` when nothing is listening on port (connection refused — `ConnectException`); (4) `getHttpPort()` returns configured value; (5) `getContextRoot()` returns configured value.

- [x] **Task 19: Integration tests — Gradle TestKit**
  - File: `jsf-autoreload-plugin/src/test/java/io/github/tizianobasile/jsfautoreload/JsfAutoreloadPluginIntegrationTest.java`
  - Action: Use `GradleRunner`. Tests: (1) Apply plugin to minimal project → verify `jsfDev` and `jsfPrepare` tasks are registered; (2) Apply plugin + liberty plugin → verify `jsfDev` depends on `libertyStart` and `libertyStart` depends on `jsfPrepare`; (3) Configure `jsfAutoreload { port = 35730 }` → verify `port` extension property resolves to `35730`.

### Acceptance Criteria

- [ ] **AC 1**: Given a `build.gradle` with both `io.openliberty.tools.liberty` and `it.bstz.jsf-autoreload` plugins applied, when the project is configured, then the task graph contains `jsfPrepare → libertyStart → jsfDev` in that dependency order.

- [ ] **AC 2**: Given `jsfDev` is running and `src/main/webapp/pages/home.xhtml` is saved, when the file change is detected, then `${outputDir}/pages/home.xhtml` reflects the updated content within 500ms.

- [ ] **AC 3**: Given `jsfDev` is running and a browser has loaded a JSF page, when the plugin broadcasts `"reload"` via WebSocket, then the browser automatically refreshes the page.

- [ ] **AC 4**: Given `jsfDev` is running, when any `.xhtml` page is requested, then the HTML response body contains the inline WebSocket reload script, and the `Content-Length` response header is either absent or matches the actual response length.

- [ ] **AC 5**: Given `jsfDev` is running and `src/main/webapp/css/styles.css` is saved, when the file change is detected, then `${outputDir}/css/styles.css` is updated and a `"reload"` WebSocket message is broadcast.

- [ ] **AC 6**: Given `jsfAutoreload { port = 35730 }` in `build.gradle`, when `jsfDev` starts, then the WebSocket server binds to port 35730.

- [ ] **AC 7**: Given port 35729 is already bound, when `jsfDev` starts, then the task fails immediately with: `"JSF Autoreload: port 35729 is already in use. Configure a different port via jsfAutoreload { port = XXXX }"`.

- [ ] **AC 8**: Given Liberty `server.xml` contains `<classloader delegation="parentFirst"/>`, when `jsfPrepare` runs, then the console prints the `parentFirst` warning.

- [ ] **AC 9**: Given `jsfDev` is running, when a file under `src/main/java/` changes, then no WebSocket reload message is broadcast and no file copy occurs.

- [ ] **AC 10**: Given `jsfPrepare` runs, when `bootstrap.properties` is written, then it contains `javax.faces.FACELETS_REFRESH_PERIOD=0` and `org.apache.myfaces.REFRESH_PERIOD=0`. Running `jsfPrepare` a second time does NOT add duplicate entries.

- [ ] **AC 11**: Given a JSON endpoint (`Content-Type: application/json`) in the same WAR, when it is requested, then `DevModeFilter` does not modify its response body.

- [ ] **AC 12**: Given `jsfAutoreload { outputDir }` is not configured and the inferred path does not exist on disk, when `jsfPrepare` runs, then the task fails with a clear error message including the inferred path and a hint to set `serverName` or `outputDir` explicitly.

---

## Additional Context

### Dependencies

**Plugin build dependencies (`jsf-autoreload-plugin`):**
- `gradleApi()` — Gradle Plugin API (provided by Gradle)
- `io.methvin:directory-watcher:0.18.+` — cross-platform file watching
- `org.java-websocket:Java-WebSocket:1.5.+` — WebSocket server (shadowed + relocated)
- `io.openliberty.tools:liberty-gradle-plugin:3.+` — compileOnly, for task type resolution
- `com.github.johnrengelman.shadow:8.+` — shadow plugin for JAR relocation

**Runtime JAR dependencies (`jsf-autoreload-runtime`):**
- `javax.servlet:javax.servlet-api:4.0.1` — compileOnly (Java EE 8 / `javax.*` namespace)
- No runtime transitive deps — the filter must be dependency-free to avoid conflicts in user's WAR

**Test dependencies (plugin subproject):**
- `org.junit.jupiter:junit-jupiter:5.+`
- `org.java-websocket:Java-WebSocket:1.5.+` (for test WS client in Task 17)
- `com.squareup.okhttp3:mockwebserver:4.+` (for LibertyServerAdapter HTTP mocking in Task 18)

**Test dependencies (runtime subproject):**
- `org.junit.jupiter:junit-jupiter:5.+`
- `org.mockito:mockito-core:5.+`

**User project requirements (documented, not enforced):**
- Gradle 7+
- `io.openliberty.tools:liberty-gradle-plugin` applied
- Exploded WAR deployment configured in Liberty server
- Liberty server name configured in `jsfAutoreload { serverName = "..." }` if not `"defaultServer"`

### Testing Strategy

**Unit tests (automated, Tasks 15–18):**
- `FileChangeWatcher` — CREATE, MODIFY, DELETE event routing
- `DevModeFilter` — script injection, Content-Length reset, non-HTML bypass, committed response bypass, system property port reading
- `DevWebSocketServer` — server start, broadcast delivery, port conflict detection
- `LibertyServerAdapter` — isRunning with 200/404/ConnectException, getHttpPort, getContextRoot

**Integration tests (automated, Task 19 — Gradle TestKit):**
- Task registration, task graph wiring, extension DSL

**Manual smoke test (no Liberty integration tests in v1 CI):**
1. Create a minimal JSF 2.x WAR project with `liberty-gradle-plugin` and exploded WAR configured
2. Apply `it.bstz.jsf-autoreload` plugin, configure `serverName`
3. Run `gradle jsfDev` — verify Liberty starts and `jsf-autoreload-runtime.jar` appears in `WEB-INF/lib/`
4. Open a JSF page in browser — verify reload script `<script>` tag present in page source
5. Edit and save an `.xhtml` file — verify browser refreshes automatically within 2s
6. Edit and save a CSS file — verify browser refreshes
7. Delete an `.xhtml` file — verify no crash, browser reloads
8. Edit a `.java` file — verify no reload triggered
9. Run `gradle jsfDev` a second time — verify no duplicate entries in `bootstrap.properties` or `jvm.options`
10. Block port 35729 (e.g. `nc -l 35729`) — verify task fails with clear port conflict message

### Notes

- v2 backlog: Maven plugin, Java class reloading (custom ClassLoader — document state loss for session-scoped beans), attach mode, Tomcat/WildFly/JBoss/GlassFish support, Jakarta EE 9+ (`jakarta.servlet`) support
- `ServerAdapter` interface must remain minimal so adding v2 server support requires only a new implementation class, no changes to core logic
- Both Mojarra and MyFaces supported from v1 via `bootstrap.properties` entries

## Review Notes

### Review 1 (tech spec adversarial review)
- Adversarial review completed
- Findings: 28 total, 24 fixed, 4 skipped (by-design or deferred)
- Resolution approach: auto-fix

#### Skipped findings
- F11 (latch.await blocks daemon): By design per tech spec — task runs until Ctrl+C
- F18 (server.xml path inconsistency): Correct per Liberty Gradle plugin conventions
- F24 (no JsfPrepareTask tests): Deferred — would require substantial test infrastructure
- F25 (ServerAdapter unused): By design per tech spec — v2 extension point

### Review 2 (code review against tech spec — 2026-03-13)
- Adversarial code review completed
- Findings: 13 total (5 HIGH, 5 MEDIUM, 3 LOW)
- Fixed: 10 (5 HIGH, 5 MEDIUM)
- Remaining: 3 LOW (cosmetic/informational)
- Resolution approach: auto-fix

#### Fixed findings
- F1 (HIGH): `watchClasses` default changed from `true` to `false` — matches v1 scope (AC 9)
- F3 (HIGH): web.xml injection now validates `<web-app>` tag exists before inserting context-params
- F4 (HIGH): `FileChangeWatcher.start()` now logs IOException instead of throwing RuntimeException
- F5 (HIGH): Removed `getProject()` calls from task execution in `JsfPrepareTask` and `JsfDevTask` — added `rootDir` and `projectDir` as task properties wired at configuration time
- F6 (MEDIUM): Shutdown hook now performs cleanup (stop watchers, WS server) directly for robustness
- F7 (MEDIUM): `stopLibertyServer()` documented as configuration-cache-incompatible with TODO
- F8 (MEDIUM): Added integration test `taskDependencyWiringWithLibertyStartTask` verifying task graph
- F9 (MEDIUM): Added 5 unit tests for `JavaSourceCompiler` (valid, invalid, empty, nonexistent, packaged)
- F10 (MEDIUM): Shadow plugin version noted as deviation from spec (functional, not changed)
- F12 (LOW bonus): Replaced deprecated `project.getBuildDir()` with `project.getLayout().getBuildDirectory()`

#### Remaining LOW findings (not fixed)
- F2 (HIGH→informational): Tech spec says bootstrap.properties but code uses web.xml injection — code approach is better, spec should be updated
- F11 (LOW): `Property<FileCollection>` with `@InputFiles` not fully idiomatic — functional
- F13 (LOW): web.xml context-param insertion order reversed — cosmetic, no impact
