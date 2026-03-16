# Story 1.7: DevWebSocketServer Extraction to Core

Status: ready-for-dev

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

- [ ] Task 1: Move `DevWebSocketServer` to core module (AC: #1, #2)
  - [ ] 1.1 Copy `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java` to `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java`
  - [ ] 1.2 Verify no Gradle API imports (current code extends `WebSocketServer` — should be clean)
  - [ ] 1.3 Delete old `DevWebSocketServer.java` from gradle-plugin
- [ ] Task 2: Implement `Closeable` and improve error handling (AC: #4, #5)
  - [ ] 2.1 Implement `Closeable` interface
  - [ ] 2.2 `close()` calls `stop()` on the WebSocket server, releases port
  - [ ] 2.3 Port-in-use detection: catch `BindException`, throw `JsfAutoreloadException` with actionable message
  - [ ] 2.4 Use `[JSF Autoreload]` prefix in all error messages
- [ ] Task 3: Handle disconnection/reconnection (AC: #3)
  - [ ] 3.1 `onClose()` — remove connection from active list cleanly
  - [ ] 3.2 `onError()` — log at WARNING, do not crash server
  - [ ] 3.3 `onOpen()` — accept new connections from previously disconnected clients
- [ ] Task 4: Add `Java-WebSocket` dependency to core module (AC: #1)
  - [ ] 4.1 Add `org.java-websocket:Java-WebSocket:1.5.7` to core's `build.gradle.kts`
  - [ ] 4.2 Note: this library gets shadow-relocated in the gradle-plugin's shadow JAR — core publishes un-relocated for direct use and testing
- [ ] Task 5: Move and update tests (AC: #1-#6)
  - [ ] 5.1 Move `DevWebSocketServerTest.java` from gradle-plugin to core
  - [ ] 5.2 Add test for port-in-use throwing `JsfAutoreloadException`
  - [ ] 5.3 Add test for client disconnection and reconnection
  - [ ] 5.4 Add test for `close()` releasing the port (can rebind after close)
  - [ ] 5.5 Add startup timing test (< 3 seconds)
  - [ ] 5.6 Use `CountDownLatch` for async assertions, no `Thread.sleep()`

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
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/websocket/DevWebSocketServer.java — current 85-line implementation]
- [Source: jsf-autoreload-plugin/src/test/java/it/bstz/jsfautoreload/websocket/DevWebSocketServerTest.java — existing 72-line tests]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
