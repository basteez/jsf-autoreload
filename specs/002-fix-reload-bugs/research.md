# Research: Fix Reload Bugs

**Feature**: 002-fix-reload-bugs | **Date**: 2026-04-16

## Research Task 1: Deferred ScriptInjector Registration (JSF Application Timing)

**Question**: How to register `ScriptInjector` when the JSF `Application` is not available during `ServletContainerInitializer.onStartup()`?

**Decision**: Use `ServletContext` attributes to pass configuration from servlet initialization to JSF initialization, then register the script injector when JSF fires `PostConstructApplicationEvent` or on the first JSF request via a one-shot approach through the bridge.

**Rationale**: The JSF `Application` object is created by the JSF runtime (Mojarra/MyFaces) during its own initialization, which runs after `ServletContainerInitializer`. The standard patterns for deferred registration in JSF libraries are:

1. **`faces-config.xml` `<system-event-listener>`** — Declare a listener for `PostConstructApplicationEvent`. This is the standard JSF approach, but requires a concrete class with namespace-specific imports (javax.faces or jakarta.faces), breaking the bridge abstraction.

2. **`ServletContext` attribute + bridge method** — Store the SSE endpoint path as a `ServletContext` attribute during `bootstrap()`. Add a new method to `JsfBridge` (e.g., `registerDeferredScriptInjector(servletContext, sseEndpointPath)`) that:
   - In `JavaxJsfBridge`: registers a `javax.faces.event.SystemEventListener` for `javax.faces.event.PostConstructApplicationEvent` via the `faces-config.xml` or a factory wrapper
   - In `JakartaJsfBridge`: equivalent with `jakarta.faces.*`

3. **One-shot PhaseListener** — Register a `PhaseListener` via `faces-config.xml` that on the first `RENDER_RESPONSE` phase, retrieves the `Application`, calls `ScriptInjector.register()`, then removes itself. This is safe but adds per-request overhead for the first request.

4. **ApplicationFactory decorator** — Wrap the JSF `ApplicationFactory` to intercept `getApplication()` and register the listener. Over-engineered for this use case.

**Selected approach**: Option 5 (evolved from Option 2) — **Programmatic `ServletContextListener` via bridge method**. Each bridge's `registerDeferredScriptInjector()` calls `servletContext.addListener(new ServletContextListener() {...})`. The Servlet spec guarantees that programmatic listeners fire AFTER declared listeners, so Mojarra/MyFaces `ConfigureListener` runs first (creating the JSF `Application`), then our listener retrieves it via `FactoryFinder` and calls `ScriptInjector.register()`.

**Why Option 2 was abandoned during implementation**: Declaring `PostConstructApplicationEvent` listeners in `faces-config.xml` requires namespace-specific `<system-event-class>` elements. Mojarra 2.3.9 treats the inability to load the jakarta listener class as a **fatal `ConfigurationException`** — it does NOT gracefully skip unresolvable classes. This prevents the single-JAR, dual-namespace architecture from working with faces-config.xml declarations.

**Alternatives considered**:
- Option 1 rejected: requires two separate faces-config.xml files or conditional class loading, adding complexity
- Option 2 rejected during implementation: faces-config.xml with both javax/jakarta listeners crashes Mojarra 2.3.9 (`ConfigurationException: Unable to create instance`)
- Option 3 rejected: PhaseListener runs on every request until first hit, adds overhead; also requires namespace-specific class
- Option 4 rejected: over-engineered; ApplicationFactory wrapping is fragile across implementations

## Research Task 2: SSE Heartbeat Best Practices

**Question**: What is the correct SSE heartbeat implementation to prevent proxy/browser connection drops?

**Decision**: Send SSE comments (`:heartbeat\n\n`) to all active connections every 30 seconds (configurable via `PluginConfiguration`).

**Rationale**: The SSE specification (W3C) defines that lines starting with `:` are comments and MUST be ignored by the EventSource client. Sending periodic comments is the standard keepalive mechanism. Key considerations:

- **Proxy timeout**: Most reverse proxies (nginx, Apache, HAProxy) have idle connection timeouts of 60-120 seconds. A 30-second heartbeat stays well within this window.
- **Browser behavior**: Browsers' `EventSource` implementations do not drop connections proactively, but some WebSocket-aware infrastructure may. Comments prevent this.
- **Error handling**: If a comment write throws `IOException`, the connection is dead and should be removed immediately. This provides passive cleanup during idle periods.
- **Thread safety**: `CopyOnWriteArraySet` iteration is snapshot-based, so concurrent modification during heartbeat is safe. Failed writes should call `remove()` after iteration.

**Alternatives considered**:
- Named SSE events (`event: heartbeat\ndata:\n\n`): rejected because the client script would need to explicitly ignore this event type. Comments are invisible to the client by spec.
- WebSocket ping/pong: not applicable — SSE is unidirectional.

## Research Task 3: Proactive Connection Cleanup via AsyncListener

**Question**: How to detect browser disconnections immediately without waiting for a failed write?

**Decision**: Register an `AsyncListener` on each connection's `AsyncContext` to detect `onComplete`, `onTimeout`, and `onError` events from the servlet container.

