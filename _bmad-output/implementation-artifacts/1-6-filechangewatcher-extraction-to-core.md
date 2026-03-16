# Story 1.6: FileChangeWatcher Extraction to Core

Status: ready-for-dev

## Story

As a developer,
I want file changes in my source directories detected automatically,
So that modified .xhtml, CSS, JS, and static files are immediately noticed.

## Acceptance Criteria

1. `onChange` callback (`Consumer<Path>`) is invoked when a file is created in a watched directory
2. `onChange` callback is invoked when a file is modified in a watched directory
3. `onDelete` callback (`Consumer<Path>`) is invoked when a file is deleted in a watched directory
4. All file types (.xhtml, .css, .js, .png, .woff, etc.) trigger the appropriate callback
5. `FileChangeWatcher` implements `Closeable` — `close()` stops watching threads and releases resources
6. Transient file system errors (e.g., locked files) do not crash the watcher
7. No Gradle API imports in `FileChangeWatcher`

## Tasks / Subtasks

- [ ] Task 1: Move `FileChangeWatcher` to core module (AC: #1, #2, #3, #7)
  - [ ] 1.1 Copy `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java` to `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java`
  - [ ] 1.2 Remove Gradle API imports if any exist (current code wraps `io.methvin.watcher.DirectoryWatcher` — should be clean)
  - [ ] 1.3 Update `onDelete` callback signature — currently both create/modify/delete go through `onChanged`; separate delete into its own `onDelete` callback as per architecture
  - [ ] 1.4 Delete the old `FileChangeWatcher.java` from gradle-plugin
- [ ] Task 2: Add `Closeable` implementation (AC: #5)
  - [ ] 2.1 Implement `Closeable` interface on `FileChangeWatcher`
  - [ ] 2.2 `close()` calls the underlying `DirectoryWatcher.close()`
  - [ ] 2.3 Ensure thread safety on `close()` — idempotent, no double-close exceptions
- [ ] Task 3: Add error resilience (AC: #6)
  - [ ] 3.1 Catch and log transient file system errors in the event loop
  - [ ] 3.2 Continue watching after error — do not terminate the watcher
  - [ ] 3.3 Log at WARNING level with `[JSF Autoreload]` prefix
- [ ] Task 4: Add `directory-watcher` dependency to core module (AC: #7)
  - [ ] 4.1 Add `io.methvin:directory-watcher:0.18.0` to `jsf-autoreload-core/build.gradle.kts`
  - [ ] 4.2 Remove this dependency from gradle-plugin (it will get it transitively via core, or via shadow)
- [ ] Task 5: Move and update tests (AC: #1, #2, #3, #4, #5, #6)
  - [ ] 5.1 Move `FileChangeWatcherTest.java` from gradle-plugin to core
  - [ ] 5.2 Add test for delete event triggering `onDelete` (not `onChange`)
  - [ ] 5.3 Add test for `close()` stops watching
  - [ ] 5.4 Ensure file type agnostic — test with `.xhtml`, `.css`, `.js` files
  - [ ] 5.5 All tests use `@TempDir` and `CountDownLatch` with timeouts (no `Thread.sleep`)

## Dev Notes

### Existing FileChangeWatcher (Current State)

Location: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java` (56 lines)

Current implementation:
- Wraps `io.methvin.watcher.DirectoryWatcher`
- Constructor takes `List<Path> paths`, `Consumer<Path> onChanged`, `Consumer<Path> onDeleted`
- Event routing: CREATE/MODIFY -> `onChanged`, DELETE -> `onDeleted`
- `watchAsync()` starts watching in background
- `stop()` method for shutdown
- Already separates changed/deleted callbacks — matches architecture requirements

### Migration Assessment: LOW RISK

The current `FileChangeWatcher` is already clean:
- No Gradle API imports
- Already has separate `onChanged` and `onDeleted` callbacks
- Already wraps `DirectoryWatcher` from `io.methvin`
- Main changes: implement `Closeable`, add error resilience, move to core

### Architecture Requirements

- All long-lived resources must implement `Closeable` (for try-with-resources in tests)
- Daemon threads only
- Use `java.util.logging` (JUL) in core module
- Logger: `private static final Logger LOG = Logger.getLogger(FileChangeWatcher.class.getName());`
- Error resilience: catch IOExceptions in event loop, log at WARNING, continue watching

### Dependency: directory-watcher Library

`io.methvin:directory-watcher:0.18.0` — cross-platform file watching library. Supports macOS (native FSEvents), Linux (inotify), Windows (ReadDirectoryChangesW). This is the correct library per architecture — do NOT switch to JDK `WatchService` directly.

### Test Patterns

Existing tests use `CountDownLatch` with 5-second timeouts — this is the correct pattern. No `Thread.sleep()` allowed.

### Project Structure Notes

- FROM: `jsf-autoreload-gradle-plugin/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java`
- TO: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java`
- Depends on: Story 1.1 (core module exists), Story 1.2 (exceptions for error handling)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Resource Management & Threading]
- [Source: _bmad-output/planning-artifacts/architecture.md#Event Pipeline]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.6]
- [Source: jsf-autoreload-plugin/src/main/java/it/bstz/jsfautoreload/watcher/FileChangeWatcher.java — current 56-line implementation]
- [Source: jsf-autoreload-plugin/src/test/java/it/bstz/jsfautoreload/watcher/FileChangeWatcherTest.java — existing 110-line tests]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
