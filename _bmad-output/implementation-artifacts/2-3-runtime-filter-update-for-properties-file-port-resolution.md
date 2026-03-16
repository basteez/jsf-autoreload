# Story 2.3: Runtime Filter Update for Properties File Port Resolution

Status: ready-for-dev

## Story

As a developer,
I want the runtime filter to read the WebSocket port from a classpath properties file,
So that browser refresh works without JVM system properties or server-specific config.

## Acceptance Criteria

1. Properties file on classpath with `port=35729` -> filter reads port `35729`
2. No properties file but JVM system property `jsf.autoreload.port=35730` -> falls back to `35730`
3. Both properties file (`port=35729`) and system property (`jsf.autoreload.port=35730`) -> properties file wins (`35729`)
4. Neither properties file nor system property -> falls back to default `35729`
5. HTML response: filter buffers body, appends WebSocket reload script before `</body>`, adjusts `Content-Length`; latency under 50ms
6. Non-HTML response (JSON, XML, image) passes through unmodified
7. Auto-registers via `web-fragment.xml` in Servlet 3.0+ containers without user configuration

## Tasks / Subtasks

- [ ] Task 1: Update `DevModeFilter` port resolution (AC: #1, #2, #3, #4)
  - [ ] 1.1 In `DevModeFilter.init()`, implement port resolution fallback chain:
    1. Read `jsf-autoreload.properties` from classpath (`getClass().getClassLoader().getResourceAsStream("jsf-autoreload.properties")`)
    2. If found, parse `port` value
    3. If not found, check system property `jsf.autoreload.port`
    4. If neither, use default `35729`
  - [ ] 1.2 Remove existing system-property-only port resolution
  - [ ] 1.3 Log resolved port at INFO level
- [ ] Task 2: Verify HTML script injection (AC: #5)
  - [ ] 2.1 Existing behavior already buffers HTML and injects script — verify it still works
  - [ ] 2.2 Verify Content-Length header adjustment
  - [ ] 2.3 Verify latency is acceptable (< 50ms for typical HTML page)
- [ ] Task 3: Verify non-HTML passthrough (AC: #6)
  - [ ] 3.1 Existing behavior already passes non-HTML through — verify it still works
- [ ] Task 4: Verify auto-registration (AC: #7)
  - [ ] 4.1 Existing `web-fragment.xml` handles this — no changes needed
- [ ] Task 5: Update tests (AC: #1-#4)
  - [ ] 5.1 Update `DevModeFilterTest.java` in runtime module
  - [ ] 5.2 Test: properties file present -> uses file value
  - [ ] 5.3 Test: only system property set -> uses system property
  - [ ] 5.4 Test: both present -> properties file wins
  - [ ] 5.5 Test: neither present -> falls back to 35729
  - [ ] 5.6 Keep existing tests for HTML injection, non-HTML passthrough, Content-Length adjustment

## Dev Notes

### Existing DevModeFilter (Current State)

Location: `jsf-autoreload-runtime/src/main/java/it/bstz/jsfautoreload/filter/DevModeFilter.java` (162 lines)

Current port resolution: reads from system property `jsf.autoreload.port` with default `35729`. This must change to the fallback chain.

Current HTML injection: buffers response body via `BufferingResponseWrapper`, appends WebSocket reload script before `</body>`, adjusts `Content-Length`. This behavior is correct and stays.

### Port Resolution Fallback Chain (Architecture Decision)

Priority order:
1. Classpath properties file (`jsf-autoreload.properties` -> key `port`)
2. JVM system property (`jsf.autoreload.port`) — backwards compatibility with v0.1-beta
3. Default: `35729`

### Properties File Loading

```java
// In DevModeFilter.init():
InputStream is = getClass().getClassLoader().getResourceAsStream("jsf-autoreload.properties");
if (is != null) {
    Properties props = new Properties();
    props.load(is);
    port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
}
```

### Testing Port Resolution

For testing with properties file on classpath, you can:
- Create a test resources directory with `jsf-autoreload.properties`
- Or use a custom classloader in the test

For testing without properties file:
- Ensure the test classpath does NOT include the properties file
- Set/clear system properties in test setup/teardown

### Architecture Constraint: Zero Dependencies

The runtime module MUST remain dependency-free. Port resolution uses only `java.util.Properties` (JDK built-in) — no external libraries.

### Project Structure Notes

- Modified: `jsf-autoreload-runtime/src/main/java/it/bstz/jsfautoreload/filter/DevModeFilter.java`
- Modified: `jsf-autoreload-runtime/src/test/java/it/bstz/jsfautoreload/filter/DevModeFilterTest.java`
- NO new dependencies — runtime stays dependency-free
- Depends on: Story 2.2 (properties file is written by prepare step)

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Port Coordination]
- [Source: _bmad-output/planning-artifacts/epics.md#Story 2.3]
- [Source: jsf-autoreload-runtime/src/main/java/it/bstz/jsfautoreload/filter/DevModeFilter.java — current 162-line implementation]
- [Source: jsf-autoreload-runtime/src/test/java/it/bstz/jsfautoreload/filter/DevModeFilterTest.java — existing 205-line tests]
- [Source: _bmad-output/planning-artifacts/prd.md#NFR5 — <50ms filter latency]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
