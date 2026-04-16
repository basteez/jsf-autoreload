# Quickstart: Fix Reload Bugs

**Feature**: 002-fix-reload-bugs | **Date**: 2026-04-16

## Prerequisites

- JDK 8+ installed
- Maven 3.6+ installed
- IDE with Java support (IntelliJ IDEA / Eclipse / VS Code)
- **Tomcat `reloadable` must be `false`**: Tomcat's built-in class-change monitoring races with jsf-autoreload's file watcher. Set `StandardContext.setReloadable(false)` (or equivalent) to let the plugin handle context reloads.

## Build & Test

```bash
# From repository root
mvn clean verify
```

This runs unit tests (surefire) and integration tests (failsafe) with embedded Tomcat.

## Verify the Fixes

### 1. ScriptInjector Wiring (Bug 1)

After the fix, the test application should inject an EventSource script into rendered pages:

```bash
# Run integration tests that verify SSE connection establishment
mvn -pl jsf-autoreload-integration-tests verify -Dit.test=XhtmlReloadIT
```

**Manual verification** with the test project at `/Users/tizianobasile/workspace/me/jsf-autoreload-test-prj`:

1. Build the plugin: `mvn clean install -DskipTests`
2. Start the test app (ensure `javax.faces.PROJECT_STAGE=Development` in `web.xml`)
3. Open a page in the browser
4. Open DevTools → Network tab → filter by EventStream
5. Confirm an SSE connection to `/_jsf-autoreload/events` is active
6. Edit an XHTML file → browser should auto-refresh

### 2. Heartbeat (Bug 2)

```bash
# Run integration tests that verify connection survives idle period
mvn -pl jsf-autoreload-integration-tests verify -Dit.test=MultiConnectionIT
```

**Manual verification**: Open a page, wait 2+ minutes without changes, then edit a file. Browser should still auto-refresh.

### 3. Connection Cleanup (Bug 3)

```bash
# Run unit tests for ConnectionManager
mvn -pl jsf-autoreload-core test -Dtest=ConnectionManagerTest
```

**Manual verification**: Open a page, note the connection count in logs, close the tab, trigger a file change. Logs should show the connection was removed.

## Key Files to Modify

| File | Change |
|------|--------|
| `AutoreloadInitializer.java` | Wire ScriptInjector with deferred registration |
| `DefaultSseHandler.java` | Implement working heartbeat; set AsyncContext timeout; register AsyncListener |
| `ConnectionManager.java` | Add `sendHeartbeatToAll()` method |
| `ServletBridge.java` | Add `addAsyncListener()` and `setAsyncTimeout()` bridge methods |
| `JavaxServletBridge.java` | Implement AsyncListener registration for javax namespace |
| `JakartaServletBridge.java` | Implement AsyncListener registration for jakarta namespace |
| `AsyncContextWrapper.java` | May need timeout and listener delegation |
| `JsfBridge.java` | Add `registerDeferredScriptInjector()` or equivalent |
| `JavaxJsfBridge.java` | Implement PostConstructApplicationEvent listener |
| `JakartaJsfBridge.java` | Implement PostConstructApplicationEvent listener |
