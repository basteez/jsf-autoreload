# Data Model: Fix Reload Bugs

**Feature**: 002-fix-reload-bugs | **Date**: 2026-04-16

This feature is a bug fix — no new entities are introduced. This document captures the state transitions and relationships relevant to the fixes.

## Entity: BrowserConnection (existing — no changes)

| Field | Type | Description |
|-------|------|-------------|
| connectionId | String (UUID) | Unique identifier per connection |
| asyncContext | AsyncContextWrapper | Wrapped servlet AsyncContext for writing SSE data |
| connectedSince | Instant | Timestamp when connection was established |
| lastNotifiedAt | Instant (nullable) | Timestamp of last successful write |
| state | ConnectionState (enum) | CONNECTED or DISCONNECTED |

### State Transitions

```
CONNECTED  ──(broadcast IOException)──→  DISCONNECTED  ──→  removed from ConnectionManager
CONNECTED  ──(AsyncListener.onError)──→  DISCONNECTED  ──→  removed from ConnectionManager
CONNECTED  ──(AsyncListener.onTimeout)──→  DISCONNECTED  ──→  removed from ConnectionManager
CONNECTED  ──(heartbeat IOException)──→  DISCONNECTED  ──→  removed from ConnectionManager
CONNECTED  ──(tab closed → container detects)──→  onComplete/onError fires  ──→  removed
```

**Change**: Currently, transitions to DISCONNECTED only happen during `broadcast()`. After the fix, three additional triggers exist: `AsyncListener` callbacks (immediate, container-detected) and heartbeat write failures (periodic, every 30s).

## Entity: DefaultSseHandler (existing — behavioral change)

### Heartbeat Behavior (before vs. after)

| Aspect | Before (bug) | After (fix) |
|--------|-------------|-------------|
| Heartbeat task body | Empty no-op | Iterates connections, sends `:heartbeat\n\n` comment |
| Dead connection detection | Only on `broadcast()` | On `broadcast()` + heartbeat write + `AsyncListener` |
| Connection timeout | Container default (30s) | Set to 0 (indefinite) via `AsyncContext.setTimeout(0)` |

## Entity: AutoreloadInitializer (existing — wiring change)

### Bootstrap Sequence (before vs. after)

| Step | Before (bug) | After (fix) |
|------|-------------|-------------|
| 1 | ConnectionManager | ConnectionManager |
| 2 | ReloadCoordinator | ReloadCoordinator |
| 3 | Debouncer → Coordinator | Debouncer → Coordinator |
| 4 | SSE Handler + servlet registration | SSE Handler + servlet registration |
| **4b** | *(missing)* | **ScriptInjector — deferred registration via ServletContext attributes** |
| 5 | DirectoryWatcher(s) | DirectoryWatcher(s) |
| 6 | Shutdown listener | Shutdown listener |

### Deferred Registration Flow

```
ServletContainerInitializer.onStartup()
  └── AutoreloadInitializer.bootstrap()
        └── JsfBridge.registerDeferredScriptInjector(servletContext, sseEndpointPath)
              └── servletContext.addListener(new ServletContextListener() { ... })

  ... (Mojarra/MyFaces ConfigureListener.contextInitialized() → Application created) ...

  ... (our programmatic ServletContextListener.contextInitialized() fires AFTER) ...
        └── FactoryFinder.getFactory(APPLICATION_FACTORY) → Application
              └── ScriptInjector.register(application)
                    └── Application.subscribeToEvent(PostAddToViewEvent, scriptListener)

  ... (first request) ...

JSF renders page → PostAddToViewEvent fires
  └── scriptListener checks re-entry guard ("jsfautoreload.scriptInjected")
        └── injects <script> with EventSource + onerror handler into <head>
              └── Browser opens SSE connection to /_jsf-autoreload/events
```

**Note**: The original plan used `PostConstructApplicationEvent` via `faces-config.xml`, but Mojarra 2.3.9 does not gracefully skip unresolvable listener classes across javax/jakarta namespaces. The programmatic `ServletContextListener` approach avoids this by leveraging the Servlet spec's ordering guarantee (programmatic listeners fire after declared listeners).

## Validation Rules

- SSE endpoint path must start with `/` (already enforced by servlet registration)
- `AsyncContext` timeout must be set to 0 for SSE connections (prevents premature timeout)
- Heartbeat interval must be > 0 and < proxy timeout (30s default, well within typical 60-120s proxy timeouts)
- Context path must not be null when injecting script (falls back to empty string for root context)
