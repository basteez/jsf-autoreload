# Story 1.8: DevServer Orchestrator

Status: done

## Story

As a developer,
I want a single dev server that watches files, copies changes to the exploded WAR, and triggers browser reloads,
So that I get a seamless sub-2-second feedback loop from file save to browser refresh.

## Acceptance Criteria

1. File modified in watch directory -> copied to output directory preserving relative path structure AND reload broadcast to all browsers
2. File deleted in watch directory -> NOT copied to output directory AND reload IS broadcast to all browsers
3. Rapid successive changes within 300ms debounce window -> single browser reload triggered
4. Startup logs: `"[JSF Autoreload] WebSocket server listening on ws://localhost:{port}. Watching: {dirs}"`
5. Missing output directory at `start()` throws `JsfAutoreloadConfigException` with actionable message
6. `close()` (or Ctrl+C) shuts down within 2 seconds in reverse order: dev loop -> WebSocket server -> file watcher
7. `DevServer` implements `Closeable` for try-with-resources in tests

## Tasks / Subtasks

- [x] Task 1: Create `DevServer` orchestrator class (AC: #1, #2, #4, #5, #7)
  - [x] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/DevServer.java`
  - [x] 1.2 Constructor takes `DevServerConfig`
  - [x] 1.3 `start()` method: validate config, initialize components, enter blocking dev loop
  - [x] 1.4 Validate output directory exists at start — throw `JsfAutoreloadConfigException` if not
  - [x] 1.5 Initialize in order: FileChangeWatcher -> DevWebSocketServer -> dev loop
  - [x] 1.6 Wire file change callback: on change -> copy file to outputDir (preserving relative path) -> broadcast reload
  - [x] 1.7 Wire file delete callback: on delete -> skip copy -> broadcast reload
  - [x] 1.8 Log startup confirmation message with port and watched directories
- [x] Task 2: Implement debounce coalescing (AC: #3)
  - [x] 2.1 Use `ScheduledExecutorService` from `DevServerConfig` (or create default if not provided)
  - [x] 2.2 On file event: schedule reload broadcast after debounce window (default 300ms)
  - [x] 2.3 If another event arrives within the window: cancel pending, reschedule
  - [x] 2.4 Result: rapid changes coalesce into a single reload
- [x] Task 3: Implement graceful shutdown (AC: #6, #7)
  - [x] 3.1 Implement `Closeable` interface
  - [x] 3.2 `close()` shuts down in reverse order: cancel debounce executor -> close WebSocket server -> close file watcher
  - [x] 3.3 Each component gets 1-second timeout via `executor.awaitTermination(1, SECONDS)`
  - [x] 3.4 Total shutdown within 2 seconds (NFR16)
  - [x] 3.5 Register shutdown hook for Ctrl+C handling
- [x] Task 4: Implement file copy logic (AC: #1, #2)
  - [x] 4.1 Copy changed file to outputDir preserving relative path from watch directory
  - [x] 4.2 Use `java.nio.file.Files.copy()` with `REPLACE_EXISTING`
  - [x] 4.3 Create parent directories if needed (`Files.createDirectories()`)
  - [x] 4.4 On delete: skip copy, still trigger reload
  - [x] 4.5 Log file operations at FINE level
- [x] Task 5: Write comprehensive tests (AC: #1-#7)
  - [x] 5.1 Create `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/DevServerTest.java`
  - [x] 5.2 Test file modify -> copy + broadcast
  - [x] 5.3 Test file delete -> no copy + broadcast
  - [x] 5.4 Test debounce: rapid changes -> single reload (inject controllable executor)
  - [x] 5.5 Test startup with missing output dir -> exception
  - [x] 5.6 Test graceful shutdown releases all resources
  - [x] 5.7 Test try-with-resources pattern
  - [x] 5.8 Use `@TempDir` for file system, `CountDownLatch` for async assertions

## Dev Notes

### Architecture: DevServer as Orchestrator

`DevServer` is the single entry point that plugin modules call. Plugin modules build a `DevServerConfig` and call `DevServer.start()`. They should NOT directly interact with `FileChangeWatcher` or `DevWebSocketServer`.

### Existing Logic to Consolidate

The current `JsfDevTask.java` (293 lines in gradle-plugin) contains the orchestration logic that should move to `DevServer`:
- Starts WebSocket server
- Creates file watcher with change callbacks
- Copies changed files to output directory
- Broadcasts reload via WebSocket
- Handles graceful shutdown via shutdown hook
- 800ms debounce for Java compilation (architecture changes this to 300ms for file changes)

Extract the core orchestration logic from `JsfDevTask.java` into `DevServer`. Leave Gradle-specific wiring (task properties, Java compilation) in the gradle-plugin.

### Debounce Implementation

Architecture specifies direct callbacks with `ScheduledExecutorService` for debounce:
```java
// On file event:
if (pendingReload != null) pendingReload.cancel(false);
pendingReload = executor.schedule(() -> {
    broadcastReload();
}, debounceMs, TimeUnit.MILLISECONDS);
```

The injectable executor enables tests to control timing.

### File Copy Logic

```java
// Compute relative path from watch directory
Path relativePath = watchDir.relativize(changedFile);
Path targetPath = outputDir.resolve(relativePath);
Files.createDirectories(targetPath.getParent());
Files.copy(changedFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
```

### Shutdown Order (CRITICAL)

Startup order: FileChangeWatcher -> DevWebSocketServer -> dev loop
Shutdown order (REVERSE): dev loop -> DevWebSocketServer -> FileChangeWatcher

Each component: `close()` + `awaitTermination(1, SECONDS)` = total under 2 seconds.

### Error Message Format

Missing output directory:
`"[JSF Autoreload] Output directory not found: {path}. Configure it explicitly via jsfAutoreload { outputDir = '...' } or verify your server name."`

### Code Conventions

- Logging: JUL, `[JSF Autoreload]` prefix for user-facing messages
- FINE level for per-file-change events
- INFO level for lifecycle events (started, stopped)
- All threads must be daemon threads
- Use `java.nio.file` for all file operations

### Project Structure Notes

- New file: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/DevServer.java`
- Tests: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/DevServerTest.java`
- Depends on: Story 1.2 (exceptions), Story 1.5 (DevServerConfig), Story 1.6 (FileChangeWatcher), Story 1.7 (DevWebSocketServer)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Core Module API Design]
- [Source: _bmad-output/planning-artifacts/architecture.md#Event Pipeline]
- [Source: _bmad-output/planning-artifacts/architecture.md#Resource Management & Threading]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.8]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (1M context)

### Debug Log References

No issues encountered.

### Completion Notes List

- Created DevServer orchestrator as the single entry point for the dev-loop.
- `start()` blocks via CountDownLatch with shutdown hook for Ctrl+C handling.
- `startNonBlocking()` provided for tests — starts all components without blocking.
- File change callback: copies file to outputDir preserving relative path, then schedules debounced reload.
- File delete callback: skips copy, schedules debounced reload.
- Debounce: uses ScheduledExecutorService with cancel-and-reschedule pattern. Injectable executor for test control.
- Graceful shutdown in reverse order: executor -> WebSocket -> watcher. Idempotent via volatile boolean.
- Implements Closeable for try-with-resources.
- 6 tests: file modify copy+broadcast, file delete no-copy+broadcast, debounce coalescing, missing output dir exception, shutdown timing (<2s), try-with-resources.

### File List

- New: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/DevServer.java`
- New: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/DevServerTest.java`

## Code Review (AI)

- **Reviewer:** Claude Opus 4.6 (1M context)
- **Date:** 2026-03-21
- **Result:** Pass — 3 issues found and fixed
- **Fixed H2:** `start()` now calls `startNonBlocking()` eliminating ~50 lines of duplicated initialization code.
- **Fixed M4:** `scheduleReload()` now cancels the previous future before scheduling the new one.
- **Fixed C3:** `Thread.sleep()` calls in tests now have documented comments.

## Change Log

- 2026-03-21: Code review complete — fixed H2 (start() dedup), M4 (scheduleReload cancel), C3 (Thread.sleep docs), status changed to done.
- 2026-03-16: Story implementation complete — created DevServer orchestrator with file watch/copy, WebSocket broadcast, debounce, and graceful shutdown.
