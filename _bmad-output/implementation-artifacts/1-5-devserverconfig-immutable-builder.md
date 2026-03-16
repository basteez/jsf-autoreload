# Story 1.5: DevServerConfig Immutable Builder

Status: ready-for-dev

## Story

As a developer,
I want to configure the dev loop with port, output directory, watch directories, and server settings,
So that the live-reload works with my specific project setup.

## Acceptance Criteria

1. `DevServerConfig.builder()` with all required fields (outputDir, watchDirs, serverAdapter) set and `build()` called returns an immutable `DevServerConfig`
2. Missing required field (outputDir, watchDirs, or serverAdapter) at `build()` throws `JsfAutoreloadConfigException` with actionable message identifying the missing field
3. Default port is `35729` when not explicitly set
4. Default debounceMs is `300` when not explicitly set
5. A `ScheduledExecutorService` can be provided via `.executor()` for test injection
6. All getters return configured values and the config object cannot be modified

## Tasks / Subtasks

- [ ] Task 1: Create `DevServerConfig` with builder (AC: #1, #2, #3, #4, #5, #6)
  - [ ] 1.1 Create `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/DevServerConfig.java`
  - [ ] 1.2 Implement static `builder()` method returning `Builder` inner class
  - [ ] 1.3 Builder methods: `port(int)`, `outputDir(Path)`, `watchDirs(List<Path>)`, `serverAdapter(ServerAdapter)`, `debounceMs(long)`, `executor(ScheduledExecutorService)`
  - [ ] 1.4 `build()` validates required fields, throws `JsfAutoreloadConfigException` for missing fields
  - [ ] 1.5 Config class: private constructor, all fields final, getter methods only
  - [ ] 1.6 Defaults: port=35729, debounceMs=300, executor=null (created by DevServer if not provided)
- [ ] Task 2: Write comprehensive tests (AC: #1-#6)
  - [ ] 2.1 Create `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/DevServerConfigTest.java`
  - [ ] 2.2 Test successful build with all required fields
  - [ ] 2.3 Test default port value (35729)
  - [ ] 2.4 Test default debounceMs value (300)
  - [ ] 2.5 Test custom port and debounce override
  - [ ] 2.6 Test missing outputDir throws `JsfAutoreloadConfigException`
  - [ ] 2.7 Test missing watchDirs throws `JsfAutoreloadConfigException`
  - [ ] 2.8 Test missing serverAdapter throws `JsfAutoreloadConfigException`
  - [ ] 2.9 Test injectable executor is stored and retrievable
  - [ ] 2.10 Test immutability — getters return values, no setters exist

## Dev Notes

### Architecture Decision: Builder Pattern

From the architecture document:
```java
DevServerConfig config = DevServerConfig.builder()
    .port(35729)
    .outputDir(path)
    .watchDirs(dirs)
    .serverAdapter(adapter)       // instance, not class reference
    .debounceMs(300)
    .executor(scheduledExecutor)  // injectable for testability
    .build();
```

### Key Design Points

- `serverAdapter` is an INSTANCE, not a class reference — plugin modules construct the adapter
- `executor` is optional — `DevServer` (Story 1.8) will create a default `Executors.newScheduledThreadPool(1)` if not provided. This enables test injection for debounce testing.
- `watchDirs` is a `List<Path>` — multiple directories can be watched
- `outputDir` is a `Path` — the exploded WAR output directory
- Defensive copies: `watchDirs` list should be copied in builder to prevent external mutation

### Error Message Format

When a required field is missing:
- `"[JSF Autoreload] Missing required configuration: outputDir. Set it via jsfAutoreload { outputDir = '...' }."`
- `"[JSF Autoreload] Missing required configuration: watchDirs. Set it via jsfAutoreload { watchDirs = ['src/main/webapp'] }."`
- `"[JSF Autoreload] Missing required configuration: serverAdapter. Ensure a server adapter is configured."`

### Code Conventions

- No `var`, no wildcard imports, explicit types
- Package: `it.bstz.jsfautoreload` (root package)
- Builder as static inner class: `DevServerConfig.Builder`
- Test naming: camelCase behavior description, e.g., `throwsExceptionWhenOutputDirMissing()`

### Project Structure Notes

- Location: `jsf-autoreload-core/src/main/java/it/bstz/jsfautoreload/DevServerConfig.java`
- Tests: `jsf-autoreload-core/src/test/java/it/bstz/jsfautoreload/DevServerConfigTest.java`
- Depends on: Story 1.2 (exceptions), Story 1.3 (ServerAdapter interface)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Configuration Model]
- [Source: _bmad-output/planning-artifacts/architecture.md#Event Pipeline]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.5]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