**Rationale**: The Servlet 3.0 `AsyncContext` API provides `addListener(AsyncListener)` which fires callbacks when:
- `onComplete`: async processing finishes normally
- `onTimeout`: the async context times out (default 30s, must be set to 0 or a large value for SSE)
- `onError`: an I/O error occurs (e.g., client disconnect detected by the container)

This is the standard mechanism for detecting client disconnections in async servlets. When any of these fire, the connection should be removed from `ConnectionManager`.

**Implementation detail**: The `AsyncContextWrapper` already wraps the `AsyncContext`. The `ServletBridge` interface needs a method to register an `AsyncListener` on the wrapped context. Each bridge implementation converts the callback to the appropriate namespace (`javax.servlet.AsyncListener` or `jakarta.servlet.AsyncListener`).

**Critical**: The `AsyncContext` timeout MUST be set to 0 (indefinite) for SSE connections. The default timeout (typically 30 seconds) would cause `onTimeout` to fire and close the connection. This should be set in `DefaultSseHandler.handleRequest()` via the `AsyncContextWrapper`.

**Alternatives considered**:
- Periodic connection health check (try-write): rejected as redundant with heartbeat. The heartbeat already writes to connections and catches failures.
- Container-specific disconnect detection: rejected; breaks modularity (Constitution I). `AsyncListener` is standard Servlet API.

## Research Task 4: SSE Endpoint Path and Context Path Handling

**Question**: Does the injected EventSource URL correctly resolve relative to the application context path?

**Decision**: The SSE endpoint path stored in `PluginConfiguration` (default: `/_jsf-autoreload/events`) is registered as a servlet mapping relative to the context. The client script must construct the full URL using the application's context path.

**Rationale**: The current `ScriptInjector.SCRIPT_TEMPLATE` injects:
```javascript
var es = new EventSource('%s');
```
where `%s` is the raw endpoint path (e.g., `/_jsf-autoreload/events`). If the application is deployed at a non-root context path (e.g., `/myapp`), the EventSource would try to connect to `/_jsf-autoreload/events` at the server root, missing the app entirely.

**Fix**: The script template should use a context-aware path. Since the script is injected into a JSF-rendered page, the `FacesContext`'s `ExternalContext.getRequestContextPath()` is available at render time. The `registerScriptInjector` bridge method should inject the context path into the script. Alternatively, use a relative path or read `document.querySelector('base')` on the client side.

**Selected approach**: Pass the context path to `ScriptInjector` at registration time and format it into the script template as `contextPath + endpointPath`.

**Alternatives considered**:
- Relative URL (e.g., `_jsf-autoreload/events` without leading slash): fragile; depends on the page's current URL
- Client-side detection via `window.location`: works but adds unnecessary complexity

## Research Task 5: PostAddToViewEvent Infinite Recursion (discovered during implementation)

**Question**: Why does `registerScriptInjector` cause a `StackOverflowError` when the script is injected?

**Decision**: Add a re-entry guard via `FacesContext.getAttributes().put("jsfautoreload.scriptInjected", true)` to prevent recursive event processing.

**Rationale**: The `PostAddToViewEvent` listener calls `UIViewRoot.addComponentResource()` to inject the `<script>` element. But adding a component to the view fires another `PostAddToViewEvent`, which re-enters the listener, causing infinite recursion. This was a latent bug — it never manifested before because `ScriptInjector` was never wired (Bug 1). The guard checks a request-scoped flag and returns early on re-entry, ensuring the script is injected exactly once per request.

## Research Task 6: Context Reload vs SSE Broadcast Ordering (discovered during implementation)

**Question**: Why don't browsers refresh after a Java class change even though the SSE broadcast fires?

**Decision**: Broadcast the SSE event before context reload AND add a client-side `onerror` handler that reloads the page when the SSE connection is lost.

**Rationale**: `StandardContext.reload()` is synchronous and destroys the servlet context, which tears down the SSE servlet and all active connections. Two issues were discovered:

1. **Original ordering** (context reload → broadcast): The broadcast sent to zero connections because the reload already destroyed them.
2. **Reversed ordering** (broadcast → context reload): The SSE event was written and flushed, but Tomcat's context reload closed the underlying TCP socket before the browser could process the data.

**Selected approach**: A two-layer solution:
- Server-side: broadcast before context reload (best-effort delivery)
- Client-side: the EventSource `onerror` handler detects connection loss (which always happens during context reload) and calls `location.reload()` after a 1-second delay. The `onerror` handler only fires after a successful connection was established (`onopen` sets a flag), preventing reload loops when the server is down.

## Research Task 7: Tomcat `reloadable` Interference (discovered during testing)

**Question**: Why does Tomcat context-reload before our plugin gets a chance to broadcast?

**Decision**: Applications using `jsf-autoreload` should set `StandardContext.setReloadable(false)` to disable Tomcat's built-in class-change monitoring.

**Rationale**: Tomcat's `reloadable=true` (the default for `addWebapp()`) runs a background thread that monitors `/WEB-INF/classes` for changes. When an IDE auto-compiles, Tomcat detects the change and triggers `StandardContext.reload()` independently of our plugin. This races with our `DirectoryWatcher` and destroys the SSE connections before our plugin can broadcast. With `reloadable=false`, only our plugin triggers context reloads, ensuring the broadcast-then-reload sequence works correctly.
