# Story 1.7: DevWebSocketServer Extraction to Core

Status: done

## Story

As a developer,
I want a WebSocket server that browsers can connect to for reload notifications,
So that my browser refreshes automatically when files change.

## Acceptance Criteria

1. `DevWebSocketServer` started on a configured port accepts browser WebSocket connections
2. `broadcast("reload")` delivers the message to all connected clients within 100ms
3. Browser client disconnection and reconnection is handled without error or manual intervention
4. Port already in use throws `JsfAutoreloadException` with message: `"[JSF Autoreload] Port {port} is already in use. Configure a different port via jsfAutoreload { port = XXXX }."`
5. `DevWebSocketServer` implements `Closeable` — `close()` closes all connections and releases the port
6. Startup initialization completes in under 3 seconds

## Tasks / Subtasks

- [x] Task 1: Move `DevWebSocketServer` to core module (AC: #1, #2)
  - [x] 1.1 Copy to core
  - [x] 1.2 Verify no Gradle API imports
  - [x] 1.3 Delete old from gradle-plugin
- [x] Task 2: Implement `Closeable` and improve error handling (AC: #4, #5)
  - [x] 2.1 Implement `Closeable` interface
  - [x] 2.2 `close()` calls `stop()` on the WebSocket server, releases port
  - [x] 2.3 Port-in-use detection: catch `BindException`, throw `JsfAutoreloadException` with actionable message
  - [x] 2.4 Use `[JSF Autoreload]` prefix in all error messages
- [x] Task 3: Handle disconnection/reconnection (AC: #3)
  - [x] 3.1 `onClose()` — remove connection from active list cleanly
  - [x] 3.2 `onError()` — log at WARNING, do not crash server
  - [x] 3.3 `onOpen()` — accept new connections from previously disconnected clients
- [x] Task 4: Add `Java-WebSocket` dependency to core module (AC: #1)
  - [x] 4.1 Add `org.java-websocket:Java-WebSocket:1.5.7` to core's `build.gradle.kts` (as `api` for transitive access)
  - [x] 4.2 Removed duplicate dependency from gradle-plugin
- [x] Task 5: Move and update tests (AC: #1-#6)
  - [x] 5.1 Move `DevWebSocketServerTest.java` from gradle-plugin to core
  - [x] 5.2 Add test for port-in-use throwing `JsfAutoreloadException`
  - [x] 5.3 Add test for client disconnection and reconnection
  - [x] 5.4 Add test for `close()` releasing the port (can rebind after close)
  - [x] 5.5 Add startup timing test (< 3 seconds)
  - [x] 5.6 Use `CountDownLatch` for async assertions

## Dev Notes

### Existing DevWebSocketServer (Current State)

Location: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java` (85 lines)

Current implementation:
- Extends `org.java_websocket.server.WebSocketServer`
- Listens on `localhost:{port}`
- `broadcastReload()` sends "reload" to all connections
- `onStart()` signals startup via `CountDownLatch`
- `onError()` logs errors, handles `BindException` for port conflicts
- 5-second startup timeout with meaningful error message

### Migration Assessment: LOW RISK

The current code has no Gradle API dependencies. Main changes:
- Implement `Closeable`
- Upgrade port-in-use error to use `JsfAutoreloadException`
- Add reconnection handling
- Move to core module

### Shadow Relocation Impact

`org.java_websocket` is currently relocated in the gradle-plugin's shadow JAR to avoid classpath pollution. After moving to core:
- Core publishes un-relocated JAR (for testing and direct use)
- gradle-plugin shadow JAR will inline core and relocate `org.java_websocket` there
- maven-plugin will declare core as a dependency (relocation handled by maven-plugin shadow)

### Architecture Constraints

- Logging: JUL in core (`Logger.getLogger(DevWebSocketServer.class.getName())`)
- Threading: daemon threads only — WebSocket server threads should be daemon
- Shutdown: `close()` must release port within the component's 1-second budget
- Error messages: `[JSF Autoreload]` prefix with actionable guidance

### Test Dependencies

- `org.java-websocket:Java-WebSocket:1.5.7` needed as both implementation and test dependency in core
- WebSocket client for tests: use the same Java-WebSocket library's client class

### Project Structure Notes

- FROM: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java`
- TO: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java`
- Depends on: Story 1.1 (core module exists), Story 1.2 (exceptions)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Resource Management & Threading]
- [Source: _bmad-output/planning-artifacts/architecture.md#Core Module API Design]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.7]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (1M context)

### Debug Log References

- Build fix: `setDaemon(boolean)` in `WebSocketServer` is already public — removed conflicting private method, used `setConnectionLostTimeout(0)` directly in constructor instead.
- Build fix: Java-WebSocket dependency changed from `implementation` to `api` scope since gradle-plugin's `JsfDevTask` references `DevWebSocketServer` which extends `WebSocketServer` — transitive access required.

### Completion Notes List

- Migrated DevWebSocketServer to core with Closeable interface.
- close() uses stop(1000ms) with idempotent guard. stopServer() kept as alias.
- Port-in-use detection throws JsfAutoreloadException with actionable message including port number.
- Startup timeout reduced to 3 seconds (was 5).
- onOpen/onClose/onError log at appropriate levels; errors don't crash server.
- Removed Java-WebSocket dependency from gradle-plugin (now transitive via core api scope).
- 6 tests: broadcast to client, port-in-use exception, disconnect/reconnect, close releases port, startup timing, close idempotency.

### File List

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/websocket/DevWebSocketServerTest.java`
- Modified: `jsf-autoreload-core/build.gradle.kts` (added Java-WebSocket as api dependency)
- Modified: `jsf-autoreload-gradle-plugin/build.gradle.kts` (removed Java-WebSocket dependencies)
- Deleted: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java`
- Deleted: `jsf-autoreload-gradle-plugin/src/test/java/it/bstz/jsfautoreload/websocket/DevWebSocketServerTest.java`

## Code Review (AI)

- **Reviewer:** Claude Opus 4.6 (1M context)
- **Date:** 2026-03-21
- **Result:** Pass — 2 issues found and fixed
- **Fixed M3:** `onError()` no longer double-reports bind failures (returns early for server-level errors that will be thrown by `startServer()`).
- **Fixed M2:** Removed backward-compat `stopServer()` alias (updated `JsfDevTask` to use `close()`).

## Change Log

- 2026-03-21: Code review complete — fixed M3 (onError double-reporting), M2 (removed stopServer() alias), status changed to done.
- 2026-03-16: Story implementation complete — extracted DevWebSocketServer to core with Closeable, proper exception handling, and reconnection support.
